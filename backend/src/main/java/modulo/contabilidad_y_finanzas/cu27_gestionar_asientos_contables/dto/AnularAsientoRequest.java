package modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo obligatorio para anular un asiento manual. */
public record AnularAsientoRequest(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 5, max = 300, message = "El motivo debe tener entre 5 y 300 caracteres")
        String motivo) {}
