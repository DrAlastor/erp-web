package modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila del listado de órdenes de compra. */
public record OrdenCompraResumenResponse(
        Integer id,
        String numeroOrden,
        LocalDate fecha,
        LocalDate fechaEntrega,
        Integer proveedorId,
        String proveedor,
        BigDecimal total,
        String estado,
        Long creadoPorId) {
}
