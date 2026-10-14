package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Dosificación del SIN, mapeada a la tabla secuencia_facturacion.
 * {@code numeroActual} es el último número de factura ya emitido.
 */
@Entity
@Table(name = "secuencia_facturacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuenciaFacturacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_autorizacion", nullable = false, length = 60)
    private String numeroAutorizacion;

    @Column(name = "leyenda_sin", nullable = false, columnDefinition = "TEXT")
    private String leyendaSin;

    @Column(name = "numero_inicial", nullable = false)
    private Integer numeroInicial;

    @Column(name = "numero_final", nullable = false)
    private Integer numeroFinal;

    @Column(name = "numero_actual", nullable = false)
    private Integer numeroActual;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Column(nullable = false)
    private Boolean activo;
}
