package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cobro (total o parcial) aplicado a una cuota por cobrar (tabla {@code cobros}). */
@Entity(name = "CobroCliente")
@Table(name = "cobros")
@Getter
@Setter
@NoArgsConstructor
public class Cobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_por_cobrar_id", nullable = false)
    private Long cuentaPorCobrarId;

    @Column(name = "forma_pago_id", nullable = false)
    private Long formaPagoId;

    @Column(name = "turno_caja_id")
    private Long turnoCajaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "numero_recibo", nullable = false, length = 50)
    private String numeroRecibo;

    @Column(name = "transaccion_externa", length = 100)
    private String transaccionExterna;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_cobro", nullable = false)
    private LocalDate fechaCobro;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
