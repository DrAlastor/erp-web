package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto;

import java.time.LocalDate;
import java.util.List;

public record BalanceGeneralResponse(
        Integer gestion,
        Integer mesCorte,
        LocalDate fechaCorte,
        String periodoNombre,
        Integer nivelDetalle,
        List<NodoFinancieroDTO> activos,
        List<NodoFinancieroDTO> pasivos,
        List<NodoFinancieroDTO> patrimonio,
        CuadreBalanceDTO cuadre
) {}
