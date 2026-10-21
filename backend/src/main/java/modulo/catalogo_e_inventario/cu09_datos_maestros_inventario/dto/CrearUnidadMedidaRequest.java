package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearUnidadMedidaRequest {

    @NotBlank(message = "El código de la unidad es obligatorio")
    @Size(max = 20, message = "El código no puede superar los 20 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre de la unidad es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotBlank(message = "La abreviatura es obligatoria")
    @Size(max = 15, message = "La abreviatura no puede superar los 15 caracteres")
    private String abreviatura;
}
