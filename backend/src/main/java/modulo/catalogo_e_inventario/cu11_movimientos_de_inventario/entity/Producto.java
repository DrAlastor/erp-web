package modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity;

import modulo.catalogo_e_inventario.compartido.entity.Categoria;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Producto mapeada a la tabla productos de ERP_Asus para movimientos de inventario.
 */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "codigo", unique = true, nullable = false, length = 50)
    private String codigoSku;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal precioVenta = BigDecimal.ZERO;

    @Column(name = "costo_compra", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costoPromedio = BigDecimal.ZERO;

    @Column(name = "stock_minimo", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal stockMinimo = BigDecimal.valueOf(5);

    @Column(name = "stock_maximo", precision = 12, scale = 2)
    private BigDecimal stockMaximo;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "unidad_medida_id")
    @Builder.Default
    private Integer unidadMedidaId = 1;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Transient
    public String getCodigoBarra() {
        return null;
    }

    @Transient
    public String getUnidadMedida() {
        return "UND";
    }

    public int getStockMinimoInt() {
        return stockMinimo != null ? stockMinimo.intValue() : 0;
    }
}
