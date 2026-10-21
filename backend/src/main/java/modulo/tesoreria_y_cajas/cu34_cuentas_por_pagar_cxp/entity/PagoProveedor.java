package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.entity;

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

/** Pago (total o parcial) aplicado a una cuota por pagar (tabla {@code pagos_proveedor}). */
@Entity(name = "PagoProveedor")
@Table(name = "pagos_proveedor")
@Getter
@Setter
@NoArgsConstructor
public class PagoProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_por_pagar_id", nullable = false)
    private Long cuentaPorPagarId;

    @Column(name = "forma_pago_id", nullable = false)
    private Long formaPagoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "numero_recibo", length = 50)
    private String numeroRecibo;

    @Column(name = "transaccion_externa", length = 100)
    private String transaccionExterna;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
