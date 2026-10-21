package modulo.reportes_y_bi.cu37_reportes_predefinidos.repository;

import modulo.reportes_y_bi.cu37_reportes_predefinidos.catalogo.ReportePredefinido;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.FiltrosReporte;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.OpcionesReporteDto.Opcion;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de los reportes predefinidos sobre las tablas de ERP_Asus.
 *
 * <p>Son agregaciones de varias tablas, así que se escriben en SQL y no con entidades JPA.
 * Los alias de cada consulta coinciden con las claves de las columnas de
 * {@link ReportePredefinido}. Los filtros opcionales se agregan solo cuando vienen, porque
 * PostgreSQL no acepta {@code :parametro IS NULL} con un parámetro nulo sin tipo.
 */
@Repository
public class ReportesRepositorio {

    /** Las ventas anuladas no cuentan en ningún reporte. */
    private static final String VENTA_VIGENTE = "COALESCE(v.estado, '') <> 'ANULADA'";

    private final NamedParameterJdbcTemplate jdbc;

    public ReportesRepositorio(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> consultar(ReportePredefinido reporte, FiltrosReporte f) {
        return switch (reporte) {
            case VENTAS_POR_PERIODO -> ventasPorPeriodo(f);
            case VENTAS_POR_CAJERO -> ventasPorCajero(f);
            case PRODUCTOS_MAS_VENDIDOS -> productosMasVendidos(f);
            case EXISTENCIAS_POR_SUCURSAL -> existencias(f, false);
            case PRODUCTOS_BAJO_MINIMO -> existencias(f, true);
            case VALORIZACION_INVENTARIO -> valorizacion(f);
        };
    }

    public List<Opcion> sucursales() {
        return jdbc.query("SELECT id, nombre FROM sucursal WHERE COALESCE(activo, TRUE) ORDER BY nombre",
                (rs, i) -> new Opcion(rs.getInt("id"), rs.getString("nombre")));
    }

    public List<Opcion> categorias() {
        return jdbc.query("SELECT id, nombre FROM categorias WHERE COALESCE(activo, TRUE) ORDER BY nombre",
                (rs, i) -> new Opcion(rs.getInt("id"), rs.getString("nombre")));
    }

    private List<Map<String, Object>> ventasPorPeriodo(FiltrosReporte f) {
        Consulta q = new Consulta("""
                SELECT CAST(v.fecha AS DATE) AS dia,
                       COUNT(*)                      AS ventas,
                       COALESCE(SUM(v.subtotal), 0)  AS subtotal,
                       COALESCE(SUM(v.descuento), 0) AS descuento,
                       COALESCE(SUM(v.iva), 0)       AS iva,
                       COALESCE(SUM(v.total), 0)     AS total
                FROM ventas v
                WHERE\s""" + VENTA_VIGENTE);
        q.rangoDeFechas("v.fecha", f).sucursal("v.sucursal_id", f);
        return q.ejecutar(" GROUP BY CAST(v.fecha AS DATE) ORDER BY dia");
    }

    private List<Map<String, Object>> ventasPorCajero(FiltrosReporte f) {
        Consulta q = new Consulta("""
                SELECT COALESCE(NULLIF(u.fullname, ''), u.email, 'Sin cajero') AS cajero,
                       COUNT(*)                         AS ventas,
                       COALESCE(SUM(v.total), 0)        AS total,
                       ROUND(COALESCE(AVG(v.total), 0), 2) AS ticket_promedio
                FROM ventas v
                LEFT JOIN usuario u ON u.id = v.cajero_id
                WHERE\s""" + VENTA_VIGENTE);
        q.rangoDeFechas("v.fecha", f).sucursal("v.sucursal_id", f);
        return q.ejecutar(" GROUP BY COALESCE(NULLIF(u.fullname, ''), u.email, 'Sin cajero') ORDER BY total DESC");
    }

    private List<Map<String, Object>> productosMasVendidos(FiltrosReporte f) {
        Consulta q = new Consulta("""
                SELECT p.codigo                       AS codigo,
                       p.nombre                       AS producto,
                       COALESCE(SUM(d.cantidad), 0)   AS cantidad,
                       COALESCE(SUM(d.subtotal), 0)   AS importe
                FROM venta_detalles d
                JOIN ventas v    ON v.id = d.venta_id
                JOIN productos p ON p.id = d.producto_id
                WHERE\s""" + VENTA_VIGENTE);
        q.rangoDeFechas("v.fecha", f).sucursal("v.sucursal_id", f);
        q.parametros.addValue("limite", f.limite());
        return q.ejecutar(" GROUP BY p.id, p.codigo, p.nombre ORDER BY cantidad DESC, importe DESC LIMIT :limite");
    }

    private List<Map<String, Object>> existencias(FiltrosReporte f, boolean soloBajoMinimo) {
        String minimo = "COALESCE(i.stock_minimo, p.stock_minimo, 0)";
        Consulta q = new Consulta("""
                SELECT s.nombre             AS sucursal,
                       p.codigo             AS codigo,
                       p.nombre             AS producto,
                       c.nombre             AS categoria,
                       i.stock_disponible   AS stock,
                       %1$s                 AS minimo,
                       GREATEST(%1$s - i.stock_disponible, 0) AS faltante,
                       CASE WHEN i.stock_disponible <= 0 THEN 'SIN STOCK'
                            WHEN i.stock_disponible <= %1$s THEN 'BAJO MINIMO'
                            ELSE 'OK' END   AS estado
                FROM inventario_sucursal i
                JOIN productos p ON p.id = i.producto_id
                JOIN sucursal s  ON s.id = i.sucursal_id
                LEFT JOIN categorias c ON c.id = p.categoria_id
                WHERE COALESCE(p.activo, TRUE)""".formatted(minimo));
        if (soloBajoMinimo) {
            q.sql.append(" AND i.stock_disponible <= ").append(minimo);
        }
        q.sucursal("i.sucursal_id", f).categoria("p.categoria_id", f);
        return q.ejecutar(soloBajoMinimo
                ? " ORDER BY faltante DESC, s.nombre, p.nombre"
                : " ORDER BY s.nombre, p.nombre");
    }

    private List<Map<String, Object>> valorizacion(FiltrosReporte f) {
        Consulta q = new Consulta("""
                SELECT s.nombre                                       AS sucursal,
                       p.codigo                                       AS codigo,
                       p.nombre                                       AS producto,
                       i.stock_disponible                             AS stock,
                       COALESCE(p.costo_compra, 0)                    AS costo_unitario,
                       ROUND(i.stock_disponible * COALESCE(p.costo_compra, 0), 2) AS valor_costo,
                       COALESCE(p.precio_venta, 0)                    AS precio_venta,
                       ROUND(i.stock_disponible * COALESCE(p.precio_venta, 0), 2) AS valor_venta
                FROM inventario_sucursal i
                JOIN productos p ON p.id = i.producto_id
                JOIN sucursal s  ON s.id = i.sucursal_id
                WHERE COALESCE(p.activo, TRUE)""");
        q.sucursal("i.sucursal_id", f);
        return q.ejecutar(" ORDER BY valor_costo DESC, s.nombre, p.nombre");
    }

    /** Arma una consulta agregando solo los filtros presentes. */
    private final class Consulta {
        private final StringBuilder sql;
        private final MapSqlParameterSource parametros = new MapSqlParameterSource();

        Consulta(String base) {
            this.sql = new StringBuilder(base);
        }

        Consulta rangoDeFechas(String columna, FiltrosReporte f) {
            if (f.desde() != null) {
                sql.append(" AND ").append(columna).append(" >= :desde");
                parametros.addValue("desde", Timestamp.valueOf(f.desde().atStartOfDay()));
            }
            if (f.hasta() != null) {
                // Hasta inclusivo: todo el día indicado.
                sql.append(" AND ").append(columna).append(" < :hasta");
                parametros.addValue("hasta", Timestamp.valueOf(f.hasta().plusDays(1).atStartOfDay()));
            }
            return this;
        }

        Consulta sucursal(String columna, FiltrosReporte f) {
            if (f.sucursalId() != null) {
                sql.append(" AND ").append(columna).append(" = :sucursalId");
                parametros.addValue("sucursalId", f.sucursalId());
            }
            return this;
        }

        Consulta categoria(String columna, FiltrosReporte f) {
            if (f.categoriaId() != null) {
                sql.append(" AND ").append(columna).append(" = :categoriaId");
                parametros.addValue("categoriaId", f.categoriaId());
            }
            return this;
        }

        List<Map<String, Object>> ejecutar(String cola) {
            List<Map<String, Object>> filas = jdbc.queryForList(sql + cola, parametros);
            List<Map<String, Object>> normalizadas = new ArrayList<>(filas.size());
            for (Map<String, Object> fila : filas) {
                Map<String, Object> copia = new LinkedHashMap<>();
                fila.forEach((clave, valor) -> copia.put(clave.toLowerCase(), normalizar(valor)));
                normalizadas.add(copia);
            }
            return normalizadas;
        }
    }

    /** Fechas de JDBC a java.time, para que el JSON salga en ISO y no como milisegundos. */
    private static Object normalizar(Object valor) {
        if (valor instanceof Date d) return d.toLocalDate();
        if (valor instanceof Timestamp t) return t.toLocalDateTime();
        return valor;
    }
}
