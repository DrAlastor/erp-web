package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculo de importes de un pedido, sin dependencias de base de datos.
 *
 * <p>Reglas: subtotal de linea = cantidad x precio unitario - descuento de linea (no puede
 * ser negativo); subtotal del pedido = suma de las lineas; total = subtotal - descuento
 * global (que no puede superar al subtotal). Todo se redondea HALF_UP a 2 decimales.
 */
public final class CalculadoraPedido {

    private CalculadoraPedido() {}

    /** Linea con precio ya resuelto (el del pedido o el precio de venta del producto). */
    public record LineaEntrada(
            Long productoId,
            String productoNombre,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento) {}

    public record LineaCalculada(
            Long productoId,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            BigDecimal subtotal) {}

    public record Resultado(
            List<LineaCalculada> lineas,
            BigDecimal subtotal,
            BigDecimal descuento,
            BigDecimal total) {}

    public static Resultado calcular(List<LineaEntrada> lineas, BigDecimal descuentoGlobal) {
        List<LineaCalculada> calculadas = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineaEntrada linea : lineas) {
            BigDecimal cantidad = escala(linea.cantidad());
            BigDecimal precio = escala(linea.precioUnitario());
            BigDecimal descuento = escala(linea.descuento());
            BigDecimal subtotalLinea = escala(cantidad.multiply(precio).subtract(descuento));
            if (subtotalLinea.signum() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "El descuento de la línea de " + linea.productoNombre()
                                + " supera su importe");
            }
            calculadas.add(new LineaCalculada(
                    linea.productoId(), cantidad, precio, descuento, subtotalLinea));
            subtotal = subtotal.add(subtotalLinea);
        }
        subtotal = escala(subtotal);
        BigDecimal descuento = escala(descuentoGlobal);
        if (descuento.signum() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "El descuento global no puede ser negativo");
        }
        if (descuento.compareTo(subtotal) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "El descuento global no puede superar el subtotal del pedido");
        }
        return new Resultado(calculadas, subtotal, descuento, escala(subtotal.subtract(descuento)));
    }

    static BigDecimal escala(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
