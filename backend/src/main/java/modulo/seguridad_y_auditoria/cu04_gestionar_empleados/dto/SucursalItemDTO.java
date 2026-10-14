package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
