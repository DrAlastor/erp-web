package modulo.seguridad_y_auditoria.gestion_de_usuarios.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * Alta de una cuenta de usuario.
 *
 * <p>El cliente solo puede enviar estos campos. Cualquier otro —un hash de contraseña, un
 * identificador, un rol— se rechaza con 400 en lugar de ignorarse en silencio, para que un
 * intento de saltarse la regla quede visible.
 */
public record UsuarioRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}") String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 150) String fullname,
        @NotNull @Size(max = 7) Set<@NotNull UUID> rolesIds) {

    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
