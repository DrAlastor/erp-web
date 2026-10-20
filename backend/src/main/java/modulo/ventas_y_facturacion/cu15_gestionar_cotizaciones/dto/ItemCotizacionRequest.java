package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Un renglón de la cotización: producto, cantidad y descuento opcional en bolivianos. */
public record ItemCotizacionRequest(
        @NotNull(message = "El producto es obligatorio") Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a cero") BigDecimal cantidad,
        @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo") BigDecimal descuento) {

    @JsonAnySetter
    public void rechazar(String campo, Object valor) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + campo);
    }
}
