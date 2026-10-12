package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnidadMedidaDTO {
    private Integer id;
    private String codigo;
    private String nombre;
    private String abreviatura;
    private Boolean activo;
    private Long productosCount;
    private LocalDateTime fechaCreacion;
}
