package modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.repository;

import lombok.RequiredArgsConstructor;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AsientoResponse;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AsientoResumenResponse;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.CuentaOpcionResponse;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.service.ReglasAsiento;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Lecturas de la CU27 (Libro Diario): cabeceras con periodo y autor, líneas con su
 * cuenta y las cuentas de movimiento para el formulario. SQL propio, como en la CU34,
 * para leer usuario, empleados y periodos sin depender del mapeo JPA de otros módulos.
 */
@Repository
@RequiredArgsConstructor
public class AsientoConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Cabecera con los datos que el detalle necesita además de la fila del listado. */
    public record Cabecera(AsientoResumenResponse resumen, String estadoPeriodo, java.time.LocalDateTime fechaCreacion) {}

    private static final String FROM = """
             FROM asientos_contables a
             LEFT JOIN periodos_contables p ON p.id = a.periodo_contable_id
             LEFT JOIN usuario u ON u.id = a.creado_por
             LEFT JOIN empleados e ON e.id = u.empleado_id
            """;

    private static final String SELECT = """
            SELECT a.id, a.numero_asiento, a.fecha, a.tipo_asiento, a.glosa, a.total_debe,
                   a.total_haber, a.estado, a.fecha_creacion, p.nombre AS periodo,
                   p.estado AS estado_periodo,
                   COALESCE(NULLIF(TRIM(CONCAT(e.nombre, ' ', e.apellido)), ''), u.email) AS creado_por
            """ + FROM;

    private static final RowMapper<Cabecera> CABECERA = (rs, n) -> {
        String tipo = rs.getString("tipo_asiento");
        Timestamp creacion = rs.getTimestamp("fecha_creacion");
        return new Cabecera(
                new AsientoResumenResponse(
                        rs.getLong("id"),
                        rs.getString("numero_asiento"),
                        fecha(rs.getDate("fecha")),
                        tipo,
                        rs.getString("glosa"),
                        rs.getBigDecimal("total_debe"),
                        rs.getBigDecimal("total_haber"),
                        rs.getString("estado"),
                        ReglasAsiento.esManual(tipo),
                        rs.getString("periodo"),
                        rs.getString("creado_por")),
                rs.getString("estado_periodo"),
                creacion == null ? null : creacion.toLocalDateTime());
    };

    /** Página del Libro Diario, del asiento más reciente al más antiguo. */
    public List<AsientoResumenResponse> listar(Filtro filtro, int limit, long offset) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = where(filtro, params);
        params.addValue("limit", limit).addValue("offset", offset);
        return jdbc.query(SELECT + where + " ORDER BY a.fecha DESC, a.id DESC LIMIT :limit OFFSET :offset",
                        params, CABECERA)
                .stream().map(Cabecera::resumen).toList();
    }

    public long contar(Filtro filtro) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = where(filtro, params);
        Long total = jdbc.queryForObject("SELECT COUNT(*)" + FROM + where, params, Long.class);
        return total == null ? 0 : total;
    }

    public Optional<Cabecera> cabecera(Long id) {
        return jdbc.query(SELECT + " WHERE a.id = :id", new MapSqlParameterSource("id", id), CABECERA)
                .stream().findFirst();
    }

    public List<AsientoResponse.Linea> lineas(Long asientoId) {
        return jdbc.query(
                """
                SELECT d.id, d.cuenta_contable_id, c.codigo, c.nombre, d.glosa, d.debe, d.haber
                FROM asiento_detalles d
                LEFT JOIN cuentas_contables c ON c.id = d.cuenta_contable_id
                WHERE d.asiento_id = :id
                ORDER BY d.id
                """,
                new MapSqlParameterSource("id", asientoId),
                (rs, n) -> new AsientoResponse.Linea(
                        rs.getLong("id"),
                        rs.getLong("cuenta_contable_id"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getString("glosa"),
                        rs.getBigDecimal("debe"),
                        rs.getBigDecimal("haber")));
    }

    /** Cuentas activas que admiten movimiento, por código o nombre. */
    public List<CuentaOpcionResponse> cuentasDeMovimiento(String search, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", limit);
        String where = " WHERE activo = TRUE AND permite_movimiento = TRUE";
        if (search != null && !search.isBlank()) {
            params.addValue("term", likeTerm(search));
            where += " AND (LOWER(codigo) LIKE :term ESCAPE '!' OR LOWER(nombre) LIKE :term ESCAPE '!')";
        }
        return jdbc.query(
                "SELECT id, codigo, nombre, tipo, naturaleza FROM cuentas_contables" + where
                        + " ORDER BY codigo LIMIT :limit",
                params,
                (rs, n) -> new CuentaOpcionResponse(
                        rs.getLong("id"), rs.getString("codigo"), rs.getString("nombre"),
                        rs.getString("tipo"), rs.getString("naturaleza")));
    }

    /** Filtros del listado; los vacíos no filtran. */
    public record Filtro(String search, String tipo, String estado, String origen, LocalDate desde, LocalDate hasta) {}

    private static String where(Filtro f, MapSqlParameterSource params) {
        StringBuilder sb = new StringBuilder(" WHERE 1 = 1");
        if (f.search() != null && !f.search().isBlank()) {
            params.addValue("term", likeTerm(f.search()));
            sb.append(" AND (LOWER(a.numero_asiento) LIKE :term ESCAPE '!' OR LOWER(a.glosa) LIKE :term ESCAPE '!')");
        }
        if (f.tipo() != null && !f.tipo().isBlank()) {
            params.addValue("tipo", f.tipo().trim().toUpperCase(Locale.ROOT));
            sb.append(" AND UPPER(a.tipo_asiento) = :tipo");
        }
        if (f.estado() != null && !f.estado().isBlank()) {
            params.addValue("estado", f.estado().trim().toUpperCase(Locale.ROOT));
            sb.append(" AND UPPER(a.estado) = :estado");
        }
        if (f.origen() != null && !f.origen().isBlank()) {
            params.addValue("manuales", ReglasAsiento.TIPOS_MANUALES);
            boolean manual = "MANUAL".equalsIgnoreCase(f.origen().trim());
            sb.append(manual ? " AND UPPER(a.tipo_asiento) IN (:manuales)"
                    : " AND UPPER(a.tipo_asiento) NOT IN (:manuales)");
        }
        if (f.desde() != null) {
            params.addValue("desde", Date.valueOf(f.desde()));
            sb.append(" AND a.fecha >= :desde");
        }
        if (f.hasta() != null) {
            params.addValue("hasta", Date.valueOf(f.hasta()));
            sb.append(" AND a.fecha <= :hasta");
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
