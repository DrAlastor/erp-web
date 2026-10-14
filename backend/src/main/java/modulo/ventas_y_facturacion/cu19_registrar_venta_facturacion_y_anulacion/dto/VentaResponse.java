package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprobante completo (factura o nota de venta). En una nota de venta los datos del SIN
 * (autorización, código de control, leyenda) vienen en {@code null}. {@code asientoContable}
 * solo viene en la respuesta de la operación que lo generó (emisión o anulación).
 */
public record VentaResponse(
        Long id,
        String tipoComprobante,
        Integer numeroFactura,
        String numeroAutorizacion,
        String codigoControl,
        String leyendaSin,
        LocalDateTime fecha,
        String estado,
        String tipoVenta,
        Integer sucursalId,
        String sucursalNombre,
        Long clienteId,
        String nitCliente,
        String razonSocialCliente,
        Long cajeroId,
        Long turnoCajaId,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal iva,
        BigDecimal it,
        BigDecimal total,
        List<Linea> detalles,
        List<Pago> pagos,
        List<Cuota> cuentasPorCobrar,
        String motivoAnulacion,
        LocalDateTime fechaAnulacion,
        Long anuladoPor,
        String asientoContable) {

    public record Linea(
            Long id,
            Long productoId,
            String codigo,
            String nombre,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            BigDecimal subtotal) {
    }

    public record Pago(
            Long id,
            Long formaPagoId,
            String formaPago,
            BigDecimal importe,
            String transaccionExterna,
            String estado) {
    }

    public record Cuota(
            Long id,
            Integer numeroCuota,
            BigDecimal montoTotal,
            BigDecimal montoCobrado,
            BigDecimal saldo,
            LocalDate fechaVencimiento,
            String estado) {
    }
}
