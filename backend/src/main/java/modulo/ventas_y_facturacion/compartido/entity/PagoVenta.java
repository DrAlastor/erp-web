package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Importe cobrado en una forma de pago, mapeado a la tabla pago_venta de ERP_Asus.
 */
@Entity
@Table(name = "pago_venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoVenta {

    public static final String ESTADO_COMPLETADO = "COMPLETADO";
    public static final String ESTADO_ANULADO = "ANULADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forma_pago_id", nullable = false)
    private FormaPago formaPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Column(name = "transaccion_externa", length = 100)
    private String transaccionExterna;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @Column(nullable = false, length = 30)
    private String estado;
}
