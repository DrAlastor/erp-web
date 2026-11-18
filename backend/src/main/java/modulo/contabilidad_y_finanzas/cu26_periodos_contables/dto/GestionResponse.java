package modulo.contabilidad_y_finanzas.cu26_periodos_contables.dto;

import java.util.List;

/** Los periodos de una gestión con su resumen. {@code creados} solo se informa al generarla. */
public record GestionResponse(
        Integer gestion,
        int abiertos,
        int cerrados,
        int creados,
        List<PeriodoResponse> periodos) {
}
