package modulo.tesoreria_y_cajas.cu32_apertura_de_caja;

import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.service.CalculadoraArqueo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraArqueoTest {

    private static BigDecimal bs(String v) {
        return new BigDecimal(v);
    }

    @Test
    void esperadoSumaFondoVentasCobrosEIngresosYRestaEgresos() {
        assertThat(CalculadoraArqueo.esperado(bs("100"), bs("250.50"), bs("49.50"), bs("10"), bs("60")))
                .isEqualByComparingTo("350.00");
    }

    @Test
    void valoresNulosCuentanComoCero() {
        assertThat(CalculadoraArqueo.esperado(bs("80"), null, null, null, null)).isEqualByComparingTo("80");
        assertThat(CalculadoraArqueo.diferencia(null, bs("5"))).isEqualByComparingTo("-5");
    }

    @Test
    void diferenciaPositivaEsSobranteYNegativaFaltante() {
        assertThat(CalculadoraArqueo.diferencia(bs("110"), bs("100"))).isEqualByComparingTo("10");
        assertThat(CalculadoraArqueo.diferencia(bs("95"), bs("100"))).isEqualByComparingTo("-5");
        assertThat(CalculadoraArqueo.diferencia(bs("100"), bs("100"))).isEqualByComparingTo("0");
    }
}
