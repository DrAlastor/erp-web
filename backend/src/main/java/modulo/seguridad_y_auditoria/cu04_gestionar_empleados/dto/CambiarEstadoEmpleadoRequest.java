package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambiarEstadoEmpleadoRequest {

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;

    private String motivo;
}
