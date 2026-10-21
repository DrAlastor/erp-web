package modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.repository;

import lombok.RequiredArgsConstructor;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.AlertaStock;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Caja;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Cobrar;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Pagar;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.PedidosPorEstado;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.TopProducto;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.SucursalOpcionResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas agregadas de solo lectura del dashboard ejecutivo (CU39) sobre tablas de otros
 * modulos: ventas, venta_detalles, pedidos, cotizaciones, clientes, cuentas_por_cobrar,
 * cuentas_por_pagar (+ compras para la sucursal), productos, inventario_sucursal,
 * turnos_caja y movimientos_caja. Nada se escribe.
 *
 * <p>{@code sucursalId} filtra donde la tabla lo permite; cotizaciones y clientes no tienen
 * sucursal y siempre son globales.
 */
@Repository
@RequiredArgsConstructor
public class DashboardConsultaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public record Suma(BigDecimal total, long cantidad) {}

    public record ResumenStock(long productosActivos, long bajoStockMinimo, long sinStock, BigDecimal valor) {}

    private static final String VENTA_VALIDA = " UPPER(v.estado) <> 'ANULADA'";

    /** Total y cantidad de ventas no anuladas con fecha en [desde, hasta). */
    public Suma ventas(LocalDateTime desde, LocalDateTime hasta, Long sucursalId) {
        MapSqlParameterSource p = rango(desde, hasta);
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(v.total), 0) AS total, COUNT(*) AS cantidad FROM ventas v"
                        + " WHERE" + VENTA_VALIDA + " AND v.fecha >= :desde AND v.fecha < :hasta"
                        + sucursal("v.sucursal_id", sucursalId, p),
                p,
                (rs, n) -> new Suma(rs.getBigDecimal("total"), rs.getLong("cantidad")));
    }

    /** Total vendido por dia en [desde, hasta); los dias sin ventas no aparecen. */
    public Map<LocalDate, BigDecimal> ventasPorDia(LocalDateTime desde, LocalDateTime hasta, Long sucursalId) {
        MapSqlParameterSource p = rango(desde, hasta);
        Map<LocalDate, BigDecimal> resultado = new HashMap<>();
        jdbc.query(
                "SELECT CAST(v.fecha AS DATE) AS dia, SUM(v.total) AS total FROM ventas v"
                        + " WHERE" + VENTA_VALIDA + " AND v.fecha >= :desde AND v.fecha < :hasta"
                        + sucursal("v.sucursal_id", sucursalId, p)
                        + " GROUP BY CAST(v.fecha AS DATE)",
                p,
                rs -> {
                    resultado.put(rs.getDate("dia").toLocalDate(), rs.getBigDecimal("total"));
                });
        return resultado;
    }

    public List<TopProducto> topProductos(LocalDateTime desde, LocalDateTime hasta, Long sucursalId, int limit) {
        MapSqlParameterSource p = rango(desde, hasta).addValue("limit", limit);
        return jdbc.query(
                "SELECT d.producto_id, pr.nombre, SUM(d.cantidad) AS cantidad, SUM(d.subtotal) AS total"
                        + " FROM venta_detalles d"
                        + " JOIN ventas v ON v.id = d.venta_id"
                        + " LEFT JOIN productos pr ON pr.id = d.producto_id"
                        + " WHERE" + VENTA_VALIDA + " AND v.fecha >= :desde AND v.fecha < :hasta"
                        + sucursal("v.sucursal_id", sucursalId, p)
                        + " GROUP BY d.producto_id, pr.nombre ORDER BY total DESC, cantidad DESC LIMIT :limit",
                p,
                (rs, n) -> new TopProducto(
                        rs.getLong("producto_id"),
                        rs.getString("nombre"),
                        rs.getBigDecimal("cantidad"),
                        rs.getBigDecimal("total")));
    }

    public List<PedidosPorEstado> pedidosPorEstado(Long sucursalId) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        return jdbc.query(
                "SELECT UPPER(p.estado) AS estado, COUNT(*) AS cantidad FROM pedidos p WHERE 1 = 1"
                        + sucursal("p.sucursal_id", sucursalId, p)
                        + " GROUP BY UPPER(p.estado) ORDER BY cantidad DESC, estado",
                p,
                (rs, n) -> new PedidosPorEstado(rs.getString("estado"), rs.getLong("cantidad")));
    }

    /** Cotizaciones EMITIDA o ACEPTADA cuyo plazo de validez sigue corriendo hoy. */
    public long cotizacionesVigentes(LocalDate hoy) {
        return contar(
                "SELECT COUNT(*) FROM cotizaciones c WHERE UPPER(c.estado) IN ('EMITIDA', 'ACEPTADA')"
                        + " AND c.fecha + c.validez_dias >= :hoy",
                new MapSqlParameterSource("hoy", Date.valueOf(hoy)));
    }

    public long clientesActivos() {
        return contar("SELECT COUNT(*) FROM clientes WHERE activo = TRUE", new MapSqlParameterSource());
    }

    /** Cuotas por cobrar con saldo (no anuladas): saldo total, vencido y cantidad vencida. */
    public Cobrar cobrar(LocalDate hoy, Long sucursalId) {
        MapSqlParameterSource p = new MapSqlParameterSource("hoy", Date.valueOf(hoy));
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(c.saldo), 0) AS saldo,"
                        + " COALESCE(SUM(CASE WHEN c.fecha_vencimiento < :hoy THEN c.saldo END), 0) AS vencido,"
                        + " COUNT(CASE WHEN c.fecha_vencimiento < :hoy THEN 1 END) AS vencidas"
                        + " FROM cuentas_por_cobrar c WHERE c.saldo > 0 AND UPPER(c.estado) <> 'ANULADA'"
                        + sucursal("c.sucursal_id", sucursalId, p),
                p,
                (rs, n) -> new Cobrar(rs.getBigDecimal("saldo"), rs.getBigDecimal("vencido"), rs.getLong("vencidas")));
    }

    /** Cuotas por pagar con saldo; la sucursal se toma de la compra que las origino. */
    public Pagar pagar(LocalDate hoy, LocalDate hasta, Long sucursalId) {
        MapSqlParameterSource p = new MapSqlParameterSource("hoy", Date.valueOf(hoy))
                .addValue("hasta", Date.valueOf(hasta));
        return jdbc.queryForObject(
                "SELECT COALESCE(SUM(cx.saldo), 0) AS saldo,"
                        + " COALESCE(SUM(CASE WHEN cx.fecha_vencimiento < :hoy THEN cx.saldo END), 0) AS vencido,"
                        + " COALESCE(SUM(CASE WHEN cx.fecha_vencimiento BETWEEN :hoy AND :hasta"
                        + " THEN cx.saldo END), 0) AS por_vencer,"
                        + " COUNT(CASE WHEN cx.fecha_vencimiento < :hoy THEN 1 END) AS vencidas"
                        + " FROM cuentas_por_pagar cx LEFT JOIN compras co ON co.id = cx.compra_id"
                        + " WHERE cx.saldo > 0 AND UPPER(cx.estado) <> 'ANULADA'"
                        + sucursal("co.sucursal_id", sucursalId, p),
                p,
                (rs, n) -> new Pagar(
                        rs.getBigDecimal("saldo"),
                        rs.getBigDecimal("vencido"),
                        rs.getBigDecimal("por_vencer"),
                        rs.getLong("vencidas")));
    }

    /**
     * Productos activos, cuantos estan en o bajo su minimo (con stock), cuantos sin stock y
     * el valor del inventario a costo de compra.
     */
    public ResumenStock resumenStock(Long sucursalId) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        return jdbc.queryForObject(
                stock(sucursalId, p)
                        + " SELECT COUNT(*) AS activos,"
                        + " COUNT(CASE WHEN st.stock > 0 AND st.stock <= st.minimo THEN 1 END) AS bajo,"
                        + " COUNT(CASE WHEN st.stock <= 0 THEN 1 END) AS sin_stock,"
                        + " COALESCE(SUM(GREATEST(st.stock, 0) * st.costo), 0) AS valor FROM st",
                p,
                (rs, n) -> new ResumenStock(
                        rs.getLong("activos"), rs.getLong("bajo"), rs.getLong("sin_stock"), rs.getBigDecimal("valor")));
    }

    /** Los productos mas criticos: primero sin stock, luego los mas por debajo del minimo. */
    public List<AlertaStock> stockCritico(Long sucursalId, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource("limit", limit);
        return jdbc.query(
                stock(sucursalId, p)
                        + " SELECT st.id, st.codigo, st.nombre, st.stock, st.minimo FROM st"
                        + " WHERE st.stock <= 0 OR st.stock <= st.minimo"
                        + " ORDER BY CASE WHEN st.stock <= 0 THEN 0 ELSE 1 END, st.stock - st.minimo, st.nombre"
                        + " LIMIT :limit",
                p,
                (rs, n) -> new AlertaStock(
                        rs.getLong("id"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getBigDecimal("stock"),
                        rs.getBigDecimal("minimo")));
    }

    /**
     * Turnos abiertos y movimientos del dia en [desde, hasta). Los ingresos suman los
     * movimientos INGRESO mas las ventas al contado no anuladas registradas en un turno de
     * caja (las ventas POS no generan movimiento de caja); los egresos son los EGRESO.
     */
    public Caja caja(LocalDateTime desde, LocalDateTime hasta, Long sucursalId) {
        MapSqlParameterSource p = rango(desde, hasta);
        String sucTurno = sucursal("t.sucursal_id", sucursalId, p);
        long abiertos = contar(
                "SELECT COUNT(*) FROM turnos_caja t WHERE UPPER(t.estado) = 'ABIERTO'" + sucTurno, p);
        BigDecimal ingresos = suma(
                "SELECT COALESCE(SUM(m.monto), 0) FROM movimientos_caja m JOIN turnos_caja t ON t.id = m.turno_caja_id"
                        + " WHERE UPPER(m.tipo) = 'INGRESO' AND m.fecha_hora >= :desde AND m.fecha_hora < :hasta"
                        + sucTurno, p);
        BigDecimal ventasCaja = suma(
                "SELECT COALESCE(SUM(v.total), 0) FROM ventas v WHERE" + VENTA_VALIDA
                        + " AND v.turno_caja_id IS NOT NULL AND UPPER(v.tipo_venta) = 'CONTADO'"
                        + " AND v.fecha >= :desde AND v.fecha < :hasta"
                        + sucursal("v.sucursal_id", sucursalId, p), p);
        BigDecimal egresos = suma(
                "SELECT COALESCE(SUM(m.monto), 0) FROM movimientos_caja m JOIN turnos_caja t ON t.id = m.turno_caja_id"
                        + " WHERE UPPER(m.tipo) = 'EGRESO' AND m.fecha_hora >= :desde AND m.fecha_hora < :hasta"
                        + sucTurno, p);
        return new Caja(abiertos, ingresos.add(ventasCaja), egresos);
    }

    /** Sucursales activas para el selector (misma consulta que las opciones de la CU16). */
    public List<SucursalOpcionResponse> sucursalesActivas() {
        return jdbc.query(
                "SELECT id, nombre FROM sucursal WHERE activo = TRUE ORDER BY nombre",
                new MapSqlParameterSource(),
                (rs, n) -> new SucursalOpcionResponse(rs.getLong("id"), rs.getString("nombre")));
    }

    // ------------------------------------------------------------------ internos

    /**
     * CTE {@code st} con el stock por producto activo. Con sucursal: su fila de
     * inventario_sucursal y su minimo propio; sin sucursal: la suma de todas las sucursales
     * contra el minimo del producto. Sin fila de inventario el stock es 0.
     */
    private String stock(Long sucursalId, MapSqlParameterSource p) {
        if (sucursalId != null) {
            p.addValue("sucursalId", sucursalId);
            return "WITH st AS (SELECT pr.id, pr.codigo, pr.nombre, pr.costo_compra AS costo,"
                    + " COALESCE(i.stock_disponible, 0) AS stock, COALESCE(i.stock_minimo, pr.stock_minimo) AS minimo"
                    + " FROM productos pr LEFT JOIN inventario_sucursal i"
                    + " ON i.producto_id = pr.id AND i.sucursal_id = :sucursalId WHERE pr.activo = TRUE)";
        }
        return "WITH st AS (SELECT pr.id, pr.codigo, pr.nombre, pr.costo_compra AS costo,"
                + " COALESCE(SUM(i.stock_disponible), 0) AS stock, pr.stock_minimo AS minimo"
                + " FROM productos pr LEFT JOIN inventario_sucursal i ON i.producto_id = pr.id"
                + " WHERE pr.activo = TRUE"
                + " GROUP BY pr.id, pr.codigo, pr.nombre, pr.costo_compra, pr.stock_minimo)";
    }

    private static String sucursal(String columna, Long sucursalId, MapSqlParameterSource p) {
        if (sucursalId == null) return "";
        p.addValue("sucursalId", sucursalId);
        return " AND " + columna + " = :sucursalId";
    }

    private static MapSqlParameterSource rango(LocalDateTime desde, LocalDateTime hasta) {
        return new MapSqlParameterSource("desde", Timestamp.valueOf(desde))
                .addValue("hasta", Timestamp.valueOf(hasta));
    }

    private long contar(String sql, MapSqlParameterSource p) {
        Long n = jdbc.queryForObject(sql, p, Long.class);
        return n == null ? 0 : n;
    }

    private BigDecimal suma(String sql, MapSqlParameterSource p) {
        BigDecimal v = jdbc.queryForObject(sql, p, BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }
}
