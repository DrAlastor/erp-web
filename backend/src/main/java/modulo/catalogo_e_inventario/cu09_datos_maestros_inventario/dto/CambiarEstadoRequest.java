package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambiarEstadoRequest {
    @NotNull(message = "El nuevo estado activo es obligatorio")
    private Boolean activo;
}
