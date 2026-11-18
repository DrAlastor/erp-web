package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto;

import java.math.BigDecimal;
import java.util.List;

public record NodoFinancieroDTO(
        Long id,
        String codigo,
        String nombre,
        Integer nivel,
        String tipo,
        String naturaleza,
        Boolean permiteMovimiento,
        BigDecimal saldo,
        BigDecimal porcentajeVertical,
        List<NodoFinancieroDTO> subcuentas
) {}
