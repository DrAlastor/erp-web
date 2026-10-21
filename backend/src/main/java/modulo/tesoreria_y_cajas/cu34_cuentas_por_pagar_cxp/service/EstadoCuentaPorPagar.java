package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service;

import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.ResumenCxpResponse.RangoAntiguedad;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.entity.CuentaPorPagar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas puras de estado, vencimiento y antiguedad de las cuotas por pagar.
 *
 * <p>Estado almacenado: PAGADA si el saldo llego a cero, PARCIAL si ya tiene pagos,
 * PENDIENTE si no. Vencida (derivada): saldo positivo y vencimiento anterior a hoy. Los
 * rangos de antiguedad se cuentan por dias de atraso: 0-30 (1 a 30), 31-60, 61-90 y 90+.
 */
public final class EstadoCuentaPorPagar {

    public static final List<String> RANGOS = List.of("0-30", "31-60", "61-90", "90+");

    private EstadoCuentaPorPagar() {}

    public static String estadoSegun(BigDecimal montoPagado, BigDecimal saldo) {
        if (saldo == null || saldo.signum() <= 0) return CuentaPorPagar.PAGADA;
        if (montoPagado != null && montoPagado.signum() > 0) return CuentaPorPagar.PARCIAL;
        return CuentaPorPagar.PENDIENTE;
    }

    public static boolean vencida(BigDecimal saldo, LocalDate fechaVencimiento, LocalDate hoy) {
        return saldo != null && saldo.signum() > 0
                && fechaVencimiento != null && fechaVencimiento.isBefore(hoy);
    }

    public static int diasVencido(BigDecimal saldo, LocalDate fechaVencimiento, LocalDate hoy) {
        if (!vencida(saldo, fechaVencimiento, hoy)) return 0;
        return (int) ChronoUnit.DAYS.between(fechaVencimiento, hoy);
    }

    public static String rango(int diasVencido) {
        if (diasVencido <= 30) return RANGOS.get(0);
        if (diasVencido <= 60) return RANGOS.get(1);
        if (diasVencido <= 90) return RANGOS.get(2);
        return RANGOS.get(3);
    }

    /** Saldo de una cuota vencida con su fecha de vencimiento. */
    public record SaldoVencido(LocalDate fechaVencimiento, BigDecimal saldo) {}

    /** Agrupa los saldos vencidos en los cuatro rangos, siempre presentes y en orden. */
    public static List<RangoAntiguedad> antiguedad(List<SaldoVencido> saldos, LocalDate hoy) {
        BigDecimal[] montos = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        long[] cantidades = new long[4];
        for (SaldoVencido s : saldos) {
            if (!vencida(s.saldo(), s.fechaVencimiento(), hoy)) continue;
            int i = RANGOS.indexOf(rango(diasVencido(s.saldo(), s.fechaVencimiento(), hoy)));
            montos[i] = montos[i].add(s.saldo());
            cantidades[i]++;
        }
        List<RangoAntiguedad> rangos = new ArrayList<>();
        for (int i = 0; i < RANGOS.size(); i++) {
            rangos.add(new RangoAntiguedad(RANGOS.get(i), escala(montos[i]), cantidades[i]));
        }
        return rangos;
    }

    public static BigDecimal escala(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
