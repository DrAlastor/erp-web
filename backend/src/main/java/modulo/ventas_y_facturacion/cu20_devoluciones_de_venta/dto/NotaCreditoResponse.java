package modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Nota de crédito completa. El ajuste financiero ({@code montoAplicadoCxc},
 * {@code montoReembolsado}) y el asiento solo vienen en la respuesta de la emisión.
 */
public record NotaCreditoResponse(
        Long id,
        String numeroNota,
        LocalDate fecha,
        String motivo,
        String estado,
        BigDecimal total,
        String observaciones,
        Long ventaId,
        String tipoComprobante,
        Integer numeroFactura,
        String nitCliente,
        String razonSocialCliente,
        String sucursalNombre,
        String tipoVenta,
        Long emitidoPor,
        List<Linea> detalles,
        BigDecimal montoAplicadoCxc,
        BigDecimal montoReembolsado,
        String asientoContable) {

    public record Linea(
            Long id,
            Long ventaDetalleId,
            Long productoId,
            String codigo,
            String nombre,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {
    }
}
