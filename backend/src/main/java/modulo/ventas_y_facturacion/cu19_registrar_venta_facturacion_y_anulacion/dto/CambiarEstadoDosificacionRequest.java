package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoDosificacionRequest(
        @NotNull(message = "Indica si la dosificación queda activa")
        Boolean activo) {
}
