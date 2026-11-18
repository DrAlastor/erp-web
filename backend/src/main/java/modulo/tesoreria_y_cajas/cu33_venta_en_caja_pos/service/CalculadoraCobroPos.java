package modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.service;

import modulo.ventas_y_facturacion.compartido.service.Montos;

import java.math.BigDecimal;

/**
 * Reglas puras del cobro en caja, separadas para probarlas sin base de datos.
 */
public final class CalculadoraCobroPos {

    private CalculadoraCobroPos() {
    }

    /** Importe de una línea con el precio de catálogo, redondeado igual que la factura. */
    public static BigDecimal importeLinea(BigDecimal cantidad, BigDecimal precio) {
        return Montos.redondear(cantidad.multiply(Montos.redondear(precio)));
    }

    /**
     * Cambio a entregar en efectivo. Si no se indica lo recibido se asume el importe exacto.
     *
     * @throws IllegalArgumentException si lo recibido no cubre el total
     */
    public static BigDecimal cambio(BigDecimal total, BigDecimal recibido) {
        BigDecimal entregado = recibido == null ? total : Montos.redondear(recibido);
        if (entregado.compareTo(total) < 0) {
            throw new IllegalArgumentException("El efectivo recibido (Bs " + entregado
                    + ") no cubre el total de la venta (Bs " + total + ").");
        }
        return entregado.subtract(total);
    }
}
