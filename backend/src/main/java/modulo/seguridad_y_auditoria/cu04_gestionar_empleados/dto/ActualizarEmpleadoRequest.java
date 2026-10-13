package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarEmpleadoRequest {

    @NotBlank(message = "El CI o documento de identidad es obligatorio")
    @Size(max = 30, message = "El CI no puede exceder los 30 caracteres")
    private String ci;

    @NotNull(message = "La sucursal asignada es obligatoria")
    private Integer sucursalId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede exceder los 100 caracteres")
    private String apellido;

    @NotBlank(message = "El cargo es obligatorio")
    @Size(max = 100, message = "El cargo no puede exceder los 100 caracteres")
    private String cargo;

    @Size(max = 30, message = "El teléfono no puede exceder los 30 caracteres")
    private String telefono;

    @Size(max = 100, message = "El correo electrónico no puede exceder los 100 caracteres")
    private String email;

    @Size(max = 255, message = "La dirección no puede exceder los 255 caracteres")
    private String direccion;

    @NotNull(message = "La fecha de ingreso es obligatoria")
    private LocalDate fechaIngreso;

    private Boolean activo;
}
