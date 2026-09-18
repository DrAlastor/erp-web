package modulo.seguridad_y_auditoria.acceso_al_sistema.mapper;

import modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth.TokenResponse;
import modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth.UsuarioPerfilResponse;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;

import org.springframework.stereotype.Component;

/**
 * Traduce el usuario autenticado a los DTO de la HU-01.
 *
 * <p>El perfil que acompaña al token es deliberadamente mínimo: identificador, usuario,
 * correo y nombre. Nunca el hash de la contraseña, ni los intentos fallidos, ni el bloqueo.
 */
@Component
public class AuthMapper {

    public UsuarioPerfilResponse toPerfil(Usuario usuario) {
        return new UsuarioPerfilResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getFullname());
    }

    /** Respuesta del login y del refresh: el token nuevo, el de renovación y el perfil. */
    public TokenResponse toTokenResponse(String accessToken, String refreshToken, long expiresIn, Usuario usuario) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn, toPerfil(usuario));
    }
}
