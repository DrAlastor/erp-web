package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Turno de caja con su efectivo esperado calculado al momento de la consulta. */
public record TurnoResponse(
        Long id,
        Integer sucursalId,
        String sucursalNombre,
        Long cajeroId,
        String cajeroNombre,
        BigDecimal montoApertura,
        BigDecimal montoCierre,
        BigDecimal diferencia,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fechaApertura,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fechaCierre,
        String estado,
        String observaciones,
        BigDecimal efectivoEsperado) {}
