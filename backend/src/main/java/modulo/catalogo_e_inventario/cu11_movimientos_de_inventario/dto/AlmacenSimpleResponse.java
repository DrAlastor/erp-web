package modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.dto;

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
