package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Turno de caja POS, mapeado a la tabla turnos_caja. Las ventas solo lo leen para
 * validar que el turno siga abierto; la apertura y movimientos son de la HU-34
 * (cu32_cuentas_bancarias_y_cajas) y el arqueo/cierre de la HU-35 (cu35_arqueos_y_conciliaciones).
 */
@Entity
@Table(name = "turnos_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoCaja {

    public static final String ESTADO_ABIERTO = "ABIERTO";
    public static final String ESTADO_CERRADO = "CERRADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sucursal_id", nullable = false)
    private Integer sucursalId;

    @Column(name = "cajero_id", nullable = false)
    private Long cajeroId;

    @Column(name = "monto_apertura", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoApertura;

    /** Efectivo contado en el arqueo de cierre (HU-35). */
    @Column(name = "monto_cierre", precision = 12, scale = 2)
    private BigDecimal montoCierre;

    /** Contado menos esperado al cierre: positivo sobrante, negativo faltante (HU-35). */
    @Column(precision = 12, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public boolean estaAbierto() {
        return ESTADO_ABIERTO.equalsIgnoreCase(estado);
    }

    public String getNombre() {
        return (observaciones != null && !observaciones.isBlank()) ? observaciones : "Turno #" + id;
    }
}
