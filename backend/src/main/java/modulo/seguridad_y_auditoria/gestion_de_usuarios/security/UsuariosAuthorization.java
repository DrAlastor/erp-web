package modulo.seguridad_y_auditoria.gestion_de_usuarios.security;

import lombok.RequiredArgsConstructor;

import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.UsuarioRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("usuariosAuthorization")
@RequiredArgsConstructor
public class UsuariosAuthorization {
    private final UsuarioRepository usuarios;

    public boolean canManage(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        boolean permission =
                auth.getAuthorities().stream()
                        .anyMatch(
                                a ->
                                        a.getAuthority().equals("SEGURIDAD:USUARIOS:ESCRITURA")
                                                || a.getAuthority()
                                                        .equals("ACCESO:USUARIOS:ESCRITURA"));
        return permission
                && usuarios.findByUsername(auth.getName())
                        .filter(u -> Boolean.TRUE.equals(u.getEnable()))
                        .map(
                                u ->
                                        u.getRoles().stream()
                                                .anyMatch(
                                                        r ->
                                                                r.getNombre()
                                                                                .equalsIgnoreCase(
                                                                                        "ADMIN")
                                                                        || r.getNombre()
                                                                                .equalsIgnoreCase(
                                                                                        "ADMINISTRADOR")))
                        .orElse(false);
    }
}
