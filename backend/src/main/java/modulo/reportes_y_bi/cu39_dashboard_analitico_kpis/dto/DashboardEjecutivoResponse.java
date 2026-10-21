package modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Foto ejecutiva de la empresa (HU-36). Cada bloque se calcula por separado: si su
 * consulta falla, el bloque llega en ceros/listas vacias y el resto sigue intacto.
 * Montos en Bs con 2 decimales; {@code sucursalId} null = todas las sucursales.
 */
public record DashboardEjecutivoResponse(
        LocalDateTime generadoEn,
        Long sucursalId,
        Ventas ventas,
        Comercial comercial,
        Cobrar cobrar,
        Pagar pagar,
        Inventario inventario,
        Caja caja,
        List<Alerta> alertas) {

    /**
     * Ventas no anuladas. {@code mesAnterior} es el mismo tramo del mes anterior (del dia 1
     * al mismo dia del mes), para que la variacion compare periodos equivalentes;
     * {@code variacionPct} es null si ese tramo no tuvo ventas.
     */
    public record Ventas(
            BigDecimal hoy,
            BigDecimal mesActual,
            BigDecimal mesAnterior,
            BigDecimal variacionPct,
            long cantidadMes,
            BigDecimal ticketPromedioMes,
            List<PuntoSerie> serieDiaria,
            List<TopProducto> topProductos) {}

    public record PuntoSerie(LocalDate fecha, BigDecimal total) {}

    public record TopProducto(Long productoId, String nombre, BigDecimal cantidad, BigDecimal total) {}

    public record Comercial(
            List<PedidosPorEstado> pedidosPorEstado, long cotizacionesVigentes, long clientesActivos) {}

    public record PedidosPorEstado(String estado, long cantidad) {}

    public record Cobrar(BigDecimal saldoTotal, BigDecimal vencido, long cantidadVencidas) {}

    public record Pagar(
            BigDecimal saldoTotal, BigDecimal vencido, BigDecimal porVencer7Dias, long cantidadVencidas) {}

    public record Inventario(
            long productosActivos,
            long bajoStockMinimo,
            long sinStock,
            BigDecimal valorInventario,
            List<AlertaStock> alertas) {}

    public record AlertaStock(
            Long productoId, String codigo, String nombre, BigDecimal stock, BigDecimal stockMinimo) {}

    public record Caja(long turnosAbiertos, BigDecimal ingresosHoy, BigDecimal egresosHoy) {}

    /**
     * Aviso clicable. {@code tipo}: CXP_VENCIDA, CXC_VENCIDA, STOCK_BAJO, SIN_STOCK,
     * PEDIDOS_PENDIENTES o VENTAS_A_LA_BAJA; {@code severidad}: ALTA, MEDIA o INFO;
     * {@code funcionId}: id de la funcion del menu a la que navega.
     */
    public record Alerta(String tipo, String severidad, String mensaje, String funcionId) {}
}
