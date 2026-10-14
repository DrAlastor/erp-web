package modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Devolución sobre una factura vigente. Cada ítem apunta a la línea de la factura
 * original; quien emite la nota es el usuario autenticado.
 */
public record NotaCreditoRequest(
        @NotNull(message = "La venta es obligatoria")
        Long ventaId,
        @NotBlank(message = "El motivo es obligatorio")
        @Pattern(regexp = "DEFECTUOSO|CAMBIO_PRODUCTO|ERROR_FACTURACION|DESISTIMIENTO|OTRO",
                message = "Motivo no válido")
        String motivo,
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones,
        @NotEmpty(message = "Selecciona al menos un producto a devolver")
        @Size(max = 100, message = "La nota no puede superar 100 líneas")
        List<@Valid Item> items) {

    public record Item(
            @NotNull(message = "La línea de la factura es obligatoria")
            Long ventaDetalleId,
            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.01", message = "La cantidad a devolver debe ser mayor a cero")
            @Digits(integer = 10, fraction = 2, message = "La cantidad admite hasta 2 decimales")
            BigDecimal cantidad) {
    }
}
