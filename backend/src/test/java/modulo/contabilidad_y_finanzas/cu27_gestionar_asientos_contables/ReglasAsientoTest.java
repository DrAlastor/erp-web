package modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables;

import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AsientoRequest.Linea;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.service.ReglasAsiento;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.service.ReglasAsiento.AsientoInvalido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasAsientoTest {

    private static Linea debe(String monto) {
        return new Linea(1L, null, new BigDecimal(monto), null);
    }

    private static Linea haber(String monto) {
        return new Linea(2L, null, null, new BigDecimal(monto));
    }

    @Test
    void asientoCuadradoDevuelveTotales() {
        var t = ReglasAsiento.validarLineas(List.of(debe("100.50"), haber("60.25"), haber("40.25")));
        assertThat(t.debe()).isEqualByComparingTo("100.50");
        assertThat(t.haber()).isEqualByComparingTo("100.50");
    }

    @Test
    void asientoDescuadradoSeRechazaConLaDiferencia() {
        assertThatThrownBy(() -> ReglasAsiento.validarLineas(List.of(debe("100"), haber("90"))))
                .isInstanceOf(AsientoInvalido.class)
                .hasMessageContaining("no cuadra")
                .hasMessageContaining("10.00");
    }

    @Test
    void unaLineaConDebeYHaberSeRechaza() {
        Linea ambas = new Linea(1L, null, new BigDecimal("5"), new BigDecimal("5"));
        assertThatThrownBy(() -> ReglasAsiento.validarLineas(List.of(ambas, haber("5"), debe("5"))))
                .isInstanceOf(AsientoInvalido.class).hasMessageContaining("Línea 1");
    }

    @Test
    void unaLineaSinImporteSeRechaza() {
        Linea vacia = new Linea(1L, null, BigDecimal.ZERO, null);
        assertThatThrownBy(() -> ReglasAsiento.validarLineas(List.of(debe("5"), haber("5"), vacia)))
                .isInstanceOf(AsientoInvalido.class).hasMessageContaining("Línea 3").hasMessageContaining("falta");
    }

    @Test
    void hacenFaltaDosLineasYAmbosLados() {
        assertThatThrownBy(() -> ReglasAsiento.validarLineas(List.of(debe("5"))))
                .isInstanceOf(AsientoInvalido.class).hasMessageContaining("dos líneas");
        assertThatThrownBy(() -> ReglasAsiento.validarLineas(List.of(debe("5"), debe("5"))))
                .isInstanceOf(AsientoInvalido.class).hasMessageContaining("debe y otra al haber");
    }

    @Test
    void importesNegativosSeRechazan() {
        assertThatThrownBy(() -> ReglasAsiento.validarLineas(List.of(debe("-5"), haber("-5"))))
                .isInstanceOf(AsientoInvalido.class).hasMessageContaining("negativos");
    }

    @Test
    void soloLosTiposManualesSeAceptan() {
        assertThat(ReglasAsiento.tipoManual(" ajuste ")).isEqualTo("AJUSTE");
        assertThat(ReglasAsiento.esManual("TRASPASO")).isTrue();
        assertThat(ReglasAsiento.esManual("VENTAS")).isFalse();
        assertThat(ReglasAsiento.esManual("REVERSION")).isFalse();
        assertThatThrownBy(() -> ReglasAsiento.tipoManual("VENTAS")).isInstanceOf(AsientoInvalido.class);
    }
}
