package modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.entity;

import jakarta.persistence.*;
import lombok.*;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;

import java.math.BigDecimal;

/**
 * Artículo devuelto, enlazado a la línea de la factura original.
 * Mapeado a la tabla notas_credito_detalles.
 */
@Entity
@Table(name = "notas_credito_detalles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaCreditoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nota_credito_id", nullable = false)
    private NotaCredito notaCredito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "venta_detalle_id")
    private Long ventaDetalleId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}
