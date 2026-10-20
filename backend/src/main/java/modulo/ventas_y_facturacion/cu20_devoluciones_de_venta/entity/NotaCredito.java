package modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.entity;

import jakarta.persistence.*;
import lombok.*;
import modulo.ventas_y_facturacion.compartido.entity.Venta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nota de crédito por devolución de mercadería, mapeada a la tabla notas_credito.
 */
@Entity
@Table(name = "notas_credito")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotaCredito {

    public static final String ESTADO_EMITIDA = "EMITIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "sucursal_id", nullable = false)
    private Integer sucursalId;

    @Column(name = "emitido_por", nullable = false)
    private Long emitidoPor;

    @Column(name = "numero_nota", nullable = false, unique = true, length = 50)
    private String numeroNota;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 50)
    private String motivo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "notaCredito", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private List<NotaCreditoDetalle> detalles = new ArrayList<>();

    public void agregarDetalle(NotaCreditoDetalle detalle) {
        detalle.setNotaCredito(this);
        detalles.add(detalle);
    }

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
