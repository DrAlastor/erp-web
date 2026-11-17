package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Reglas puras de la CU29. El estado guardado es PENDIENTE, PARCIAL, PAGADO o ANULADA
 * (los dos extremos ya los usa la CU19); "vencida" se deriva de la fecha y no se guarda.
 */
public final class EstadoCuentaPorCobrar {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String PARCIAL = "PARCIAL";
    public static final String PAGADO = "PAGADO";
    public static final String ANULADA = "ANULADA";
    public static final String VENCIDA = "VENCIDA";

    /** Estados por los que se puede filtrar el listado. */
    public static final List<String> FILTROS = List.of(PENDIENTE, PARCIAL, PAGADO, ANULADA, VENCIDA);

    private EstadoCuentaPorCobrar() {
    }

    public static BigDecimal escala(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }

    /** Estado tras aplicar cobros: sin saldo PAGADO, con algo cobrado PARCIAL, si no PENDIENTE. */
    public static String estadoSegun(BigDecimal cobrado, BigDecimal saldo) {
        if (escala(saldo).signum() <= 0) return PAGADO;
        if (escala(cobrado).signum() > 0) return PARCIAL;
        return PENDIENTE;
    }

    public static boolean tieneDeuda(String estado, BigDecimal saldo) {
        String e = estado == null ? "" : estado.toUpperCase(Locale.ROOT);
        return escala(saldo).signum() > 0 && !e.startsWith("ANULAD") && !PAGADO.equals(e);
    }

    public static boolean vencida(String estado, BigDecimal saldo, LocalDate vencimiento, LocalDate hoy) {
        return tieneDeuda(estado, saldo) && vencimiento != null && vencimiento.isBefore(hoy);
    }

    /** Días de atraso (0 si no está vencida). */
    public static long diasVencida(String estado, BigDecimal saldo, LocalDate vencimiento, LocalDate hoy) {
        return vencida(estado, saldo, vencimiento, hoy) ? ChronoUnit.DAYS.between(vencimiento, hoy) : 0;
    }

    /** Tramo de antigüedad de un saldo vencido: 1-30, 31-60, 61-90 o mas de 90 días. */
    public static int tramo(long diasVencida) {
        if (diasVencida <= 30) return 0;
        if (diasVencida <= 60) return 1;
        if (diasVencida <= 90) return 2;
        return 3;
    }

    /** Medio de cobro en la contabilidad: el efectivo va a Caja, todo lo demás a Bancos. */
    public static boolean vaACaja(String codigoFormaPago) {
        return "EFECTIVO".equalsIgnoreCase(codigoFormaPago);
    }
}
