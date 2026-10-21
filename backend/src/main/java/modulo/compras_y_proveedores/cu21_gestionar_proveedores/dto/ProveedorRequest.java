package modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.CondicionPago;

/**
 * Datos para registrar o modificar un proveedor.
 *
 * <p>Que CONTADO lleve 0 días y CREDITO más de 0 lo valida el servicio, porque depende de
 * dos campos a la vez.
 */
public record ProveedorRequest(
        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no puede exceder 150 caracteres") String razonSocial,
        @NotBlank(message = "El NIT/CI es obligatorio")
        @Pattern(regexp = "\\d{4,30}", message = "El NIT/CI debe ser numérico (entre 4 y 30 dígitos)") String nitCi,
        @Size(max = 100, message = "El contacto no puede exceder 100 caracteres") String contacto,
        @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{6,30}$", message = "El teléfono sólo admite dígitos, espacios, +, - y paréntesis")
        String telefono,
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 100, message = "El correo no puede exceder 100 caracteres") String email,
        @Size(max = 255, message = "La dirección no puede exceder 255 caracteres") String direccion,
        @Size(max = 100, message = "La ciudad no puede exceder 100 caracteres") String ciudad,
        @NotNull(message = "La condición de pago es obligatoria") CondicionPago condicionPago,
        @Min(value = 0, message = "Los días de crédito no pueden ser negativos")
        @Max(value = 365, message = "Los días de crédito no pueden superar 365") Integer diasCredito) {

    @JsonAnySetter
    public void rechazar(String campo, Object valor) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + campo);
    }
}
