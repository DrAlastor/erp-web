package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SucursalItemDTO {
    private Integer id;
    private String nombre;
    private String direccion;
    private String telefono;
    private Boolean activo;
}
