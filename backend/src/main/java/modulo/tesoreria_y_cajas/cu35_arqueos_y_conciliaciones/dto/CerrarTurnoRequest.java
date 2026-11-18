package modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Cierre del turno con el efectivo contado. Que las observaciones sean obligatorias cuando
 * hay diferencia se valida en el servicio (400).
 */
public record CerrarTurnoRequest(
        @NotNull(message = "El monto contado es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto contado no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El monto contado admite hasta 2 decimales")
        BigDecimal montoContado,
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones) {}
