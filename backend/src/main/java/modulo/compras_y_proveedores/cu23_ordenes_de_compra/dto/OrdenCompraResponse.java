package modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orden de compra completa, con sus renglones.
 *
 * @param registradaPorMi si quien consulta es quien la registró (y por lo tanto no puede aprobarla)
 */
public record OrdenCompraResponse(
        Integer id,
        String numeroOrden,
        LocalDate fecha,
        LocalDate fechaEntrega,
        Integer proveedorId,
        String proveedor,
        String proveedorNit,
        String proveedorCondicionPago,
        Integer proveedorDiasCredito,
        Long creadoPorId,
        String creadoPor,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal total,
        String estado,
        String observaciones,
        LocalDateTime fechaCreacion,
        boolean registradaPorMi,
        List<Detalle> detalles) {

    public record Detalle(
            Integer id,
            Long productoId,
            String codigo,
            String producto,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal subtotal) {
    }
}
