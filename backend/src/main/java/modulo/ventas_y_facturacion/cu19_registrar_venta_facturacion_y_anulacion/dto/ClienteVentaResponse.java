package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

/** Datos fiscales del cliente que se copian a la factura. */
public record ClienteVentaResponse(Long id, String razonSocial, String nitCi) {
}
