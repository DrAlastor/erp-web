package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Ingreso o egreso manual dentro de un turno abierto (HU-34). La fecha la pone el servidor. */
public record RegistrarMovimientoRequest(
        @NotBlank(message = "El tipo de movimiento es obligatorio")
        @Pattern(regexp = "(?i)INGRESO|EGRESO", message = "El tipo debe ser INGRESO o EGRESO")
        String tipo,
        @NotNull(message = "La forma de pago es obligatoria")
        Long formaPagoId,
        @NotBlank(message = "El concepto es obligatorio")
        @Size(min = 3, max = 255, message = "El concepto debe tener entre 3 y 255 caracteres")
        String concepto,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales")
        BigDecimal monto,
        @Size(max = 50, message = "El número de comprobante no puede superar 50 caracteres")
        String numeroComprobante) {}
