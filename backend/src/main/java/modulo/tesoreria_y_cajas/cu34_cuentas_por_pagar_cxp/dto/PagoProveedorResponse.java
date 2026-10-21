package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Un pago del historial de la cuota. */
public record PagoProveedorResponse(
        Long id,
        Long formaPagoId,
        String formaPagoNombre,
        BigDecimal monto,
        LocalDate fechaPago,
        String numeroRecibo,
        String transaccionExterna,
        String observaciones,
        String usuarioNombre,
        LocalDateTime fechaCreacion) {}
