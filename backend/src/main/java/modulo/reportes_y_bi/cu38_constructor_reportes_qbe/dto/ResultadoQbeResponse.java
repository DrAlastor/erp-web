package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto;

import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.TipoCampo;

import java.util.List;
import java.util.Map;

/** Página de la vista previa del reporte (HU38.3). */
public record ResultadoQbeResponse(
        List<ColumnaResultado> columnas,
        List<Map<String, Object>> filas,
        long total,
        int pagina,
        int tamanio,
        int totalPaginas,
        String descripcionFiltros) {

    public record ColumnaResultado(String clave, String etiqueta, TipoCampo tipo) {}
}
