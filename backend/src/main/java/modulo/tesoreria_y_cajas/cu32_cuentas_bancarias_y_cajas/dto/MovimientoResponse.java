package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movimiento de caja (ingreso o egreso) de un turno. */
public record MovimientoResponse(
        Long id,
        Long turnoCajaId,
        String tipo,
        Long formaPagoId,
        String formaPagoNombre,
        String concepto,
        BigDecimal monto,
        String numeroComprobante,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fechaHora) {}
