package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaNodoDTO {
    private Integer id;
    private String nombre;
    private String descripcion;
    private Integer padreId;
    private Boolean activo;
    private Long articulosCount;
    private LocalDateTime fechaCreacion;

    @Builder.Default
    private List<CategoriaNodoDTO> subcategorias = new ArrayList<>();
}
