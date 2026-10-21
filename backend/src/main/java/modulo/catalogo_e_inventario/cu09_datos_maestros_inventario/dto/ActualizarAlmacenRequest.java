package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarAlmacenRequest {

    @NotBlank(message = "El nombre de la sucursal/almacén es obligatorio")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    private String nombre;

    private String codigo;

    private Integer ciudadId;

    private Integer sucursalId;

    @Size(max = 255, message = "La dirección no puede exceder 255 caracteres")
    private String direccion;

    private String telefono;

    private String horaApertura;

    private String horaCierre;

    private BigDecimal capacidadEstimada;

    private Boolean esPrincipal;
}
