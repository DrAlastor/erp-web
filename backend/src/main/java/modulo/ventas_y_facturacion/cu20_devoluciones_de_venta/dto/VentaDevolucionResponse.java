package modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Factura vigente con lo vendido, lo ya devuelto y lo que aún se puede devolver por línea.
 */
public record VentaDevolucionResponse(
        Long ventaId,
        String tipoComprobante,
        Integer numeroFactura,
        String numeroAutorizacion,
        LocalDateTime fecha,
        String nitCliente,
        String razonSocialCliente,
        String sucursalNombre,
        String tipoVenta,
        BigDecimal total,
        List<Linea> lineas) {

    public record Linea(
            Long ventaDetalleId,
            Long productoId,
            String codigo,
            String nombre,
            BigDecimal cantidadVendida,
            BigDecimal cantidadDevuelta,
            BigDecimal cantidadDisponible,
            BigDecimal precioUnitarioNeto) {
    }
}
