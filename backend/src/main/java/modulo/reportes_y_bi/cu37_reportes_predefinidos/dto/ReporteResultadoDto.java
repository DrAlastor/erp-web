package modulo.reportes_y_bi.cu37_reportes_predefinidos.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Un reporte generado.
 *
 * @param filtros  filtros aplicados, ya legibles ("Sucursal" → "Sucursal Central")
 * @param filas    cada fila es un mapa {@code clave de columna → valor}
 * @param totales  suma de las columnas totalizables; las demás no aparecen
 */
public record ReporteResultadoDto(
        String codigo,
        String titulo,
        String categoria,
        LocalDateTime generadoEn,
        String generadoPor,
        Map<String, String> filtros,
        List<ColumnaDto> columnas,
        List<Map<String, Object>> filas,
        Map<String, Object> totales) {
}
