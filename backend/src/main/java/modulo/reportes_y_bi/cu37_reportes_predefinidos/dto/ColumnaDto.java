package modulo.reportes_y_bi.cu37_reportes_predefinidos.dto;

/** Columna de un reporte: el frontend la usa para el encabezado y el formato del valor. */
public record ColumnaDto(String clave, String etiqueta, String tipo, boolean totalizar) {
}
