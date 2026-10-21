package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Linea de detalle enviada al crear o modificar un pedido. */
public record PedidoDetalleRequest(
        @NotNull(message = "El producto de la línea es obligatorio")
        Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "La cantidad admite hasta 2 decimales")
        BigDecimal cantidad,
        @DecimalMin(value = "0", message = "El precio unitario no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio unitario admite hasta 2 decimales")
        BigDecimal precioUnitario,
        @DecimalMin(value = "0", message = "El descuento de la línea no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El descuento de la línea admite hasta 2 decimales")
        BigDecimal descuento) {}
