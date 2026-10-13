package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpleadoDTO {

    private Long id;
    private Integer sucursalId;
    private String sucursalNombre;
    private String ci;
    private String nombre;
    private String apellido;
    private String nombreCompleto;
    private String cargo;
    private String telefono;
    private String email;
    private String direccion;
    private LocalDate fechaIngreso;
    private Boolean activo;

    // Estado de Vinculación con Cuenta de Usuario ERP
    private Boolean tieneUsuario;
    private Long usuarioId;
    private String usuarioEmail;
    private Boolean usuarioActivo;
}
