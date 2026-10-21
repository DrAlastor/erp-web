package modulo.reportes_y_bi.cu37_reportes_predefinidos.dto;

import java.time.LocalDate;

/**
 * Filtros con que se pide un reporte. Cada reporte usa solo los que declara; el resto se
 * ignora. Las fechas son inclusivas.
 */
public record FiltrosReporte(
        LocalDate desde,
        LocalDate hasta,
        Integer sucursalId,
        Integer categoriaId,
        Integer limite) {
}
