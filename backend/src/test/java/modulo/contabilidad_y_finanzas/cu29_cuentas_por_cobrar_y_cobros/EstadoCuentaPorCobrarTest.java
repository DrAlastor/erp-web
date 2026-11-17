package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros;

import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.service.EstadoCuentaPorCobrar;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoCuentaPorCobrarTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 17);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void estadoSegunCobradoYSaldo() {
        assertThat(EstadoCuentaPorCobrar.estadoSegun(bd("0"), bd("100"))).isEqualTo("PENDIENTE");
        assertThat(EstadoCuentaPorCobrar.estadoSegun(bd("40"), bd("60"))).isEqualTo("PARCIAL");
        assertThat(EstadoCuentaPorCobrar.estadoSegun(bd("100"), bd("0.00"))).isEqualTo("PAGADO");
    }

    @Test
    void soloHayDeudaConSaldoYSinPagarNiAnular() {
        assertThat(EstadoCuentaPorCobrar.tieneDeuda("PENDIENTE", bd("10"))).isTrue();
        assertThat(EstadoCuentaPorCobrar.tieneDeuda("PARCIAL", bd("10"))).isTrue();
        assertThat(EstadoCuentaPorCobrar.tieneDeuda("ANULADA", bd("10"))).isFalse();
        assertThat(EstadoCuentaPorCobrar.tieneDeuda("PAGADO", bd("0"))).isFalse();
        assertThat(EstadoCuentaPorCobrar.tieneDeuda("PENDIENTE", bd("0"))).isFalse();
    }

    @Test
    void vencidaSeDerivaDeLaFecha() {
        assertThat(EstadoCuentaPorCobrar.vencida("PENDIENTE", bd("5"), HOY.minusDays(1), HOY)).isTrue();
        assertThat(EstadoCuentaPorCobrar.vencida("PENDIENTE", bd("5"), HOY, HOY)).isFalse();
        assertThat(EstadoCuentaPorCobrar.vencida("PAGADO", bd("0"), HOY.minusDays(9), HOY)).isFalse();
        assertThat(EstadoCuentaPorCobrar.diasVencida("PARCIAL", bd("5"), HOY.minusDays(45), HOY)).isEqualTo(45);
    }

    @Test
    void tramosDeAntiguedad() {
        assertThat(EstadoCuentaPorCobrar.tramo(1)).isZero();
        assertThat(EstadoCuentaPorCobrar.tramo(30)).isZero();
        assertThat(EstadoCuentaPorCobrar.tramo(31)).isEqualTo(1);
        assertThat(EstadoCuentaPorCobrar.tramo(90)).isEqualTo(2);
        assertThat(EstadoCuentaPorCobrar.tramo(91)).isEqualTo(3);
    }

    @Test
    void elEfectivoVaACajaYLoDemasABancos() {
        assertThat(EstadoCuentaPorCobrar.vaACaja("EFECTIVO")).isTrue();
        assertThat(EstadoCuentaPorCobrar.vaACaja("QR_SIMPLE")).isFalse();
        assertThat(EstadoCuentaPorCobrar.vaACaja("TRANSFERENCIA")).isFalse();
    }
}
