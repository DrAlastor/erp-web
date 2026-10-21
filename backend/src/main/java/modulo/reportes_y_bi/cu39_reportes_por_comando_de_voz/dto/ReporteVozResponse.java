package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.Visualizacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Resultado de un comando de voz: la consulta que se entendió, una descripción legible de
 * esa interpretación y los datos listos para graficar o mostrar en tabla.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteVozResponse {

    /** Frase dictada; vacía cuando el reporte se refinó a mano. */
    private String textoOriginal;

    /** Quién interpretó la frase: N8N, LOCAL (intérprete de respaldo) o MANUAL. */
    private String origen;

    private ConsultaReporteVoz consulta;
    private String titulo;
    private String interpretacion;
    private Visualizacion visualizacion;

    private List<ColumnaReporte> columnas;
    private List<Map<String, Object>> filas;

    /** Suma de cada medida acumulable sobre todas las filas devueltas. */
    private Map<String, BigDecimal> totales;

    private List<String> advertencias;
    private LocalDateTime generadoEn;
}
