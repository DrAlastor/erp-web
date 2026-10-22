package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Emisión de una venta con su factura.
 *
 * <p>No trae precios ni el cajero: el precio sale del catálogo y el cajero es el usuario
 * autenticado, así el cliente HTTP no puede alterarlos.
 */
public record VentaRequest(
        @NotNull(message = "La sucursal es obligatoria")
        Integer sucursalId,
        Long clienteId,
        Long turnoCajaId,
        @NotBlank(message = "El tipo de venta es obligatorio")
        @Pattern(regexp = "CONTADO|CREDITO", message = "El tipo de venta debe ser CONTADO o CREDITO")
        String tipoVenta,
        /** FACTURA (por defecto) o NOTA_VENTA. */
        @Pattern(regexp = "FACTURA|NOTA_VENTA", message = "El comprobante debe ser FACTURA o NOTA_VENTA")
        String tipoComprobante,
        @Min(value = 1, message = "Los días de crédito deben ser al menos 1")
        @Max(value = 365, message = "Los días de crédito no pueden superar 365")
        Integer diasCredito,
        Integer cotizacionId,
        @NotEmpty(message = "La venta debe tener al menos un producto")
        @Size(max = 100, message = "La venta no puede superar 100 líneas")
        List<@Valid Linea> detalles,
        @Size(max = 10, message = "La venta no puede superar 10 formas de pago")
        List<@Valid Pago> pagos) {

    public record Linea(
            @NotNull(message = "El producto es obligatorio")
            Long productoId,
            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero")
            @Digits(integer = 10, fraction = 2, message = "La cantidad admite hasta 2 decimales")
            BigDecimal cantidad,
            Long descuentoId,
            @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
            @Digits(integer = 10, fraction = 2, message = "El descuento admite hasta 2 decimales")
            BigDecimal descuento) {
    }

    public record Pago(
            @NotNull(message = "La forma de pago es obligatoria")
            Long formaPagoId,
            @NotNull(message = "El importe es obligatorio")
            @DecimalMin(value = "0.01", message = "El importe debe ser mayor a cero")
            @Digits(integer = 10, fraction = 2, message = "El importe admite hasta 2 decimales")
            BigDecimal importe,
            @Size(max = 100, message = "La referencia de la transacción no puede superar 100 caracteres")
            String transaccionExterna) {
    }

    /** Comprobante pedido; si no viene, se emite factura. */
    public String comprobante() {
        return tipoComprobante == null || tipoComprobante.isBlank() ? "FACTURA" : tipoComprobante;
    }
}
