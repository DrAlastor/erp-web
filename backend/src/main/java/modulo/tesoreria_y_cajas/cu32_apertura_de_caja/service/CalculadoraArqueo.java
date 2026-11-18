package modulo.tesoreria_y_cajas.cu32_apertura_de_caja.service;

import java.math.BigDecimal;

/**
 * Reglas puras del arqueo de un turno, sin base de datos.
 *
 * <p>Solo el efectivo se cuenta físicamente en la caja: el esperado es el fondo inicial más
 * las ventas y cobros en efectivo, más los ingresos y menos los egresos de efectivo. Las
 * ventas con tarjeta, QR o transferencia se informan pero no forman parte del esperado.
 */
public final class CalculadoraArqueo {

    private CalculadoraArqueo() {
    }

    public static BigDecimal esperado(BigDecimal apertura, BigDecimal ventasEfectivo, BigDecimal cobrosCxcEfectivo,
                                      BigDecimal ingresos, BigDecimal egresos) {
        return n(apertura).add(n(ventasEfectivo)).add(n(cobrosCxcEfectivo)).add(n(ingresos)).subtract(n(egresos));
    }

    /** Positivo = sobrante, negativo = faltante. */
    public static BigDecimal diferencia(BigDecimal contado, BigDecimal esperado) {
        return n(contado).subtract(n(esperado));
    }

    private static BigDecimal n(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
