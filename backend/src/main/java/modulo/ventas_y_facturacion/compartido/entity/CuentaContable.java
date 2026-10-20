package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Cuenta del plan de cuentas, mapeada a la tabla cuentas_contables.
 */
@Entity
@Table(name = "cuentas_contables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaContable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    private Integer nivel;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String naturaleza;

    @Column(name = "permite_movimiento", nullable = false)
    private Boolean permiteMovimiento;

    @Column(name = "padre_id")
    private Long padreId;

    @Column(nullable = false)
    private Boolean activo;
}
