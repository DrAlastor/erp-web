package modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo de rechazo o de anulación de una orden de compra. */
public record MotivoRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 255, message = "El motivo no puede exceder 255 caracteres") String motivo) {
}
