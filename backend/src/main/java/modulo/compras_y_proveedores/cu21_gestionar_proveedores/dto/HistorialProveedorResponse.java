package modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el proveedor tiene con la empresa: sus órdenes de compra, sus compras y lo que
 * todavía se le debe en cuentas por pagar.
 */
public record HistorialProveedorResponse(
        ProveedorResponse proveedor,
        List<Documento> ordenes,
        List<Documento> compras,
        BigDecimal saldoPendiente,
        long ordenesAbiertas) {

    /** Una orden de compra o una compra, con lo mínimo para listarla. */
    public record Documento(Integer id, String numero, LocalDate fecha, BigDecimal total, String estado) {
    }
}
