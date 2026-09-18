package modulo.inventario_y_existencia.movimiento_inventario.security;

import lombok.RequiredArgsConstructor;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("inventarioAuthorization")
@RequiredArgsConstructor
public class InventarioAuthorization {

    private final UsuarioRepository usuarioRepository;

    public boolean canAccess(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;

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
