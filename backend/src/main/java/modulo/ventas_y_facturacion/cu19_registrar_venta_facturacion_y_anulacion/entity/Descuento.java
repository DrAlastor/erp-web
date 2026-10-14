package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Descuento o promoción aplicable a una línea de venta, mapeado a la tabla descuentos.
 * {@code tipo} es PORCENTAJE o MONTO_FIJO.
 */
@Entity(name = "DescuentoVenta")
@Table(name = "descuentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Descuento {

    public static final String PORCENTAJE = "PORCENTAJE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(nullable = false)
    private Boolean activo;

    /** Activo y dentro de su vigencia en la fecha indicada. */
    public boolean vigente(LocalDate fecha) {
        return Boolean.TRUE.equals(activo)
                && (fechaInicio == null || !fecha.isBefore(fechaInicio))
                && (fechaFin == null || !fecha.isAfter(fechaFin));
    }
}
