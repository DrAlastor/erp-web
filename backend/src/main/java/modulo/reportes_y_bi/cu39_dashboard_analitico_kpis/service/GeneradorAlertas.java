package modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.service;

import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Alerta;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Cobrar;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Comercial;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Inventario;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Pagar;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Ventas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reglas puras de las alertas del dashboard, ordenadas ALTA, MEDIA, INFO:
 * <ul>
 *   <li>CXP_VENCIDA (ALTA): hay cuotas por pagar vencidas.</li>
 *   <li>SIN_STOCK (ALTA): hay productos activos sin stock.</li>
 *   <li>CXC_VENCIDA (MEDIA): hay cuotas por cobrar vencidas.</li>
 *   <li>STOCK_BAJO (MEDIA): hay productos con stock en o bajo su minimo.</li>
 *   <li>VENTAS_A_LA_BAJA (MEDIA): el mes va {@value #UMBRAL_BAJA_PCT}% o mas por debajo del
 *       mismo tramo del mes anterior.</li>
 *   <li>PEDIDOS_PENDIENTES (INFO): hay pedidos de venta en estado PENDIENTE.</li>
 * </ul>
 * {@code funcionId} es el id de la funcion del menu lateral a la que lleva cada alerta.
 */
public final class GeneradorAlertas {

    public static final int UMBRAL_BAJA_PCT = 20;

    private static final List<String> SEVERIDADES = List.of("ALTA", "MEDIA", "INFO");

    private GeneradorAlertas() {}

    public static List<Alerta> generar(
            Ventas ventas, Comercial comercial, Cobrar cobrar, Pagar pagar, Inventario inventario) {
        List<Alerta> alertas = new ArrayList<>();
        if (pagar != null && pagar.cantidadVencidas() > 0) {
            alertas.add(new Alerta("CXP_VENCIDA", "ALTA",
                    pagar.cantidadVencidas() + " cuota(s) por pagar vencida(s) por Bs " + monto(pagar.vencido()),
                    "gestionar-cuentas-por-pagar"));
        }
        if (inventario != null && inventario.sinStock() > 0) {
            alertas.add(new Alerta("SIN_STOCK", "ALTA",
                    inventario.sinStock() + " producto(s) activo(s) sin stock", "consultar-kardex"));
        }
        if (cobrar != null && cobrar.cantidadVencidas() > 0) {
            alertas.add(new Alerta("CXC_VENCIDA", "MEDIA",
                    cobrar.cantidadVencidas() + " cuota(s) por cobrar vencida(s) por Bs " + monto(cobrar.vencido()),
                    "cuentas-por-cobrar-y-cobros"));
        }
        if (inventario != null && inventario.bajoStockMinimo() > 0) {
            alertas.add(new Alerta("STOCK_BAJO", "MEDIA",
                    inventario.bajoStockMinimo() + " producto(s) en o bajo su stock mínimo", "consultar-kardex"));
        }
        if (ventas != null && ventas.variacionPct() != null
                && ventas.variacionPct().compareTo(BigDecimal.valueOf(-UMBRAL_BAJA_PCT)) <= 0) {
            alertas.add(new Alerta("VENTAS_A_LA_BAJA", "MEDIA",
                    "Las ventas del mes van " + monto(ventas.variacionPct().abs())
                            + "% por debajo del mismo periodo del mes anterior",
                    "facturacion"));
        }
        long pendientes = pedidosPendientes(comercial);
        if (pendientes > 0) {
            alertas.add(new Alerta("PEDIDOS_PENDIENTES", "INFO",
                    pendientes + " pedido(s) de venta pendiente(s) de aprobación", "pedidos-de-venta"));
        }
        alertas.sort(Comparator.comparingInt(a -> SEVERIDADES.indexOf(a.severidad())));
        return alertas;
    }

    private static long pedidosPendientes(Comercial comercial) {
        if (comercial == null || comercial.pedidosPorEstado() == null) return 0;
        return comercial.pedidosPorEstado().stream()
                .filter(p -> "PENDIENTE".equalsIgnoreCase(p.estado()))
                .mapToLong(p -> p.cantidad())
                .sum();
    }

    private static String monto(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
