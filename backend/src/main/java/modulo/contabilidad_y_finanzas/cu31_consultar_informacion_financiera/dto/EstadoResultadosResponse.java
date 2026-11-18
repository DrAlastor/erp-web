package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EstadoResultadosResponse(
        Integer gestion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String periodoNombre,
        Integer nivelDetalle,
        BigDecimal totalIngresosOperativos,
        BigDecimal totalCostoVentas,
        BigDecimal utilidadBruta,
        BigDecimal totalGastosOperativos,
        BigDecimal resultadoOperativo,
        BigDecimal otrosIngresos,
        BigDecimal otrosGastos,
        BigDecimal utilidadNeta,
        BigDecimal margenBrutoPorcentaje,
        BigDecimal margenNetoPorcentaje,
        List<RubroResultadoDTO> resumenEscalonado,
        List<NodoFinancieroDTO> detalleIngresos,
        List<NodoFinancieroDTO> detalleEgresos
) {}
