package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pago a aplicar sobre una cuota. Que el monto no supere el saldo se valida en el servicio
 * (409). Sin fecha se toma la de hoy.
 */
public record RegistrarPagoRequest(
        @NotNull(message = "La forma de pago es obligatoria")
        Long formaPagoId,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales")
        BigDecimal monto,
        @PastOrPresent(message = "La fecha de pago no puede ser futura")
        LocalDate fechaPago,
        @Size(max = 50, message = "El número de recibo no puede superar 50 caracteres")
        String numeroRecibo,
        @Size(max = 100, message = "La transacción externa no puede superar 100 caracteres")
        String transaccionExterna,
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones) {}
