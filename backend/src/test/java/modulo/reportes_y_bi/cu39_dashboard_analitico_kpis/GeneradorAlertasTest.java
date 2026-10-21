package modulo.reportes_y_bi.cu39_dashboard_analitico_kpis;

import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Alerta;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Cobrar;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Comercial;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Inventario;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Pagar;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.PedidosPorEstado;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Ventas;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.service.DashboardEjecutivoService;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.service.GeneradorAlertas;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorAlertasTest {

    private static final BigDecimal CERO = BigDecimal.ZERO;

    private static Ventas ventas(String variacion) {
        return new Ventas(CERO, CERO, CERO, variacion == null ? null : new BigDecimal(variacion),
                0, CERO, List.of(), List.of());
    }

    private static Comercial comercial(long pendientes) {
        return new Comercial(List.of(new PedidosPorEstado("PENDIENTE", pendientes),
                new PedidosPorEstado("APROBADO", 4)), 0, 0);
    }

    @Test
    void sinProblemasNoHayAlertas() {
        List<Alerta> alertas = GeneradorAlertas.generar(
                ventas("5.00"), comercial(0), new Cobrar(CERO, CERO, 0),
                new Pagar(CERO, CERO, CERO, 0), new Inventario(10, 0, 0, CERO, List.of()));

        assertThat(alertas).isEmpty();
    }

    @Test
    void generaTodasLasAlertasOrdenadasPorSeveridadConSuFuncion() {
        List<Alerta> alertas = GeneradorAlertas.generar(
                ventas("-35.50"), comercial(3),
                new Cobrar(new BigDecimal("500"), new BigDecimal("200"), 2),
                new Pagar(new BigDecimal("900"), new BigDecimal("300"), CERO, 1),
                new Inventario(10, 4, 2, CERO, List.of()));

        assertThat(alertas).extracting(Alerta::tipo).containsExactly(
                "CXP_VENCIDA", "SIN_STOCK", "CXC_VENCIDA", "STOCK_BAJO", "VENTAS_A_LA_BAJA", "PEDIDOS_PENDIENTES");
        assertThat(alertas).extracting(Alerta::severidad).containsExactly(
                "ALTA", "ALTA", "MEDIA", "MEDIA", "MEDIA", "INFO");
        assertThat(alertas).extracting(Alerta::funcionId).containsExactly(
                "gestionar-cuentas-por-pagar", "consultar-kardex", "cuentas-por-cobrar-y-cobros",
                "consultar-kardex", "facturacion", "pedidos-de-venta");
        assertThat(alertas.get(0).mensaje()).contains("300.00");
        assertThat(alertas.get(5).mensaje()).startsWith("3 ");
    }

    @Test
    void ventasALaBajaSoloDesdeElUmbralYNuncaSinComparacion() {
        assertThat(GeneradorAlertas.generar(ventas("-19.99"), null, null, null, null)).isEmpty();
        assertThat(GeneradorAlertas.generar(ventas(null), null, null, null, null)).isEmpty();
        assertThat(GeneradorAlertas.generar(ventas("-20.00"), null, null, null, null))
                .extracting(Alerta::tipo).containsExactly("VENTAS_A_LA_BAJA");
    }

    @Test
    void variacionPorcentualYNullSinBase() {
        assertThat(DashboardEjecutivoService.variacion(new BigDecimal("150"), new BigDecimal("100")))
                .isEqualByComparingTo("50.00");
        assertThat(DashboardEjecutivoService.variacion(new BigDecimal("75"), new BigDecimal("100")))
                .isEqualByComparingTo("-25.00");
        assertThat(DashboardEjecutivoService.variacion(new BigDecimal("75"), BigDecimal.ZERO)).isNull();
    }
}
