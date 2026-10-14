package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;

import java.math.BigDecimal;

/**
 * Línea de la factura, mapeada a la tabla venta_detalles de ERP_Asus.
 * El subtotal es neto: cantidad por precio unitario menos el descuento de la línea.
 */
@Entity
@Table(name = "venta_detalles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "descuento_id")
    private Long descuentoId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}
