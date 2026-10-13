package modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Existencias por almacén/sucursal mapeada a la tabla inventario_sucursal de ERP_Asus.
 */
@Entity
@Table(
    name = "inventario_sucursal",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_inv_sucursal_producto", columnNames = {"sucursal_id", "producto_id"})
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
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Almacen almacen;

    @Column(name = "stock_disponible", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cantidadActual = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal stockMinimo = BigDecimal.valueOf(2);

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;

    @PrePersist
    @PreUpdate
    public void preActualizar() {
        if (ultimaActualizacion == null) {
            ultimaActualizacion = LocalDateTime.now();
        }
    }
}
