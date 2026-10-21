package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Plantilla de reporte QBE; mapea la tabla {@code REPORTES_QBE} del esquema del tenant.
 *
 * <p>{@code camposSeleccionados} guarda las claves de las columnas separadas por coma (legible
 * desde la base) y {@code filtrosAplicados} el resto de la definición en JSON: descripción,
 * columnas con su agregación, filtros, operador lógico y orden.
 */
@Entity
@Table(name = "reportes_qbe")
@Getter
@Setter
@NoArgsConstructor
public class ReporteQbe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(name = "tabla_base", nullable = false, length = 50)
    private String tablaBase;

    @Column(name = "campos_seleccionados", nullable = false, columnDefinition = "TEXT")
    private String camposSeleccionados;

    @Column(name = "filtros_aplicados", nullable = false, columnDefinition = "TEXT")
    private String filtrosAplicados;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
