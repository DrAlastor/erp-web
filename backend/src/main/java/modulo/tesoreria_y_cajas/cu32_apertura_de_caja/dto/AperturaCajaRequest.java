package modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AperturaCajaRequest(
        @NotNull(message = "La sucursal es obligatoria")
        Integer sucursalId,
        @NotNull(message = "El monto de apertura es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El monto no puede ser negativo")
        @Digits(integer = 10, fraction = 2)
        BigDecimal montoApertura,
        @Size(max = 255, message = "Las observaciones no pueden superar 255 caracteres")
        String observaciones) {
}
