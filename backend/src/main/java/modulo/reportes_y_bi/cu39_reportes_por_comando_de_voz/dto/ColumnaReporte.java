package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto;

import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.TipoDato;

/** Columna del resultado: clave en cada fila, título para mostrar y tipo para formatear. */
public record ColumnaReporte(String clave, String etiqueta, TipoDato tipo) {
}
