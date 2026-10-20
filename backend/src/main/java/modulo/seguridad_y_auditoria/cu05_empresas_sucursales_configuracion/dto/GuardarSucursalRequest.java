package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record GuardarSucursalRequest(
        @NotNull(message = "La ciudad es obligatoria")
        Integer ciudadId,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder los 120 caracteres")
        String nombre,

        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 255, message = "La dirección no puede exceder los 255 caracteres")
        String direccion,

        @Size(max = 30, message = "El teléfono no puede exceder los 30 caracteres")
        String telefono,

        LocalTime horaApertura,

        LocalTime horaCierre) {
}
