package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos para registrar o modificar una cotización.
 *
 * <p>El precio de cada renglón no viaja en la petición: lo pone el servidor desde el
 * catálogo, para que nadie pueda cotizar a un precio inventado desde el navegador.
 *
 * @param borrador si es {@code true} queda en BORRADOR; si no, se emite directamente
 */
public record CotizacionRequest(
        @NotNull(message = "El cliente es obligatorio") Long clienteId,
        @Min(value = 1, message = "La validez mínima es de 1 día")
        @Max(value = 90, message = "La validez máxima es de 90 días") Integer validezDias,
        @DecimalMin(value = "0.00", message = "El descuento global no puede ser negativo")
        BigDecimal descuentoGlobal,
        @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres")
        String observaciones,
        Boolean borrador,
        @NotEmpty(message = "La cotización debe tener al menos un producto")
        @Size(max = 100, message = "Una cotización admite hasta 100 renglones")
        List<@Valid @NotNull ItemCotizacionRequest> items) {

    @JsonAnySetter
    public void rechazar(String campo, Object valor) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + campo);
    }
}
