package modulo.inventario_y_existencia.movimiento_inventario.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlmacenSimpleResponse {
    private Integer id;
    private String nombre;
    private String direccion;
    private Boolean esPrincipal;
}
