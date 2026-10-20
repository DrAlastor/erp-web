package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprobante del Libro Diario, mapeado a la tabla asientos_contables.
 */
@Entity
@Table(name = "asientos_contables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsientoContable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "periodo_contable_id", nullable = false)
    private Long periodoContableId;

    @Column(name = "creado_por", nullable = false)
    private Long creadoPor;

    @Column(name = "numero_asiento", nullable = false, unique = true, length = 50)
    private String numeroAsiento;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "tipo_asiento", nullable = false, length = 30)
    private String tipoAsiento;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String glosa;

    @Column(name = "total_debe", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDebe;

    @Column(name = "total_haber", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalHaber;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AsientoDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(AsientoDetalle detalle) {
        detalle.setAsiento(this);
        detalles.add(detalle);
    }

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
