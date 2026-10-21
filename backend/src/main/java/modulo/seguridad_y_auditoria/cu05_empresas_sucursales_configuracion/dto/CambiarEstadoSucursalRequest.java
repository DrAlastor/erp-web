package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoSucursalRequest(
        @NotNull(message = "El estado es obligatorio")
        Boolean activo) {
}
