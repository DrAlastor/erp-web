package modulo.inventario_y_almacenes.movimientos_de_inventario.security;

import lombok.RequiredArgsConstructor;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("inventarioAuthorization")
@RequiredArgsConstructor
public class InventarioAuthorization {

    private final UsuarioRepository usuarioRepository;
    private final modulo.seguridad_y_auditoria.roles_y_permisos.ServicioAutorizacion permisos;

    public boolean canRegister(Authentication auth) {
        var identidad = modulo.seguridad_y_auditoria.roles_y_permisos.auth.UsuarioActual.desde(auth);
        if (identidad.isPresent()) return permisos.tienePermiso(identidad.get().usuarioId(), "INVENTARIO_CREAR")
                || permisos.tienePermiso(identidad.get().usuarioId(), "INVENTARIO_MODIFICAR");
        return canAccess(auth);
    }

    public boolean canAccess(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;

        var identidad = modulo.seguridad_y_auditoria.roles_y_permisos.auth.UsuarioActual.desde(auth);
        if (identidad.isPresent()) return permisos.tienePermiso(identidad.get().usuarioId(), "INVENTARIO_CONSULTAR");

        boolean hasAuthority = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().startsWith("INVENTARIO:") ||
                a.getAuthority().contains("ADMIN") ||
                a.getAuthority().contains("ALMACENERO")
        );

        if (hasAuthority) return true;

        return usuarioRepository.findByUsername(auth.getName())
                .filter(u -> Boolean.TRUE.equals(u.getEnable()))
                .map(u -> u.getRoles().stream().anyMatch(r -> {
                    String nombre = r.getNombre().toUpperCase();
                    return nombre.contains("ADMIN") || nombre.contains("ALMACENERO");
                }))
                .orElse(false);
    }
}
