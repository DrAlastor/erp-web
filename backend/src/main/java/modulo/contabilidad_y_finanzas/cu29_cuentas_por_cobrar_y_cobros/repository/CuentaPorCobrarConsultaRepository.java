package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.repository;

import lombok.RequiredArgsConstructor;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.CobroResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.OpcionResponse;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Lecturas de la CU29: cuotas con su venta, cliente y sucursal, cobros con forma de pago
 * y usuario, agregados del tablero y opciones. SQL propio, como en la CU34, para leer
 * tablas de Ventas y Seguridad sin depender de su mapeo JPA.
 */
@Repository
@RequiredArgsConstructor
public class CuentaPorCobrarConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Cuota con venta, cliente y sucursal ya resueltos. */
    public record CuentaFila(
            Long id,
            Long ventaId,
            String documento,
            Long clienteId,
            String razonSocial,
            String nitCi,
            String telefono,
            String email,
            Integer numeroCuota,
            BigDecimal montoTotal,
            BigDecimal montoCobrado,
            BigDecimal saldo,
            LocalDate fechaVencimiento,
            String estado,
            LocalDate fechaVenta,
            BigDecimal totalVenta,
            String sucursal) {}

    public record Totales(BigDecimal saldoTotal, BigDecimal vencido, BigDecimal porVencer,
                          long cantidad, long vencidas) {}

    public record SaldoVencido(LocalDate fechaVencimiento, BigDecimal saldo) {}

    public record ClienteFila(Long id, String razonSocial, String nitCi, BigDecimal limiteCredito) {}

    private static final String FROM = """
             FROM cuentas_por_cobrar cx
             LEFT JOIN ventas v ON v.id = cx.venta_id
             LEFT JOIN clientes cl ON cl.id = cx.cliente_id
             LEFT JOIN sucursal s ON s.id = cx.sucursal_id
            """;

    private static final String SELECT = """
            SELECT cx.id, cx.venta_id,
                   CONCAT(CASE WHEN UPPER(COALESCE(v.tipo_comprobante, 'FACTURA')) = 'FACTURA' THEN 'FAC-' ELSE 'NV-' END,
                          v.numero_factura) AS documento,
                   cx.cliente_id, cl.razon_social, cl.nit_ci, cl.telefono, cl.email, cx.numero_cuota,
                   cx.monto_total, cx.monto_cobrado, cx.saldo, cx.fecha_vencimiento, cx.estado,
                   v.fecha AS fecha_venta, v.total AS total_venta, s.nombre AS sucursal
            """ + FROM;

    /** Cuota con deuda viva (con saldo, ni pagada ni anulada). */
    private static final String CON_SALDO =
            " cx.saldo > 0 AND UPPER(cx.estado) NOT IN ('PAGADO', 'ANULADA', 'ANULADO')";

    private static final RowMapper<CuentaFila> CUENTA = (rs, n) -> {
        Timestamp venta = rs.getTimestamp("fecha_venta");
        return new CuentaFila(
                rs.getLong("id"),
                rs.getLong("venta_id"),
                rs.getString("documento"),
                rs.getLong("cliente_id"),
                rs.getString("razon_social"),
                rs.getString("nit_ci"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getInt("numero_cuota"),
                rs.getBigDecimal("monto_total"),
                rs.getBigDecimal("monto_cobrado"),
                rs.getBigDecimal("saldo"),
                fecha(rs.getDate("fecha_vencimiento")),
                rs.getString("estado"),
                venta == null ? null : venta.toLocalDateTime().toLocalDate(),
                rs.getBigDecimal("total_venta"),
                rs.getString("sucursal"));
    };

    /** Página ordenada por vencimiento (lo más urgente primero). */
    public List<CuentaFila> listar(String search, String estado, Long clienteId, LocalDate hoy, int limit, long offset) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = filtros(search, estado, clienteId, hoy, params);
        params.addValue("limit", limit).addValue("offset", offset);
        return jdbc.query(SELECT + where + " ORDER BY cx.fecha_vencimiento ASC, cx.id ASC LIMIT :limit OFFSET :offset",
                params, CUENTA);
    }

    public long contar(String search, String estado, Long clienteId, LocalDate hoy) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = filtros(search, estado, clienteId, hoy, params);
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + FROM + where, params, Long.class);
        return total == null ? 0 : total;
    }

    public Optional<CuentaFila> cuenta(Long id) {
        return jdbc.query(SELECT + " WHERE cx.id = :id", new MapSqlParameterSource("id", id), CUENTA)
                .stream().findFirst();
    }

    public List<CuentaFila> cuotasDeCliente(Long clienteId) {
        return jdbc.query(SELECT + " WHERE cx.cliente_id = :id ORDER BY cx.fecha_vencimiento, cx.id",
                new MapSqlParameterSource("id", clienteId), CUENTA);
    }

    public Optional<ClienteFila> cliente(Long id) {
        return jdbc.query("SELECT id, razon_social, nit_ci, limite_credito FROM clientes WHERE id = :id",
                        new MapSqlParameterSource("id", id),
                        (rs, n) -> new ClienteFila(rs.getLong("id"), rs.getString("razon_social"),
                                rs.getString("nit_ci"), rs.getBigDecimal("limite_credito")))
                .stream().findFirst();
    }

    /** Historial de cobros de la cuota, del más reciente al más antiguo. */
    public List<CobroResponse> cobros(Long cuentaId) {
        return jdbc.query(SELECT_COBRO + " WHERE c.cuenta_por_cobrar_id = :id ORDER BY c.fecha_cobro DESC, c.id DESC",
                new MapSqlParameterSource("id", cuentaId), COBRO);
    }

    public Optional<CobroResponse> cobro(Long cobroId) {
        return jdbc.query(SELECT_COBRO + " WHERE c.id = :id", new MapSqlParameterSource("id", cobroId), COBRO)
                .stream().findFirst();
    }

    private static final String SELECT_COBRO = """
            SELECT c.id, c.numero_recibo, c.forma_pago_id, fp.nombre AS forma_pago, c.monto, c.fecha_cobro,
                   c.transaccion_externa, c.observaciones, c.turno_caja_id, c.fecha_creacion,
                   COALESCE(NULLIF(TRIM(CONCAT(e.nombre, ' ', e.apellido)), ''), u.email) AS usuario
            FROM cobros c
            LEFT JOIN formas_pago fp ON fp.id = c.forma_pago_id
            LEFT JOIN usuario u ON u.id = c.usuario_id
            LEFT JOIN empleados e ON e.id = u.empleado_id
            """;

    private static final RowMapper<CobroResponse> COBRO = (rs, n) -> {
        Timestamp creacion = rs.getTimestamp("fecha_creacion");
        return new CobroResponse(
                rs.getLong("id"),
                rs.getString("numero_recibo"),
                rs.getLong("forma_pago_id"),
                rs.getString("forma_pago"),
                rs.getBigDecimal("monto"),
                fecha(rs.getDate("fecha_cobro")),
                rs.getString("transaccion_externa"),
                rs.getString("observaciones"),
                rs.getObject("turno_caja_id") == null ? null : rs.getLong("turno_caja_id"),
                rs.getString("usuario"),
                creacion == null ? null : creacion.toLocalDateTime());
    };

    public Totales totales(LocalDate hoy, LocalDate hasta) {
        return jdbc.queryForObject(
                """
                SELECT COALESCE(SUM(cx.saldo), 0) AS saldo_total,
                       COALESCE(SUM(CASE WHEN cx.fecha_vencimiento < :hoy THEN cx.saldo END), 0) AS vencido,
                       COALESCE(SUM(CASE WHEN cx.fecha_vencimiento BETWEEN :hoy AND :hasta THEN cx.saldo END), 0) AS por_vencer,
                       COUNT(*) AS cantidad,
                       COUNT(CASE WHEN cx.fecha_vencimiento < :hoy THEN 1 END) AS vencidas
                FROM cuentas_por_cobrar cx
                WHERE""" + CON_SALDO,
                new MapSqlParameterSource("hoy", Date.valueOf(hoy)).addValue("hasta", Date.valueOf(hasta)),
                (rs, n) -> new Totales(rs.getBigDecimal("saldo_total"), rs.getBigDecimal("vencido"),
                        rs.getBigDecimal("por_vencer"), rs.getLong("cantidad"), rs.getLong("vencidas")));
    }

    /** Suma de los cobros con fecha en [desde, hasta). */
    public BigDecimal cobrado(LocalDate desde, LocalDate hastaExclusive) {
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(monto), 0) FROM cobros WHERE fecha_cobro >= :desde AND fecha_cobro < :hasta",
                new MapSqlParameterSource("desde", Date.valueOf(desde)).addValue("hasta", Date.valueOf(hastaExclusive)),
                BigDecimal.class);
    }

    public List<SaldoVencido> saldosVencidos(LocalDate hoy) {
        return jdbc.query(
                "SELECT cx.fecha_vencimiento, cx.saldo FROM cuentas_por_cobrar cx WHERE" + CON_SALDO
                        + " AND cx.fecha_vencimiento < :hoy",
                new MapSqlParameterSource("hoy", Date.valueOf(hoy)),
                (rs, n) -> new SaldoVencido(fecha(rs.getDate("fecha_vencimiento")), rs.getBigDecimal("saldo")));
    }

    public List<OpcionResponse> topClientes(int limit) {
        return jdbc.query(
                "SELECT cx.cliente_id, cl.razon_social, SUM(cx.saldo) AS saldo"
                        + " FROM cuentas_por_cobrar cx LEFT JOIN clientes cl ON cl.id = cx.cliente_id"
                        + " WHERE" + CON_SALDO
                        + " GROUP BY cx.cliente_id, cl.razon_social ORDER BY saldo DESC LIMIT :limit",
                new MapSqlParameterSource("limit", limit),
                (rs, n) -> new OpcionResponse(rs.getLong("cliente_id"), rs.getString("razon_social"),
                        rs.getBigDecimal("saldo").toPlainString()));
    }

    /** Clientes que tienen alguna cuota (con o sin saldo), para el filtro y el estado de cuenta. */
    public List<OpcionResponse> clientesConCuentas(String search, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        String where = "";
        if (search != null && !search.isBlank()) {
            params.addValue("term", likeTerm(search));
            where = " AND (LOWER(cl.razon_social) LIKE :term ESCAPE '!' OR LOWER(cl.nit_ci) LIKE :term ESCAPE '!')";
        }
        return jdbc.query(
                "SELECT cl.id, cl.razon_social, cl.nit_ci FROM clientes cl"
                        + " WHERE EXISTS (SELECT 1 FROM cuentas_por_cobrar cx WHERE cx.cliente_id = cl.id)" + where
                        + " ORDER BY cl.razon_social LIMIT :limit",
                params,
                (rs, n) -> new OpcionResponse(rs.getLong("id"), rs.getString("razon_social"), rs.getString("nit_ci")));
    }

    /** Serializa la numeración de recibos dentro de la transacción (empresa = base propia). */
    public void bloquearNumeracionRecibos() {
        jdbc.getJdbcOperations().queryForList("SELECT pg_advisory_xact_lock(290029)");
    }

    private static String filtros(String search, String estado, Long clienteId, LocalDate hoy, MapSqlParameterSource params) {
        StringBuilder sb = new StringBuilder(" WHERE 1 = 1");
        if (search != null && !search.isBlank()) {
            params.addValue("term", likeTerm(search));
            sb.append(" AND (LOWER(cl.razon_social) LIKE :term ESCAPE '!' OR LOWER(cl.nit_ci) LIKE :term ESCAPE '!'"
                    + " OR CAST(v.numero_factura AS VARCHAR) LIKE :term ESCAPE '!')");
        }
        if (clienteId != null) {
            params.addValue("clienteId", clienteId);
            sb.append(" AND cx.cliente_id = :clienteId");
        }
        if (estado != null && !estado.isBlank()) {
            String e = estado.trim().toUpperCase(Locale.ROOT);
            if ("VENCIDA".equals(e)) {
                params.addValue("hoy", Date.valueOf(hoy));
                sb.append(" AND").append(CON_SALDO).append(" AND cx.fecha_vencimiento < :hoy");
            } else if ("ANULADA".equals(e)) {
                sb.append(" AND UPPER(cx.estado) IN ('ANULADA', 'ANULADO')");
            } else {
                params.addValue("estado", e);
                sb.append(" AND UPPER(cx.estado) = :estado");
            }
        }
        return sb.toString();
    }

    private static String likeTerm(String search) {
        String escaped = search.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    private static LocalDate fecha(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
