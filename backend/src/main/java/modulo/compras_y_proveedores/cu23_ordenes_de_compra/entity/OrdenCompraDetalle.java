package modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Un renglón de la orden de compra (tabla ORDEN_COMPRA_DETALLES). */
@Entity
@Table(name = "orden_compra_detalles")
@Getter
@Setter
@NoArgsConstructor
public class OrdenCompraDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_compra_id", nullable = false)
    private OrdenCompra orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** subtotal = cantidad × costo unitario, a dos decimales. */
    public void calcularSubtotal() {
        subtotal = cantidad.multiply(costoUnitario).setScale(2, RoundingMode.HALF_UP);
    }
}
