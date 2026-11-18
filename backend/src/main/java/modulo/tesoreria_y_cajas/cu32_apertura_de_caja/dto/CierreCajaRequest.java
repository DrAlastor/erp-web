package modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CierreCajaRequest(
        @NotNull(message = "El monto declarado es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El monto no puede ser negativo")
        @Digits(integer = 10, fraction = 2)
        BigDecimal montoCierre,
        @Size(max = 255)
        String observaciones) {
}
