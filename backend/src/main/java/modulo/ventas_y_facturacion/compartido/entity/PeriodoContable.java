package modulo.ventas_y_facturacion.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Periodo contable mensual, mapeado a la tabla periodos_contables.
 */
@Entity
@Table(name = "periodos_contables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodoContable {

    public static final String ESTADO_ABIERTO = "ABIERTO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer gestion;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    public boolean estaAbierto() {
        return ESTADO_ABIERTO.equalsIgnoreCase(estado);
    }
}
