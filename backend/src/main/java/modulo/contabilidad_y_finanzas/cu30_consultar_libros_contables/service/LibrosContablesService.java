package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.service;

import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface LibrosContablesService {

    List<PeriodoContableSimpleDTO> listarPeriodos();

    List<CuentaContableSimpleDTO> listarCuentasMovimiento();

    List<String> listarTiposAsiento();

    LibroDiarioResponse obtenerLibroDiario(
            Long periodoId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String tipoAsiento,
            String estado,
            String busqueda
    );

    LibroMayorResponse obtenerLibroMayor(
            Long cuentaId,
            Long periodoId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    byte[] exportarLibroDiarioCsv(
            Long periodoId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String tipoAsiento,
            String estado,
            String busqueda
    );

    byte[] exportarLibroMayorCsv(
            Long cuentaId,
            Long periodoId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );
}
