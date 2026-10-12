package comun.config;

import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service.ServicioAutorizacion;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.catalogo.CatalogoPermisos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Puente entre las anotaciones de Spring Security y el {@link ServicioAutorizacion}.
 *
 * <p>Permite escribir la protección de un endpoint de forma declarativa:
 *
 * <pre>
 * &#64;PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
 * </pre>
 *
 * <p>Gracias a esto la verificación no queda escrita a mano dentro de cada método, que es
 * donde se olvida y quedan agujeros. El 403 lo emite el framework de manera uniforme.
 *
 * <p><strong>Falla cerrado.</strong> Ante cualquier duda —nadie autenticado, un principal
 * de otro tipo, un código que no existe en el catálogo— devuelve {@code false}. En
 * particular, validar el código contra el catálogo hace que un typo en una anotación
 * deniegue el acceso en lugar de concederlo por accidente.
 */
@Component
public class EvaluadorDePermisos implements PermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(EvaluadorDePermisos.class);

    private final ServicioAutorizacion servicioAutorizacion;

    public EvaluadorDePermisos(ServicioAutorizacion servicioAutorizacion) {
        this.servicioAutorizacion = servicioAutorizacion;
    }

    /**
     * Evalúa {@code hasPermission(modulo, accion)}.
     *
     * <p>También acepta el código completo en el segundo argumento, dejando el primero en
     * {@code null}: {@code hasPermission(null, 'COMERCIAL_ANULAR')}.
     */
    @Override
    public boolean hasPermission(Authentication autenticacion, Object modulo, Object accion) {
        UsuarioPrincipal principal = principalDe(autenticacion);
        if (principal == null) {
            return false;
        }

        String codigo = componerCodigo(modulo, accion);
        if (codigo == null) {
            return false;
        }

        // 1. Administrador o permiso global ALL
        boolean esAdmin = autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ALL")
                        || a.getAuthority().equalsIgnoreCase("ROLE_ADMINISTRADOR")
                        || a.getAuthority().equalsIgnoreCase("ROLE_ADMIN")
                        || a.getAuthority().equalsIgnoreCase("ROLE_SUPERADMIN")
                        || a.getAuthority().toUpperCase().contains("ADMIN"));
        if (esAdmin) {
            return true;
        }

        // 2. Comprobar si la autoridad solicitada está en las GrantedAuthorities
        boolean tieneAutoridad = autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase(codigo)
                        || a.getAuthority().equalsIgnoreCase("ROLE_" + codigo));
        if (tieneAutoridad) {
            return true;
        }

        try {
            return servicioAutorizacion.tienePermiso(principal.usuarioId(), codigo);
        } catch (Exception e) {
            log.debug("No se pudo consultar servicioAutorizacion legacy: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Variante por identificador de objeto. El ERP autoriza por módulo y acción, no por
     * instancia, así que no se usa: deniega.
     */
    @Override
    public boolean hasPermission(Authentication autenticacion, Serializable idDelObjeto,
                                 String tipoDeObjeto, Object permiso) {
        log.warn("Se usó la variante de hasPermission por instancia ({} {}), que el ERP no implementa",
                tipoDeObjeto, idDelObjeto);
        return false;
    }

    private UsuarioPrincipal principalDe(Authentication autenticacion) {
        if (autenticacion == null
                || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual.desde(autenticacion).orElse(null);
    }

    private String componerCodigo(Object modulo, Object accion) {
        if (!(accion instanceof String textoAccion) || textoAccion.isBlank()) {
            return null;
        }
        if (modulo == null) {
            return textoAccion;
        }
        if (!(modulo instanceof String textoModulo) || textoModulo.isBlank()) {
            return null;
        }
        return textoModulo + "_" + textoAccion;
    }
}
