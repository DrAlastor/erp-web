package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasPlanCuentasTest {

    @Test
    void validaElFormatoDelCodigo() {
        assertThat(ReglasPlanCuentas.codigoValido("1")).isTrue();
        assertThat(ReglasPlanCuentas.codigoValido("1.1.1.01")).isTrue();
        assertThat(ReglasPlanCuentas.codigoValido("1..1")).isFalse();
        assertThat(ReglasPlanCuentas.codigoValido("1.1.")).isFalse();
        assertThat(ReglasPlanCuentas.codigoValido("A.1")).isFalse();
        assertThat(ReglasPlanCuentas.codigoValido(null)).isFalse();
    }

    @Test
    void elNivelEsLaCantidadDeSegmentos() {
        assertThat(ReglasPlanCuentas.nivel("1")).isEqualTo(1);
        assertThat(ReglasPlanCuentas.nivel("1.1.1.01")).isEqualTo(4);
    }

    @Test
    void unaSubcuentaAgregaExactamenteUnSegmento() {
        assertThat(ReglasPlanCuentas.esHijoDirecto("1.1", "1.1.3")).isTrue();
        assertThat(ReglasPlanCuentas.esHijoDirecto("1.1", "1.1.3.01")).isFalse();
        assertThat(ReglasPlanCuentas.esHijoDirecto("1.1", "1.10")).isFalse();
        assertThat(ReglasPlanCuentas.esHijoDirecto("1", "2.1")).isFalse();
    }

    @Test
    void naturalezaPorDefectoSegunElTipo() {
        assertThat(ReglasPlanCuentas.naturalezaPorDefecto("ACTIVO")).isEqualTo("DEUDORA");
        assertThat(ReglasPlanCuentas.naturalezaPorDefecto("EGRESO")).isEqualTo("DEUDORA");
        assertThat(ReglasPlanCuentas.naturalezaPorDefecto("PASIVO")).isEqualTo("ACREEDORA");
        assertThat(ReglasPlanCuentas.naturalezaPorDefecto("PATRIMONIO")).isEqualTo("ACREEDORA");
        assertThat(ReglasPlanCuentas.naturalezaPorDefecto("INGRESO")).isEqualTo("ACREEDORA");
    }

    @Test
    void reconoceLasCuentasDeIntegracionDeVentas() {
        assertThat(ReglasPlanCuentas.esCuentaIntegracion("1.1.1.01")).isTrue();
        assertThat(ReglasPlanCuentas.esCuentaIntegracion("4.1.1.01")).isTrue();
        // Las de compras (CU28) también: inventario, crédito fiscal y cuentas por pagar.
        assertThat(ReglasPlanCuentas.esCuentaIntegracion("1.1.3.01")).isTrue();
        assertThat(ReglasPlanCuentas.esCuentaIntegracion("1.1.4.01")).isTrue();
        assertThat(ReglasPlanCuentas.esCuentaIntegracion("2.1.1.01")).isTrue();
        assertThat(ReglasPlanCuentas.esCuentaIntegracion("5.1.1.01")).isFalse();
    }
}
