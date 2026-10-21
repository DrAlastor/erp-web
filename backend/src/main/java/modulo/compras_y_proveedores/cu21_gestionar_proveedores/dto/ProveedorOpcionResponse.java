package modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto;

/** Proveedor activo en forma corta, para los combos de órdenes de compra y compras. */
public record ProveedorOpcionResponse(
        Integer id,
        String razonSocial,
        String nitCi,
        String condicionPago,
        Integer diasCredito) {
}
