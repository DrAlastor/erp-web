package modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos;

import modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.service.CalculadoraCobroPos;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraCobroPosTest {

    @Test
    void importeDeLineaRedondeaADosDecimales() {
        assertThat(CalculadoraCobroPos.importeLinea(new BigDecimal("3"), new BigDecimal("10.335")))
                .isEqualByComparingTo("31.02");
        assertThat(CalculadoraCobroPos.importeLinea(new BigDecimal("1.5"), new BigDecimal("4.00")))
                .isEqualByComparingTo("6.00");
    }

    @Test
    void cambioEsLaDiferenciaEntreRecibidoYTotal() {
        assertThat(CalculadoraCobroPos.cambio(new BigDecimal("87.50"), new BigDecimal("100")))
                .isEqualByComparingTo("12.50");
    }

    @Test
    void sinMontoRecibidoSeAsumeImporteExacto() {
        assertThat(CalculadoraCobroPos.cambio(new BigDecimal("40"), null)).isEqualByComparingTo("0");
    }

    @Test
    void efectivoInsuficienteSeRechaza() {
        assertThatThrownBy(() -> CalculadoraCobroPos.cambio(new BigDecimal("50"), new BigDecimal("49.99")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no cubre");
    }
}
