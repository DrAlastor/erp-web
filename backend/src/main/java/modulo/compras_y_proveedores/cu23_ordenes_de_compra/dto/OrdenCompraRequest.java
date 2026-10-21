package modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos para registrar o modificar una orden de compra.
 *
 * <p>Número, estado, totales y quién la crea los pone el servidor.
 *
 * @param borrador si es {@code true} queda en BORRADOR; si no, queda PENDIENTE de aprobación
 */
public record OrdenCompraRequest(
        @NotNull(message = "El proveedor es obligatorio") Integer proveedorId,
        LocalDate fechaEntrega,
        @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo") BigDecimal descuento,
        @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres") String observaciones,
        Boolean borrador,
        @NotEmpty(message = "La orden debe tener al menos un producto")
        @Size(max = 100, message = "Una orden admite hasta 100 renglones")
        List<@Valid @NotNull ItemOrdenCompraRequest> items) {

    @JsonAnySetter
    public void rechazar(String campo, Object valor) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + campo);
    }
}
