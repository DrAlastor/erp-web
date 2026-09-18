package modulo.inventario_y_existencia.movimiento_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Column(name = "impuesto_id")
    private Integer impuestoId;

    @Column(name = "codigo_sku", unique = true, length = 50)
    private String codigoSku;

    @Column(name = "codigo_barra", unique = true, length = 50)
    private String codigoBarra;

    @Column(name = "unidad_medida", length = 20)
    @Builder.Default
    private String unidadMedida = "UNIDAD";

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal precioVenta = BigDecimal.ZERO;

    @Column(name = "costo_promedio", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal costoPromedio = BigDecimal.ZERO;

    @Column(name = "stock_minimo")
    @Builder.Default
    private Integer stockMinimo = 5;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "creado_por", length = 50)
    private String creadoPor;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "modificado_por", length = 50)
    private String modificadoPor;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        fechaModificacion = LocalDateTime.now();
    }
}
