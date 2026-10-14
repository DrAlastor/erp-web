package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Forma de pago (efectivo, tarjeta, QR, transferencia), mapeada a la tabla formas_pago.
 */
@Entity
@Table(name = "formas_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormaPago {

    public static final String EFECTIVO = "EFECTIVO";
    /** Pago con tarjeta en línea a través de Stripe Checkout. */
    public static final String PASARELA_ONLINE = "PASARELA_ONLINE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public boolean esEfectivo() {
        return EFECTIVO.equalsIgnoreCase(codigo);
    }

    public boolean esPasarelaOnline() {
        return PASARELA_ONLINE.equalsIgnoreCase(codigo);
    }
}
