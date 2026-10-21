package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Línea del pedido con la existencia del producto en la sucursal. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaPedidoResponse {
    private Integer productoId;
    private String productoNombre;
    private String productoSku;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuento;
    private BigDecimal subtotal;
    private BigDecimal stockFisico;
    private BigDecimal reservado;
    private BigDecimal disponible;
    private boolean cubierta;
}
