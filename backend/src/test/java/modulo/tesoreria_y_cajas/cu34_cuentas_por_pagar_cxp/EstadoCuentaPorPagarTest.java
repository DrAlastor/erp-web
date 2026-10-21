package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp;

import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.ResumenCxpResponse.RangoAntiguedad;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.entity.CuentaPorPagar;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service.EstadoCuentaPorPagar;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service.EstadoCuentaPorPagar.SaldoVencido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoCuentaPorPagarTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 21);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void estadoSegunPagosYSaldo() {
        assertThat(EstadoCuentaPorPagar.estadoSegun(bd("0.00"), bd("100.00"))).isEqualTo(CuentaPorPagar.PENDIENTE);
        assertThat(EstadoCuentaPorPagar.estadoSegun(bd("40.00"), bd("60.00"))).isEqualTo(CuentaPorPagar.PARCIAL);
        assertThat(EstadoCuentaPorPagar.estadoSegun(bd("100.00"), bd("0.00"))).isEqualTo(CuentaPorPagar.PAGADA);
    }

    @Test
    void vencidaSoloConSaldoYVencimientoAnteriorAHoy() {
        assertThat(EstadoCuentaPorPagar.vencida(bd("10"), HOY.minusDays(1), HOY)).isTrue();
        assertThat(EstadoCuentaPorPagar.vencida(bd("10"), HOY, HOY)).isFalse();
        assertThat(EstadoCuentaPorPagar.vencida(bd("0"), HOY.minusDays(40), HOY)).isFalse();
    }

    @Test
    void diasVencidoEsCeroSiNoEstaVencida() {
        assertThat(EstadoCuentaPorPagar.diasVencido(bd("10"), HOY.minusDays(45), HOY)).isEqualTo(45);
        assertThat(EstadoCuentaPorPagar.diasVencido(bd("10"), HOY.plusDays(3), HOY)).isZero();
        assertThat(EstadoCuentaPorPagar.diasVencido(bd("0"), HOY.minusDays(3), HOY)).isZero();
    }

    @Test
    void rangosDeAntiguedadEnSusLimites() {
        assertThat(EstadoCuentaPorPagar.rango(1)).isEqualTo("0-30");
        assertThat(EstadoCuentaPorPagar.rango(30)).isEqualTo("0-30");
        assertThat(EstadoCuentaPorPagar.rango(31)).isEqualTo("31-60");
        assertThat(EstadoCuentaPorPagar.rango(60)).isEqualTo("31-60");
        assertThat(EstadoCuentaPorPagar.rango(61)).isEqualTo("61-90");
        assertThat(EstadoCuentaPorPagar.rango(90)).isEqualTo("61-90");
        assertThat(EstadoCuentaPorPagar.rango(91)).isEqualTo("90+");
    }

    @Test
    void antiguedadAgrupaSaldosVencidosYSiempreTraeLosCuatroRangos() {
        List<RangoAntiguedad> rangos = EstadoCuentaPorPagar.antiguedad(List.of(
                new SaldoVencido(HOY.minusDays(5), bd("100.00")),
                new SaldoVencido(HOY.minusDays(30), bd("50.50")),
                new SaldoVencido(HOY.minusDays(120), bd("200.00")),
                new SaldoVencido(HOY.plusDays(2), bd("999.00"))), HOY);

        assertThat(rangos).extracting(RangoAntiguedad::rango).containsExactly("0-30", "31-60", "61-90", "90+");
        assertThat(rangos.get(0).monto()).isEqualByComparingTo("150.50");
        assertThat(rangos.get(0).cantidad()).isEqualTo(2);
        assertThat(rangos.get(1).monto()).isEqualByComparingTo("0.00");
        assertThat(rangos.get(1).cantidad()).isZero();
        assertThat(rangos.get(3).monto()).isEqualByComparingTo("200.00");
        assertThat(rangos.get(3).cantidad()).isEqualTo(1);
    }
}
