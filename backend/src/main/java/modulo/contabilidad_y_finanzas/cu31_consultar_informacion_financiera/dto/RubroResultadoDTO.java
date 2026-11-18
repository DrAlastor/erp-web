package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto;

import java.math.BigDecimal;

public record RubroResultadoDTO(
        String clave,
        String titulo,
        BigDecimal monto,
        BigDecimal porcentajeVertical
) {}
