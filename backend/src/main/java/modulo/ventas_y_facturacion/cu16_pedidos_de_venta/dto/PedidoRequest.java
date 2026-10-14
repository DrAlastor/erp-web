package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cuerpo para crear o modificar un pedido de venta.
 *
 * <p>El vendedor, el numero, el estado y los importes no se reciben: el vendedor es el
 * usuario autenticado y los importes se recalculan siempre en el servidor.
 */
public record PedidoRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,
        @NotNull(message = "La sucursal es obligatoria")
        Long sucursalId,
        LocalDate fecha,
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones,
        @DecimalMin(value = "0", message = "El descuento global no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El descuento global admite hasta 2 decimales")
        BigDecimal descuento,
        @NotEmpty(message = "El pedido debe tener al menos una línea de detalle")
        List<@Valid @NotNull(message = "La línea de detalle no puede ser nula") PedidoDetalleRequest> detalles) {}
