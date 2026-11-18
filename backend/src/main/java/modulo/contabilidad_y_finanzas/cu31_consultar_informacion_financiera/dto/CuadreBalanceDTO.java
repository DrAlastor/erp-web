package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto;

import java.math.BigDecimal;

public record CuadreBalanceDTO(
        BigDecimal totalActivos,
        BigDecimal totalPasivos,
        BigDecimal totalPatrimonioCuentas,
        BigDecimal utilidadEjercicio,
        BigDecimal totalPatrimonioNeto,
        BigDecimal totalPasivoMasPatrimonio,
        BigDecimal diferencia,
        boolean estaCuadrado
) {}
