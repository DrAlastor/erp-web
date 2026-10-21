package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Justificación con la que se aprueba o se rechaza un pedido. */
@Getter
@Setter
@NoArgsConstructor
public class DecisionRequest {

    @Size(max = 500, message = "La justificación no puede superar los 500 caracteres.")
    private String observacion;
}
