package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto;

import java.math.BigDecimal;

public record DetalleDespachoResponse(
        Integer id,
        Long productoId,
        String productoCodigo,
        String productoNombre,
        BigDecimal cantidad) {
}
