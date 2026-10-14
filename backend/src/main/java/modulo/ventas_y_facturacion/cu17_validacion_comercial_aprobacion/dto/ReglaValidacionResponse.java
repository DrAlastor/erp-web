package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Resultado de una regla de validación comercial. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReglaValidacionResponse {
    private String codigo;
    private String nombre;
    private boolean cumple;
    /** Si es verdadera y no se cumple, el pedido no se puede aprobar. */
    private boolean bloqueante;
    private String detalle;
}
