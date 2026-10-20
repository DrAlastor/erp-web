package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularCotizacionRequest(
        @NotBlank(message = "El motivo de anulación es obligatorio")
        @Size(max = 255, message = "El motivo no puede exceder 255 caracteres") String motivo) {
}
