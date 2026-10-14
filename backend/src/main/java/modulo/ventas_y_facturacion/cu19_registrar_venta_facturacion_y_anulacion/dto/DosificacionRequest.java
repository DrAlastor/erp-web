package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Autorización de facturación otorgada por el SIN. */
public record DosificacionRequest(
        @NotBlank(message = "El número de autorización es obligatorio")
        @Size(max = 60, message = "El número de autorización no puede superar 60 caracteres")
        @Pattern(regexp = "[0-9A-Za-z-]+", message = "El número de autorización solo admite letras, números y guiones")
        String numeroAutorizacion,
        @NotBlank(message = "La leyenda del SIN es obligatoria")
        @Size(max = 500, message = "La leyenda no puede superar 500 caracteres")
        String leyendaSin,
        @NotNull(message = "El número inicial es obligatorio")
        @Min(value = 1, message = "El número inicial debe ser al menos 1")
        Integer numeroInicial,
        @NotNull(message = "El número final es obligatorio")
        @Max(value = 999_999_999, message = "El número final es demasiado grande")
        Integer numeroFinal,
        @NotNull(message = "La fecha límite de emisión es obligatoria")
        @Future(message = "La fecha límite de emisión debe ser posterior a hoy")
        LocalDate fechaLimite) {
}
