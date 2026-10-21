package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp;

import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service.CalculadoraCuotas;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service.CalculadoraCuotas.Cuota;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraCuotasTest {

    private static final LocalDate PRIMERA = LocalDate.of(2026, 11, 15);

    @Test
    void repartePartesIgualesCuandoElTotalEsDivisible() {
        List<Cuota> cuotas = CalculadoraCuotas.calcular(new BigDecimal("900.00"), 3, PRIMERA, 30);

        assertThat(cuotas).extracting(Cuota::monto)
                .allSatisfy(m -> assertThat(m).isEqualByComparingTo("300.00"));
        assertThat(cuotas).extracting(Cuota::numero).containsExactly(1, 2, 3);
    }

    @Test
    void laUltimaCuotaAbsorbeElRedondeoYLaSumaEsExacta() {
        List<Cuota> cuotas = CalculadoraCuotas.calcular(new BigDecimal("100.00"), 3, PRIMERA, 30);

        assertThat(cuotas.get(0).monto()).isEqualByComparingTo("33.33");
        assertThat(cuotas.get(1).monto()).isEqualByComparingTo("33.33");
        assertThat(cuotas.get(2).monto()).isEqualByComparingTo("33.34");
        BigDecimal suma = cuotas.stream().map(Cuota::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo("100.00");
        assertThat(cuotas).allSatisfy(c -> assertThat(c.monto().scale()).isEqualTo(2));
    }

    @Test
    void sumaExactaConTotalesDeLosDatosSemilla() {
        List<Cuota> cuotas = CalculadoraCuotas.calcular(new BigDecimal("26800.00"), 7, PRIMERA, 15);

        BigDecimal suma = cuotas.stream().map(Cuota::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma).isEqualByComparingTo("26800.00");
        assertThat(cuotas.get(6).monto()).isEqualByComparingTo("3828.58");
    }

    @Test
    void lasFechasAvanzanSegunLosDiasEntreCuotas() {
        List<Cuota> cuotas = CalculadoraCuotas.calcular(new BigDecimal("50.00"), 3, PRIMERA, 15);

        assertThat(cuotas).extracting(Cuota::fechaVencimiento).containsExactly(
                LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 15));
    }

    @Test
    void unaSolaCuotaLlevaTodoElTotal() {
        List<Cuota> cuotas = CalculadoraCuotas.calcular(new BigDecimal("1234.56"), 1, PRIMERA, 30);

        assertThat(cuotas).hasSize(1);
        assertThat(cuotas.get(0).monto()).isEqualByComparingTo("1234.56");
        assertThat(cuotas.get(0).fechaVencimiento()).isEqualTo(PRIMERA);
    }

    @Test
    void rechazaTotalNoPositivoOInsuficienteParaLasCuotas() {
        assertThatThrownBy(() -> CalculadoraCuotas.calcular(BigDecimal.ZERO, 2, PRIMERA, 30))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        assertThatThrownBy(() -> CalculadoraCuotas.calcular(new BigDecimal("0.05"), 10, PRIMERA, 30))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void primerVencimientoPorDefectoUsaDiasDeCreditoO30() {
        LocalDate compra = LocalDate.of(2026, 1, 16);

        assertThat(CalculadoraCuotas.primerVencimientoPorDefecto(compra, 15)).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(CalculadoraCuotas.primerVencimientoPorDefecto(compra, null)).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(CalculadoraCuotas.primerVencimientoPorDefecto(compra, 0)).isEqualTo(compra);
    }
}
