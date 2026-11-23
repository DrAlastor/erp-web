package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Línea de la lista "Movimientos del turno": un ingreso/egreso manual ({@code origen} MOVIMIENTO)
 * o el cobro de una venta ligada al turno ({@code origen} VENTA, {@code tipo} VENTA, solo lectura).
 */
public record MovimientoResponse(
        Long id,
        Long turnoCajaId,
        String tipo,
        Long formaPagoId,
        String formaPagoNombre,
        String concepto,
        BigDecimal monto,
        String numeroComprobante,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime fechaHora,
        String origen) {

    public static final String ORIGEN_MOVIMIENTO = "MOVIMIENTO";
    public static final String ORIGEN_VENTA = "VENTA";
}
