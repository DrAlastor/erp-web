package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.service;

import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto.BalanceGeneralResponse;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto.EstadoResultadosResponse;

import java.time.LocalDate;
import java.util.List;

public interface InformacionFinancieraService {

    List<Integer> listarGestionesDisponibles();

    BalanceGeneralResponse generarBalanceGeneral(
            Integer gestion,
            Integer mesCorte,
            LocalDate fechaCorte,
            Integer nivelDetalle
    );

    EstadoResultadosResponse generarEstadoResultados(
            Integer gestion,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer nivelDetalle
    );

    byte[] exportarBalanceCsv(
            Integer gestion,
            Integer mesCorte,
            LocalDate fechaCorte,
            Integer nivelDetalle
    );

    byte[] exportarEstadoResultadosCsv(
            Integer gestion,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer nivelDetalle
    );
}
