package modulo.seguridad_y_auditoria.gestion_de_usuarios.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Edición de los datos personales de una cuenta.
 *
 * <p>Solo nombre completo y correo: el usuario, la contraseña, los roles y el estado se
 * cambian por otras operaciones, así que enviarlos aquí se rechaza con 400.
 */
public record UsuarioUpdateRequest(
        @NotBlank @Size(max = 150) String fullname,
        @NotBlank @Email @Size(max = 100) String email) {

    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
