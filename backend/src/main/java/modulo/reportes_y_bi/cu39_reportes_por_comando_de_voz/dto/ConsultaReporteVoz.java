package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.AgrupacionVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.MetricaVoz;

import java.time.LocalDate;

/**
 * Consulta estructurada en la que termina un comando de voz. Es lo que devuelve N8N (o el
 * intérprete local) y lo que el usuario reenvía al refinar el reporte desde la pantalla.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ConsultaReporteVoz {

    @NotNull(message = "Indica qué reporte deseas consultar.")
    private MetricaVoz metrica;

    private AgrupacionVoz agrupacion;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private Long sucursalId;
    private Long productoId;

    /** Stock bajo el mínimo o cuentas por cobrar vencidas, según la métrica. */
    private Boolean soloCriticos;

    @Min(value = 1, message = "El límite debe ser al menos 1.")
    @Max(value = 100, message = "El límite no puede superar 100 filas.")
    private Integer limite;
}
