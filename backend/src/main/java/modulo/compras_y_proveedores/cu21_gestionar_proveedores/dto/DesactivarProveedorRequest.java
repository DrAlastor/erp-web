package modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DesactivarProveedorRequest(
        @NotBlank(message = "El motivo de la desactivación es obligatorio")
        @Size(max = 255, message = "El motivo no puede exceder 255 caracteres") String motivo) {
}
