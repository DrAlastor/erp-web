package modulo.reportes_y_bi.cu37_reportes_predefinidos.dto;

import java.util.List;

/** Un reporte del catálogo, tal como lo lista la pantalla para elegirlo. */
public record ReporteDefinicionDto(
        String codigo,
        String titulo,
        String descripcion,
        String categoria,
        String categoriaEtiqueta,
        List<String> filtros,
        List<ColumnaDto> columnas) {
}
