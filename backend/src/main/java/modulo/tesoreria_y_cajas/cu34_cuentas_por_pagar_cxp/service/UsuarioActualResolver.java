package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service;

import lombok.RequiredArgsConstructor;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.security.ApplicationUserPrincipal;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.repository.CuentaPorPagarConsultaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Obtiene el id de la tabla {@code usuario} del usuario autenticado, que queda como
 * creador de las cuotas y responsable de los pagos. Mismo criterio que el
 * {@code VendedorActualResolver} de la CU16: identidad del token verificada contra la
 * tabla y, si no, busqueda por correo. Nunca se asume un usuario por defecto.
 */
@Component("usuarioActualResolverCxp")
@RequiredArgsConstructor
public class UsuarioActualResolver {

    private final CuentaPorPagarConsultaRepository consultas;

    public Long resolver(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw sinUsuario();

        Long usuarioId = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof ApplicationUserPrincipal app && app.identidad() != null) {
            usuarioId = app.identidad().usuarioId();
        } else if (principal instanceof UsuarioPrincipal identidad) {
            usuarioId = identidad.usuarioId();
        }
        if (usuarioId != null && consultas.existeUsuarioActivo(usuarioId)) return usuarioId;

        return consultas.usuarioActivoPorEmail(authentication.getName())
                .orElseThrow(this::sinUsuario);
    }

    private ResponseStatusException sinUsuario() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN, "No se pudo identificar al usuario que registra la operación");
    }
}
