package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** Pedido con el resultado completo de su validación comercial. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidacionPedidoResponse {
    private PedidoResumenResponse pedido;
    private String observaciones;

    private String condicionPago;
    private BigDecimal limiteCredito;
    private BigDecimal saldoPendiente;
    private BigDecimal saldoVencido;
    private BigDecimal creditoDisponible;

    private List<LineaPedidoResponse> lineas;
    private List<ReglaValidacionResponse> reglas;

    /** El pedido está pendiente y ninguna regla bloqueante falla. */
    private boolean puedeAprobar;
    /** Alguna regla no bloqueante falla: aprobar exige dejar una justificación. */
    private boolean requiereJustificacion;
}
