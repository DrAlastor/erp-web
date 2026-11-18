package modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CierreCajaResponse(
        Long turnoId,
        String estado,
        BigDecimal montoApertura,
        BigDecimal montoEsperado,
        BigDecimal montoCierre,
        BigDecimal diferencia,
        LocalDateTime fechaCierre) {
}
