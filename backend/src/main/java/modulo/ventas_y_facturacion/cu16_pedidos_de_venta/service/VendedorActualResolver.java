package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.service;

import lombok.RequiredArgsConstructor;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.security.ApplicationUserPrincipal;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.repository.PedidoVentaConsultaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Obtiene el id de la tabla {@code usuario} del usuario autenticado, que es el vendedor
 * del pedido.
 *
 * <p>El filtro JWT deja un {@link ApplicationUserPrincipal} cuya identidad
 * ({@link UsuarioPrincipal#usuarioId()}) es el id de {@code usuario}. Igual se verifica que
 * exista en la tabla y, si no, se busca por el nombre de cuenta (el correo). Nunca se
 * asume un vendedor por defecto.
 */
@Component
@RequiredArgsConstructor
public class VendedorActualResolver {

    private final PedidoVentaConsultaRepository consultas;

    public Long resolver(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw sinVendedor();

        Long usuarioId = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof ApplicationUserPrincipal app && app.identidad() != null) {
            usuarioId = app.identidad().usuarioId();
        } else if (principal instanceof UsuarioPrincipal identidad) {
            usuarioId = identidad.usuarioId();
        }
        if (usuarioId != null && consultas.existeUsuarioActivo(usuarioId)) return usuarioId;

        return consultas.usuarioActivoPorEmail(authentication.getName())
                .orElseThrow(this::sinVendedor);
    }

    private ResponseStatusException sinVendedor() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN, "No se pudo identificar al vendedor");
    }
}
