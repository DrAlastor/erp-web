package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
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
