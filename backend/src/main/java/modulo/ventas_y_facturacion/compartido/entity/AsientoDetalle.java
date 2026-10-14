package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Partida del asiento (debe o haber contra una cuenta), mapeada a la tabla asiento_detalles.
 */
@Entity
@Table(name = "asiento_detalles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsientoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asiento_id", nullable = false)
    private AsientoContable asiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_contable_id", nullable = false)
    private CuentaContable cuentaContable;

    @Column(length = 255)
    private String glosa;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal debe;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal haber;
}
