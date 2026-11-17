package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cobro registrado sobre una cuota, para el historial y el recibo. */
public record CobroResponse(
        Long id,
        String numeroRecibo,
        Long formaPagoId,
        String formaPago,
        BigDecimal monto,
        LocalDate fechaCobro,
        String transaccionExterna,
        String observaciones,
        Long turnoCajaId,
        String usuario,
        LocalDateTime fechaCreacion) {}
