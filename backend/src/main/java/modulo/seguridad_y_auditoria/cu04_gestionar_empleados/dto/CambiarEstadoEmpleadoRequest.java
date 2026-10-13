package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CambiarEstadoEmpleadoRequest {

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;

    private String motivo;
}
