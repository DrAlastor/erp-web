package modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.dto.AlmacenSimpleResponse;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.dto.ProductoSimpleResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs de la CU23, agrupados para no dispersar una docena de archivos de pocas lineas. */
public final class ComprasDtos {

    private ComprasDtos() {
    }

    // ------------------------------------------------------------ Entrada

    public record LineaCompraRequest(
            @NotNull(message = "El producto es obligatorio")
            Long productoId,

            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero")
            BigDecimal cantidad,

            @NotNull(message = "El costo unitario es obligatorio")
            @DecimalMin(value = "0.01", message = "El costo unitario debe ser mayor a cero")
            BigDecimal costoUnitario) {
    }

    public record RegistrarCompraRequest(
            @NotNull(message = "El proveedor es obligatorio")
            Integer proveedorId,

            @NotNull(message = "El almacén de destino es obligatorio")
            Integer sucursalId,

            @NotBlank(message = "El número de factura es obligatorio")
            @Size(max = 50, message = "El número de factura no puede exceder los 50 caracteres")
            String numeroFactura,

            @Size(max = 60, message = "El número de autorización no puede exceder los 60 caracteres")
            String numeroAutorizacion,

            @NotNull(message = "La fecha es obligatoria")
            LocalDate fecha,

            @NotBlank(message = "El tipo de pago es obligatorio (CONTADO o CREDITO)")
            String tipoPago,

            Integer ordenCompraId,

            String observaciones,

            @NotEmpty(message = "La compra debe tener al menos un producto")
            @Valid
            List<LineaCompraRequest> lineas) {
    }

    public record AnularCompraRequest(
            @NotBlank(message = "El motivo de anulación es obligatorio")
            @Size(max = 200, message = "El motivo no puede exceder los 200 caracteres")
            String motivo) {
    }

    public record LineaDevolucionRequest(
            @NotNull(message = "La línea de compra es obligatoria")
            Integer compraDetalleId,

            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero")
            BigDecimal cantidad) {
    }

    public record RegistrarDevolucionRequest(
            @NotBlank(message = "El motivo de la devolución es obligatorio")
            @Size(max = 255, message = "El motivo no puede exceder los 255 caracteres")
            String motivo,

            LocalDate fecha,

            @NotEmpty(message = "Debe devolver al menos un producto")
            @Valid
            List<LineaDevolucionRequest> lineas) {
    }

    // ------------------------------------------------------------- Salida

    public record CompraDetalleDTO(
            Integer id,
            Long productoId,
            String productoCodigo,
            String productoNombre,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal subtotal,
            BigDecimal cantidadDevuelta) {
    }

    public record CompraDTO(
            Integer id,
            Integer proveedorId,
            String proveedorNombre,
            Integer sucursalId,
            String sucursalNombre,
            Integer ordenCompraId,
            String numeroFactura,
            String numeroAutorizacion,
            LocalDate fecha,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            String tipoPago,
            String estado,
            String observaciones,
            LocalDateTime fechaCreacion,
            List<CompraDetalleDTO> detalles) {
    }

    public record DevolucionDetalleDTO(
            Integer id,
            Integer compraDetalleId,
            Long productoId,
            String productoNombre,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal subtotal) {
    }

    public record DevolucionDTO(
            Integer id,
            Integer compraId,
            String numeroDevolucion,
            String numeroFactura,
            String proveedorNombre,
            LocalDate fecha,
            String motivo,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            List<DevolucionDetalleDTO> detalles) {
    }

    public record ProveedorItemDTO(Integer id, String razonSocial, String nitCi, Integer diasCredito) {
    }

    public record CatalogosDTO(
            List<ProveedorItemDTO> proveedores,
            List<AlmacenSimpleResponse> almacenes,
            List<ProductoSimpleResponse> productos) {
    }
}
