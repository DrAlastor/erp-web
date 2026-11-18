package modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ticket de la venta en caja: comprobante emitido y el cambio a entregar. */
public record VentaPosResponse(
        Long ventaId,
        String tipoComprobante,
        Integer numero,
        LocalDateTime fecha,
        Long turnoCajaId,
        String razonSocialCliente,
        String nitCliente,
        String formaPago,
        BigDecimal total,
        BigDecimal montoRecibido,
        BigDecimal cambio) {
}
