package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar una cuenta.
 *
 * <p>Una cuenta raíz (sin padre) exige tipo y naturaleza; una subcuenta los hereda de su padre,
 * así que los que vengan se ignoran. {@code permiteMovimiento} por defecto es {@code true}.
 */
public record CuentaRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 50, message = "El código no puede exceder 50 caracteres")
        @Pattern(regexp = "^\\d+(\\.\\d+)*$", message = "El código solo admite números separados por puntos (ej. 1.1.1.01)")
        String codigo,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,
        Long padreId,
        @Size(max = 30, message = "El tipo no puede exceder 30 caracteres") String tipo,
        @Size(max = 20, message = "La naturaleza no puede exceder 20 caracteres") String naturaleza,
        Boolean permiteMovimiento) {

    @JsonAnySetter
    public void rechazar(String campo, Object valor) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + campo);
    }
}
