package modulo.ventas_y_facturacion.cu13_gestionar_clientes.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ClienteRequest(
        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no puede superar 150 caracteres")
        String razonSocial,

        @NotBlank(message = "El NIT/CI es obligatorio")
        @Size(max = 30, message = "El NIT/CI no puede superar 30 caracteres")
        String nitCi,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String telefono,

        @Email(message = "El formato de correo electrónico no es válido")
        @Size(max = 100, message = "El correo electrónico no puede superar 100 caracteres")
        String email,

        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String direccion,

        @Size(max = 100, message = "La ciudad no puede superar 100 caracteres")
        String ciudad,

        @Pattern(regexp = "^(CONTADO|CREDITO)$", message = "La condición de pago debe ser CONTADO o CREDITO")
        String condicionPago,

        @DecimalMin(value = "0.00", message = "El límite de crédito no puede ser negativo")
        BigDecimal limiteCredito,

        @Min(value = 0, message = "Los días de crédito no pueden ser negativos")
        Integer diasCredito) {

    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
