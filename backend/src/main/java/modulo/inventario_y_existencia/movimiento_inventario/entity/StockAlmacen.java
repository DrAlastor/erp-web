package modulo.inventario_y_existencia.movimiento_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "stock_almacen",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_stock_producto_almacen", columnNames = {"producto_id", "almacen_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlmacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "cantidad_actual", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cantidadActual = BigDecimal.ZERO;
}
