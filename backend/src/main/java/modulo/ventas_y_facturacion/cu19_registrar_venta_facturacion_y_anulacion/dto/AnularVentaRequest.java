package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularVentaRequest(
        @NotBlank(message = "El motivo de anulación es obligatorio")
        @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
        String motivo) {
}
