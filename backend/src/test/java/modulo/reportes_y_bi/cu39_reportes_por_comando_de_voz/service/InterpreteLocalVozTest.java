package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.AgrupacionVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.MetricaVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ConsultaReporteVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.repository.ReporteVozRepository.Entidad;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterpreteLocalVozTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    private static final List<Entidad> SUCURSALES = List.of(
            new Entidad(1L, "Sucursal Central Equipetrol", "Santa Cruz de la Sierra"),
            new Entidad(2L, "Sucursal Calacoto La Paz", "La Paz"));

    private static final List<Entidad> PRODUCTOS = List.of(
            new Entidad(1L, "Laptop ASUS ROG Strix SCAR 16 G634", "LAP-ROG-SCAR16"),
            new Entidad(2L, "Laptop ASUS TUF Gaming A15 FA507", "LAP-TUF-A15"),
            new Entidad(3L, "Laptop ASUS Zenbook 14 OLED UX3405", "LAP-ZEN-14"),
            new Entidad(5L, "Tarjeta de Video ASUS TUF Gaming RTX 4070 Ti SUPER 16GB", "GPU-TUF-4070TIS"));

    private final InterpreteLocalVoz interprete = new InterpreteLocalVoz();

    private ConsultaReporteVoz interpretar(String frase) {
        return interprete.interpretar(frase, SUCURSALES, PRODUCTOS, HOY).orElseThrow();
    }

    @Test
    void ventasDeLaSucursalCentralDelMesPasado() {
        ConsultaReporteVoz c = interpretar("Muéstrame las ventas de la sucursal Central del mes pasado");

        assertThat(c.getMetrica()).isEqualTo(MetricaVoz.VENTAS);
        assertThat(c.getSucursalId()).isEqualTo(1L);
        assertThat(c.getFechaInicio()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(c.getFechaFin()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(c.getAgrupacion()).isNull();
    }

    @Test
    void loQueVendimosEsVentasYSeReconoceLaCiudad() {
        ConsultaReporteVoz c = interpretar("lo que vendimos en La Paz esta semana por día");

        assertThat(c.getMetrica()).isEqualTo(MetricaVoz.VENTAS);
        assertThat(c.getSucursalId()).isEqualTo(2L);
        assertThat(c.getAgrupacion()).isEqualTo(AgrupacionVoz.DIA);
        assertThat(c.getFechaInicio()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(c.getFechaFin()).isEqualTo(HOY);
    }

    @Test
    void topCincoProductosMasVendidos() {
        ConsultaReporteVoz c = interpretar("top cinco productos más vendidos de este año");

        assertThat(c.getMetrica()).isEqualTo(MetricaVoz.PRODUCTOS_VENDIDOS);
        assertThat(c.getAgrupacion()).isEqualTo(AgrupacionVoz.PRODUCTO);
        assertThat(c.getLimite()).isEqualTo(5);
        assertThat(c.getFechaInicio()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void mejoresClientesEsRankingDeVentasPorCliente() {
        ConsultaReporteVoz c = interpretar("los 3 mejores clientes");

        assertThat(c.getMetrica()).isEqualTo(MetricaVoz.VENTAS);
        assertThat(c.getAgrupacion()).isEqualTo(AgrupacionVoz.CLIENTE);
        assertThat(c.getLimite()).isEqualTo(3);
    }

    @Test
    void ventasDeUnProductoUsanElDetalleSoloSiElProductoEsInequivoco() {
        ConsultaReporteVoz tuf = interpretar("ventas de la laptop TUF en enero");
        assertThat(tuf.getMetrica()).isEqualTo(MetricaVoz.PRODUCTOS_VENDIDOS);
        assertThat(tuf.getProductoId()).isEqualTo(2L);
        assertThat(tuf.getAgrupacion()).isEqualTo(AgrupacionVoz.NINGUNA);
        assertThat(tuf.getFechaInicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(tuf.getFechaFin()).isEqualTo(LocalDate.of(2026, 1, 31));

        ConsultaReporteVoz laptop = interpretar("ventas de laptop");
        assertThat(laptop.getMetrica()).isEqualTo(MetricaVoz.VENTAS);
        assertThat(laptop.getProductoId()).isNull();
    }

    @Test
    void stockBajoElMinimoIgnoraFechas() {
        ConsultaReporteVoz c = interpretar("productos con stock bajo el mínimo de hoy");

        assertThat(c.getMetrica()).isEqualTo(MetricaVoz.STOCK);
        assertThat(c.getSoloCriticos()).isTrue();
        assertThat(c.getFechaInicio()).isNull();
    }

    @Test
    void cuentasPorCobrarVencidasYUltimosDias() {
        ConsultaReporteVoz cxc = interpretar("cuentas por cobrar vencidas por cliente");
        assertThat(cxc.getMetrica()).isEqualTo(MetricaVoz.CUENTAS_POR_COBRAR);
        assertThat(cxc.getSoloCriticos()).isTrue();
        assertThat(cxc.getAgrupacion()).isEqualTo(AgrupacionVoz.CLIENTE);

        ConsultaReporteVoz kardex = interpretar("movimientos de kardex de los últimos 30 días");
        assertThat(kardex.getMetrica()).isEqualTo(MetricaVoz.MOVIMIENTOS_KARDEX);
        assertThat(kardex.getFechaInicio()).isEqualTo(HOY.minusDays(29));
        assertThat(kardex.getLimite()).isNull();
    }

    @Test
    void mesSinAnioFuturoSeTomaDelAnioAnterior() {
        ConsultaReporteVoz c = interpretar("compras por proveedor de diciembre");

        assertThat(c.getMetrica()).isEqualTo(MetricaVoz.COMPRAS);
        assertThat(c.getAgrupacion()).isEqualTo(AgrupacionVoz.PROVEEDOR);
        assertThat(c.getFechaInicio()).isEqualTo(LocalDate.of(2025, 12, 1));
    }

    @Test
    void fraseSinIntencionReconocibleNoSeInterpreta() {
        assertThat(interprete.interpretar("eh mmm hola", SUCURSALES, PRODUCTOS, HOY)).isEmpty();
    }

    @Test
    void argumentosDeLaIaSoloAceptanValoresDelCatalogo() throws Exception {
        ObjectMapper json = new ObjectMapper();

        var valida = json.readTree("""
                {"metrica":"ventas","agrupacion":"SUCURSAL","fechaInicio":"2026-09-01",
                 "fechaFin":"2026-09-30T23:59:59","sucursalId":1.0,"limite":500}""");
        ConsultaReporteVoz consulta = ConsultaVozJson.convertir(valida).orElseThrow();
        assertThat(consulta.getMetrica()).isEqualTo(MetricaVoz.VENTAS);
        assertThat(consulta.getAgrupacion()).isEqualTo(AgrupacionVoz.SUCURSAL);
        assertThat(consulta.getFechaFin()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(consulta.getSucursalId()).isEqualTo(1L);
        assertThat(consulta.getLimite()).isEqualTo(100);

        var inyeccion = json.readTree("""
                {"metrica":"VENTAS; DROP TABLE ventas","sucursalId":"1 OR 1=1"}""");
        assertThat(ConsultaVozJson.convertir(inyeccion)).isEmpty();

        var sucursalComoTexto = json.readTree("""
                {"metrica":"STOCK","sucursalId":"1 OR 1=1","agrupacion":"x","fechaInicio":"ayer"}""");
        ConsultaReporteVoz stock = ConsultaVozJson.convertir(sucursalComoTexto).orElseThrow();
        assertThat(stock.getSucursalId()).isNull();
        assertThat(stock.getAgrupacion()).isNull();
        assertThat(stock.getFechaInicio()).isNull();
    }
}
