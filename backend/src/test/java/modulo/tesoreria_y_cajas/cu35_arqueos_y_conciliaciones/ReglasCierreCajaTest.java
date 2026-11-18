package modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones;

import modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.service.ReglasCierreCaja;
import modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.service.ReglasCierreCaja.ResultadoCierre;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasCierreCajaTest {

    private static final BigDecimal ESPERADO = new BigDecimal("1370.25");

    @Test
    void cierreCuadradoNoExigeObservaciones() {
        ResultadoCierre r = ReglasCierreCaja.evaluar(ESPERADO, new BigDecimal("1370.25"), "  ");

        assertThat(r.diferencia().signum()).isZero();
        assertThat(r.montoContado()).isEqualByComparingTo("1370.25");
        assertThat(r.observaciones()).isNull();
    }

    @Test
    void sobranteYFaltanteTienenSignoYExigenObservaciones() {
        assertThat(ReglasCierreCaja.evaluar(ESPERADO, new BigDecimal("1400"), "Billete de más").diferencia())
                .isEqualByComparingTo("29.75");
        assertThat(ReglasCierreCaja.evaluar(ESPERADO, new BigDecimal("1300"), "Falta cambio").diferencia())
                .isEqualByComparingTo("-70.25");

        assertThatThrownBy(() -> ReglasCierreCaja.evaluar(ESPERADO, new BigDecimal("1300"), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("faltante de Bs 70.25")
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void montoContadoNegativoONuloEsInvalido() {
        assertThatThrownBy(() -> ReglasCierreCaja.evaluar(ESPERADO, new BigDecimal("-1"), "x"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> ReglasCierreCaja.evaluar(ESPERADO, null, "x"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void lasObservacionesDeCierreSeAgreganALasDeApertura() {
        assertThat(ReglasCierreCaja.observacionesFinales("Turno mañana", "Faltó cambio"))
                .isEqualTo("Turno mañana | Cierre: Faltó cambio");
        assertThat(ReglasCierreCaja.observacionesFinales(null, "Faltó cambio")).isEqualTo("Cierre: Faltó cambio");
        assertThat(ReglasCierreCaja.observacionesFinales("Turno mañana", null)).isEqualTo("Turno mañana");
        assertThat(ReglasCierreCaja.observacionesFinales(null, " ")).isNull();
    }

    @Test
    void describeLaDiferencia() {
        assertThat(ReglasCierreCaja.descripcion(new BigDecimal("0.00"))).isEqualTo("cuadrada");
        assertThat(ReglasCierreCaja.descripcion(new BigDecimal("-4.50"))).isEqualTo("faltante de Bs 4.50");
    }
}
