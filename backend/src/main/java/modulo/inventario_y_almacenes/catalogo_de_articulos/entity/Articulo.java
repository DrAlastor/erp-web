package modulo.inventario_y_almacenes.catalogo_de_articulos.entity;

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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import modulo.inventario_y_almacenes.compartido.entity.Categoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Artículo del catálogo maestro (HU-05 / CU-08).
 *
 * <p>La categoría es la entidad compartida del módulo de inventario: HU-05 y HU-06 clasifican
 * contra la misma tabla {@code categorias}. Las validaciones del alta viven en el DTO, no
 * acá, para que la entidad no dependa del contrato HTTP.
 */
@Entity
@Table(name = "articulos")
@Getter
@Setter
@NoArgsConstructor
public class Articulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String sku;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal precio;

    private Integer stock;

    @Column(name = "imagen_url", length = 512)
    private String imagenUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

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
    }
}
