package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pedido tal como aparece en la bandeja de validación comercial. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoResumenResponse {
    private Integer id;
    private String numeroPedido;
    private LocalDate fecha;
    private Integer clienteId;
    private String clienteNombre;
    private Integer vendedorId;
    private Integer sucursalId;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal total;
    private String estado;
}
