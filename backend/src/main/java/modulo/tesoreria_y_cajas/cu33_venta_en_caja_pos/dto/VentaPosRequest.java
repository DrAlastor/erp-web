package modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Venta rápida en caja. No trae sucursal, turno, precios ni tipo de venta: la sucursal y el
 * turno salen del turno abierto del cajero autenticado, los precios del catálogo y la venta
 * siempre es al contado con un único pago.
 */
public record VentaPosRequest(
        /** Opcional: sin cliente se emite a "Cliente Pasante / Sin Nombre". */
        Long clienteId,
        @Pattern(regexp = "FACTURA|NOTA_VENTA", message = "El comprobante debe ser FACTURA o NOTA_VENTA")
        String tipoComprobante,
        @NotNull(message = "La forma de pago es obligatoria")
        Long formaPagoId,
        /** Solo para efectivo: lo que entrega el cliente, para calcular el cambio. */
        @DecimalMin(value = "0.00", message = "El monto recibido no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El monto recibido admite hasta 2 decimales")
        BigDecimal montoRecibido,
        @NotEmpty(message = "La venta debe tener al menos un producto")
        @Size(max = 100, message = "La venta no puede superar 100 líneas")
        List<@Valid Linea> detalles) {

    public record Linea(
            @NotNull(message = "El producto es obligatorio")
            Long productoId,
            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero")
            @Digits(integer = 10, fraction = 2, message = "La cantidad admite hasta 2 decimales")
            BigDecimal cantidad) {
    }

    public String comprobante() {
        return tipoComprobante == null || tipoComprobante.isBlank() ? "NOTA_VENTA" : tipoComprobante;
    }
}
