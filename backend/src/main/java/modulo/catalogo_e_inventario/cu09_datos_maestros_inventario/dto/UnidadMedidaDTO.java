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
public class UnidadMedidaDTO {
    private Integer id;
    private String codigo;
    private String nombre;
    private String abreviatura;
    private Boolean activo;
    private Long productosCount;
    private LocalDateTime fechaCreacion;
}
