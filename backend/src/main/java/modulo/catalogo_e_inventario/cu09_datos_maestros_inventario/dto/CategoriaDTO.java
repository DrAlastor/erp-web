package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaDTO {
    private Integer id;
    private String nombre;
    private String descripcion;
    private Integer padreId;
    private String padreNombre;
    private Boolean activo;
    private Long articulosCount;
    private Long subcategoriasCount;
    private LocalDateTime fechaCreacion;
}
