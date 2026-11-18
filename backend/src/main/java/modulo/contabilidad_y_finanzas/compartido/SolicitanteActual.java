package modulo.contabilidad_y_finanzas.compartido;

import jakarta.servlet.http.HttpServletRequest;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;

import java.security.Principal;

/** Arma el {@link Solicitante} a partir del usuario autenticado y la petición HTTP. */
public final class SolicitanteActual {

    private SolicitanteActual() {
    }

    public static Solicitante de(Principal actor, HttpServletRequest http) {
        UsuarioPrincipal usuario = UsuarioActual.obtener();
        String nombre = actor != null ? actor.getName() : usuario.email();
        return new Solicitante(usuario.usuarioId(), nombre, http.getRemoteAddr());
    }
}
