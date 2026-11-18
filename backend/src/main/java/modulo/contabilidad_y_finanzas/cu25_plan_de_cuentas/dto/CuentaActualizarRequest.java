package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos modificables de una cuenta. Código, padre, tipo y naturaleza son inmutables para no
 * romper la jerarquía ni los asientos ya registrados.
 */
public record CuentaActualizarRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,
        @NotNull(message = "Indique si la cuenta permite movimiento")
        Boolean permiteMovimiento) {

    @JsonAnySetter
    public void rechazar(String campo, Object valor) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + campo);
    }
}
