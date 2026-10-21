package modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto;

import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorOpcionResponse;

import java.math.BigDecimal;
import java.util.List;

/** Lo que necesita el formulario de la orden: proveedores activos, productos y sucursales. */
public record OpcionesOrdenCompraResponse(
        List<ProveedorOpcionResponse> proveedores,
        List<ProductoComprable> productos,
        List<SucursalOpcion> sucursales) {

    /** Producto activo con el costo de su última compra (o el costo del catálogo si nunca se compró). */
    public record ProductoComprable(Long id, String codigo, String nombre, BigDecimal ultimoCosto) {
    }

    public record SucursalOpcion(Integer id, String nombre) {
    }
}
