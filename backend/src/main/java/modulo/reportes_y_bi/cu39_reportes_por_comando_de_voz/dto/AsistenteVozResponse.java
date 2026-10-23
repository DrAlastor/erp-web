package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto;

import java.util.List;

/**
 * Respuesta del asistente: el texto que se muestra y se lee en voz alta y, si consultó
 * datos, el último reporte generado para graficarlo.
 *
 * @param origen GEMINI (asistente IA) o LOCAL (intérprete de respaldo)
 */
public record AsistenteVozResponse(
        String respuesta,
        ReporteVozResponse reporte,
        String origen,
        List<String> advertencias) {
}
