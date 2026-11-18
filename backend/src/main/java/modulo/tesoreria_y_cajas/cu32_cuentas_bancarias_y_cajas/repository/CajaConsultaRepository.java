package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.repository;

import lombok.RequiredArgsConstructor;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.OpcionesCajaResponse.SucursalOpcion;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.CalculadoraArqueo.Partida;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.ReglasCaja;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Lecturas de solo consulta de la caja POS (HU-34 / HU-35): turnos con sucursal, cajero y
 * sumas de efectivo, partidas del arqueo por forma de pago, sucursales activas y rol del usuario.
 *
 * <p>Como en la CU34 (CxP), se usa SQL propio para leer tablas de otros módulos (sucursal,
 * usuario, empleados, ventas, pago_venta) sin depender de su mapeo JPA. Las escrituras van
 * por las entidades compartidas TurnoCaja y MovimientoCaja.
 */
@Repository
@RequiredArgsConstructor
public class CajaConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Turno con nombres resueltos y las tres sumas de efectivo que alimentan el esperado. */
    public record TurnoFila(
            Long id,
            Integer sucursalId,
            String sucursalNombre,
            Long cajeroId,
            String cajeroNombre,
            BigDecimal montoApertura,
            BigDecimal montoCierre,
            BigDecimal diferencia,
            LocalDateTime fechaApertura,
            LocalDateTime fechaCierre,
            String estado,
            String observaciones,
            BigDecimal ventasEfectivo,
            BigDecimal ingresosEfectivo,
            BigDecimal egresosEfectivo) {}

    /** Filtros del historial (HU-35). {@code cajeroId} null = todos (administrador). */
    public record FiltroTurnos(Long cajeroId, Integer sucursalId, String estado, LocalDate desde, LocalDate hasta) {}

    /** Ventas vigentes del turno: venta no ANULADA y pago no anulado ni rechazado. */
    private static final String PAGOS_VIGENTES = """
             FROM pago_venta pv
             JOIN ventas v ON v.id = pv.venta_id
             JOIN formas_pago fp ON fp.id = pv.forma_pago_id
             WHERE v.turno_caja_id = %s
               AND UPPER(v.estado) <> 'ANULADA'
               AND UPPER(pv.estado) NOT IN ('ANULADO', 'RECHAZADO')
            """;

    private static final String EFECTIVO = "UPPER(fp.codigo) = 'EFECTIVO'";

    private static final String FROM_TURNOS = """
             FROM turnos_caja t
             LEFT JOIN sucursal s ON s.id = t.sucursal_id
             LEFT JOIN usuario u ON u.id = t.cajero_id
             LEFT JOIN empleados e ON e.id = u.empleado_id
            """;

    /** Suma de movimientos en efectivo del turno {@code t} para un tipo (INGRESO o EGRESO). */
    private static final String MOVIMIENTOS_EFECTIVO = "(SELECT COALESCE(SUM(m.monto), 0) FROM movimientos_caja m"
            + " JOIN formas_pago fp ON fp.id = m.forma_pago_id"
            + " WHERE m.turno_caja_id = t.id AND UPPER(m.tipo) = '%s' AND " + EFECTIVO + ")";

    private static final String SELECT_TURNO = "SELECT t.id, t.sucursal_id, s.nombre AS sucursal_nombre, t.cajero_id,"
            + " COALESCE(NULLIF(TRIM(CONCAT(e.nombre, ' ', e.apellido)), ''), u.email) AS cajero_nombre,"
            + " t.monto_apertura, t.monto_cierre, t.diferencia, t.fecha_apertura, t.fecha_cierre,"
            + " t.estado, t.observaciones,"
            + " (SELECT COALESCE(SUM(pv.importe), 0)" + PAGOS_VIGENTES.formatted("t.id") + " AND " + EFECTIVO
            + ") AS ventas_efectivo, "
            + MOVIMIENTOS_EFECTIVO.formatted("INGRESO") + " AS ingresos_efectivo, "
            + MOVIMIENTOS_EFECTIVO.formatted("EGRESO") + " AS egresos_efectivo"
            + FROM_TURNOS;

    private static final RowMapper<TurnoFila> TURNO = (rs, n) -> new TurnoFila(
            rs.getLong("id"),
            rs.getInt("sucursal_id"),
            rs.getString("sucursal_nombre"),
            rs.getLong("cajero_id"),
            rs.getString("cajero_nombre"),
            rs.getBigDecimal("monto_apertura"),
            rs.getBigDecimal("monto_cierre"),
            rs.getBigDecimal("diferencia"),
            fechaHora(rs.getTimestamp("fecha_apertura")),
            fechaHora(rs.getTimestamp("fecha_cierre")),
            rs.getString("estado"),
            rs.getString("observaciones"),
            rs.getBigDecimal("ventas_efectivo"),
            rs.getBigDecimal("ingresos_efectivo"),
            rs.getBigDecimal("egresos_efectivo"));

    public Optional<TurnoFila> turno(Long id) {
        return jdbc.query(SELECT_TURNO + " WHERE t.id = :id", new MapSqlParameterSource("id", id), TURNO)
                .stream()
                .findFirst();
    }

    /** Página del historial, de la apertura más reciente a la más antigua. */
    public List<TurnoFila> listarTurnos(FiltroTurnos filtro, int limit, long offset) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = filtros(filtro, params);
        params.addValue("limit", limit).addValue("offset", offset);
        return jdbc.query(SELECT_TURNO + where
                        + " ORDER BY t.fecha_apertura DESC, t.id DESC LIMIT :limit OFFSET :offset",
                params, TURNO);
    }

    public long contarTurnos(FiltroTurnos filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = filtros(filtro, params);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM turnos_caja t" + where, params, Long.class);
        return total == null ? 0 : total;
    }

    /** Montos del turno sumados por forma de pago y origen (VENTA, INGRESO, EGRESO). */
    public List<Partida> partidasArqueo(Long turnoId) {
        return jdbc.query(
                "SELECT fp.id, fp.nombre, fp.codigo, 'VENTA' AS origen, SUM(pv.importe) AS monto"
                        + PAGOS_VIGENTES.formatted(":turnoId")
                        + " GROUP BY fp.id, fp.nombre, fp.codigo"
                        + " UNION ALL"
                        + " SELECT fp.id, fp.nombre, fp.codigo, UPPER(m.tipo) AS origen, SUM(m.monto) AS monto"
                        + " FROM movimientos_caja m JOIN formas_pago fp ON fp.id = m.forma_pago_id"
                        + " WHERE m.turno_caja_id = :turnoId"
                        + " GROUP BY fp.id, fp.nombre, fp.codigo, UPPER(m.tipo)",
                new MapSqlParameterSource("turnoId", turnoId),
                (rs, n) -> new Partida(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        "EFECTIVO".equalsIgnoreCase(rs.getString("codigo")),
                        rs.getString("origen"),
                        rs.getBigDecimal("monto")));
    }

    public List<SucursalOpcion> sucursalesActivas() {
        return jdbc.query("SELECT id, nombre FROM sucursal WHERE activo = TRUE ORDER BY nombre",
                new MapSqlParameterSource(),
                (rs, n) -> new SucursalOpcion(rs.getInt("id"), rs.getString("nombre")));
    }

    public boolean sucursalActiva(Integer sucursalId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM sucursal WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", sucursalId), Integer.class);
        return n != null && n > 0;
    }

    /** Indica si el rol del usuario es ADMINISTRADOR, ADMIN o SUPERADMIN. */
    public boolean esAdministrador(Long usuarioId) {
        return jdbc.queryForList(
                        "SELECT r.nombre FROM usuario u JOIN rol r ON r.id = u.rol_id WHERE u.id = :id",
                        new MapSqlParameterSource("id", usuarioId), String.class)
                .stream()
                .anyMatch(ReglasCaja::esRolAdmin);
    }

    /**
     * Pone en fila las aperturas de un mismo usuario hasta el fin de la transacción (bloqueo
     * consultivo de PostgreSQL), para que dos peticiones simultáneas no abran dos turnos.
     */
    public void bloquearAperturaDe(Long usuarioId) {
        jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT pg_advisory_xact_lock(:clave)) AS bloqueo",
                new MapSqlParameterSource("clave", 32_000_000_000L + usuarioId), Long.class);
    }

    private static String filtros(FiltroTurnos f, MapSqlParameterSource params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (f.cajeroId() != null) {
            params.addValue("cajeroId", f.cajeroId());
            where.append(" AND t.cajero_id = :cajeroId");
        }
        if (f.sucursalId() != null) {
            params.addValue("sucursalId", f.sucursalId());
            where.append(" AND t.sucursal_id = :sucursalId");
        }
        if (f.estado() != null && !f.estado().isBlank()) {
            params.addValue("estado", f.estado().trim().toUpperCase(Locale.ROOT));
            where.append(" AND UPPER(t.estado) = :estado");
        }
        if (f.desde() != null) {
            params.addValue("desde", Timestamp.valueOf(f.desde().atStartOfDay()));
            where.append(" AND t.fecha_apertura >= :desde");
        }
        if (f.hasta() != null) {
            params.addValue("hasta", Timestamp.valueOf(f.hasta().plusDays(1).atStartOfDay()));
            where.append(" AND t.fecha_apertura < :hasta");
        }
        return where.toString();
    }

    private static LocalDateTime fechaHora(Timestamp valor) {
        return valor == null ? null : valor.toLocalDateTime();
    }
}
