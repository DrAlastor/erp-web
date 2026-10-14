package modulo.ventas_y_facturacion.compartido.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas de redondeo comunes a ventas y notas de crédito. Los importes se guardan con dos
 * decimales (NUMERIC(12,2)).
 */
public final class Montos {

    private Montos() {
    }

    public static BigDecimal redondear(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }

    /** Aplica una tasa en porcentaje (13.00 = 13 %) a un importe. */
    public static BigDecimal porcentaje(BigDecimal importe, BigDecimal tasa) {
        return redondear(importe.multiply(tasa).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
    }

    /** Parte proporcional de {@code monto}: monto × parte / todo. */
    public static BigDecimal proporcion(BigDecimal monto, BigDecimal parte, BigDecimal todo) {
        if (todo.signum() == 0) return BigDecimal.ZERO;
        return redondear(monto.multiply(parte).divide(todo, 6, RoundingMode.HALF_UP));
    }

    public static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
