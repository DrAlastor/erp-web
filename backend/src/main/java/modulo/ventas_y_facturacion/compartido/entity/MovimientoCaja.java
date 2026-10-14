package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ingreso o egreso de dinero dentro de un turno, mapeado a la tabla movimientos_caja.
 */
@Entity
@Table(name = "movimientos_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoCaja {

    public static final String TIPO_EGRESO = "EGRESO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turno_caja_id", nullable = false)
    private Long turnoCajaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forma_pago_id", nullable = false)
    private FormaPago formaPago;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 255)
    private String concepto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "numero_comprobante", length = 50)
    private String numeroComprobante;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
}
