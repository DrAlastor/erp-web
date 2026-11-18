package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas;

import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.CalculadoraArqueo;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.CalculadoraArqueo.Arqueo;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.CalculadoraArqueo.Linea;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.CalculadoraArqueo.Partida;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraArqueoTest {

    private static Partida efectivo(String origen, String monto) {
        return new Partida(1L, "Efectivo", true, origen, new BigDecimal(monto));
    }

    private static Partida qr(String origen, String monto) {
        return new Partida(3L, "QR", false, origen, new BigDecimal(monto));
    }

    @Test
    void sinMovimientosElEsperadoEsElFondoDeApertura() {
        Arqueo a = CalculadoraArqueo.calcular(new BigDecimal("500"), List.of());

        assertThat(a.efectivoEsperado()).isEqualByComparingTo("500.00");
        assertThat(a.efectivoEsperado().scale()).isEqualTo(2);
        assertThat(a.porFormaPago()).isEmpty();
        assertThat(a.totalIngresos()).isEqualByComparingTo("0");
    }

    @Test
    void esperadoSumaVentasEIngresosEnEfectivoYRestaEgresos() {
        Arqueo a = CalculadoraArqueo.calcular(new BigDecimal("1000.00"), List.of(
                efectivo("VENTA", "350.50"),
                efectivo("INGRESO", "100.00"),
                efectivo("EGRESO", "80.25")));

        assertThat(a.ventasEfectivo()).isEqualByComparingTo("350.50");
        assertThat(a.ingresosEfectivo()).isEqualByComparingTo("100.00");
        assertThat(a.egresosEfectivo()).isEqualByComparingTo("80.25");
        assertThat(a.efectivoEsperado()).isEqualByComparingTo("1370.25");
    }

    @Test
    void otrasFormasDePagoNoAfectanElEfectivoPeroSiLosTotales() {
        Arqueo a = CalculadoraArqueo.calcular(new BigDecimal("200.00"), List.of(
                efectivo("INGRESO", "50.00"),
                qr("VENTA", "900.00"),
                qr("INGRESO", "40.00"),
                qr("EGRESO", "10.00")));

        assertThat(a.efectivoEsperado()).isEqualByComparingTo("250.00");
        assertThat(a.totalIngresos()).isEqualByComparingTo("90.00");
        assertThat(a.totalEgresos()).isEqualByComparingTo("10.00");
        assertThat(a.porFormaPago()).extracting(Linea::formaPagoId).containsExactly(1L, 3L);
        Linea lineaQr = a.porFormaPago().get(1);
        assertThat(lineaQr.ventas()).isEqualByComparingTo("900.00");
        assertThat(lineaQr.ingresos()).isEqualByComparingTo("40.00");
        assertThat(lineaQr.egresos()).isEqualByComparingTo("10.00");
    }

    @Test
    void agrupaPartidasRepetidasDeLaMismaFormaDePagoEIgnoraOrigenesDesconocidos() {
        Arqueo a = CalculadoraArqueo.calcular(BigDecimal.ZERO, List.of(
                efectivo("ingreso", "10.00"),
                efectivo("INGRESO", "5.00"),
                efectivo("OTRO", "999.00")));

        assertThat(a.porFormaPago()).hasSize(1);
        assertThat(a.ingresosEfectivo()).isEqualByComparingTo("15.00");
        assertThat(a.efectivoEsperado()).isEqualByComparingTo("15.00");
    }

    @Test
    void elEsperadoPuedeQuedarNegativoSiLosEgresosHistoricosSuperanElFondo() {
        // Los reembolsos de la HU-20 registran egresos sin validar disponible.
        assertThat(CalculadoraArqueo.efectivoEsperado(
                new BigDecimal("100"), null, null, new BigDecimal("150")))
                .isEqualByComparingTo("-50.00");
    }

    @Test
    void diferenciaEsContadoMenosEsperado() {
        assertThat(CalculadoraArqueo.diferencia(new BigDecimal("1010"), new BigDecimal("1000.00")))
                .isEqualByComparingTo("10.00");
        assertThat(CalculadoraArqueo.diferencia(new BigDecimal("995.5"), new BigDecimal("1000")))
                .isEqualByComparingTo("-4.50");
        assertThat(CalculadoraArqueo.diferencia(new BigDecimal("1000"), new BigDecimal("1000.00")).signum())
                .isZero();
    }
}
