package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto;

import java.math.BigDecimal;
import java.util.List;

public record LibroDiarioResponse(
        String periodoSeleccionado,
        BigDecimal totalDebeGeneral,
        BigDecimal totalHaberGeneral,
        Boolean estaCuadrado,
        int totalAsientos,
        List<AsientoDiarioDTO> asientos
) {}
