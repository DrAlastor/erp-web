package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Apertura mínima de un turno de caja: sucursal y fondo inicial (HU-34). */
public record AbrirTurnoRequest(
        @NotNull(message = "La sucursal es obligatoria")
        Integer sucursalId,
        @NotNull(message = "El monto de apertura es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto de apertura no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El monto de apertura admite hasta 2 decimales")
        BigDecimal montoApertura,
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
        String observaciones) {}
