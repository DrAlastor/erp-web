package modulo.catalogo_e_inventario.cu10_catalogo_de_articulos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import modulo.catalogo_e_inventario.compartido.entity.Categoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapeo de productos/artículos del catálogo de ERP_Asus.
 */
@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Articulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", unique = true, nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "costo_compra", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoCompra = BigDecimal.ZERO;

    @Column(name = "stock_minimo", precision = 12, scale = 2)
    private BigDecimal stockMinimo = BigDecimal.ONE;

    @Column(name = "stock_maximo", precision = 12, scale = 2)
    private BigDecimal stockMaximo = BigDecimal.valueOf(100);

    @Transient
    private Integer stock = 15;

    @Column(name = "imagen_url", columnDefinition = "TEXT")
    private String imagenUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "unidad_medida_id", nullable = false)
    private Integer unidadMedidaId = 1;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (activo == null) {
            activo = true;
        }
        if (costoCompra == null) {
            costoCompra = BigDecimal.ZERO;
        }
        if (unidadMedidaId == null) {
            unidadMedidaId = 1;
        }
        if (stockMinimo == null) {
            stockMinimo = BigDecimal.ONE;
        }
    }
}
