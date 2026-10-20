package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularDespachoRequest(
        @NotBlank(message = "El motivo de anulación es obligatorio")
        @Size(max = 255, message = "El motivo no puede exceder 255 caracteres") String motivo) {
}
