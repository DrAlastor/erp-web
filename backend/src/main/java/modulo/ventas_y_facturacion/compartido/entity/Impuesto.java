package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Impuesto aplicable a las ventas (IVA 13 %, IT 3 %), mapeado a la tabla impuestos.
 * {@code porcentaje} se guarda como 13.00, no como 0.13.
 */
@Entity
@Table(name = "impuestos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Impuesto {

    public static final String IVA = "IVA";
    public static final String IT = "IT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(nullable = false)
    private Boolean activo;
}
