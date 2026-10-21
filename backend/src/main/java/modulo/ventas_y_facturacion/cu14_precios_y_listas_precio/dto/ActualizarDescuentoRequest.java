package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ActualizarDescuentoRequest(
        @NotBlank(message = "El nombre del descuento o promoción es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El tipo de descuento es obligatorio")
        @Pattern(regexp = "^(PORCENTAJE|MONTO_FIJO)$", message = "El tipo debe ser PORCENTAJE o MONTO_FIJO")
        String tipo,

        @NotNull(message = "El valor del descuento es obligatorio")
        @DecimalMin(value = "0.01", message = "El valor del descuento debe ser mayor a 0")
        BigDecimal valor,

        LocalDate fechaInicio,
        LocalDate fechaFin,
        Boolean activo) {}
