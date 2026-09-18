package modulo.seguridad_y_auditoria.roles_y_permisos.web;

import modulo.seguridad_y_auditoria.roles_y_permisos.auth.UsuarioActual;
import modulo.seguridad_y_auditoria.roles_y_permisos.auth.UsuarioPrincipal;
import modulo.seguridad_y_auditoria.roles_y_permisos.ServicioRoles;
import modulo.seguridad_y_auditoria.roles_y_permisos.web.dto.PermisoDto;
import modulo.seguridad_y_auditoria.roles_y_permisos.web.dto.Peticiones;
import modulo.seguridad_y_auditoria.roles_y_permisos.web.dto.RolDto;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Administración de roles y de su matriz de permisos.
 *
 * <p>La protección es declarativa: cada método dice qué permiso exige y el 403 lo emite el
 * framework. La empresa nunca viene del cliente, sale del token: así nadie puede pedir los
 * roles de otro inquilino.
 */
@RestController
@RequestMapping("/api/seguridad")
public class ControladorRoles {

    private final ServicioRoles servicioRoles;

    public ControladorRoles(ServicioRoles servicioRoles) {
        this.servicioRoles = servicioRoles;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR')")
    public List<RolDto> listarRoles() {
        return servicioRoles.listar(UsuarioActual.obtener().empresaId());
    }

    @GetMapping("/permisos")
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR')")
    public List<PermisoDto> catalogoDePermisos() {
        return servicioRoles.catalogo();
    }

    @PutMapping("/roles/{rolId}/permisos")
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
    public RolDto reemplazarMatriz(@PathVariable UUID rolId, @Valid @RequestBody Peticiones.Matriz cuerpo) {
        UsuarioPrincipal actor = UsuarioActual.obtener();
        return servicioRoles.reemplazarMatriz(actor.empresaId(), rolId, cuerpo.permisos(), actor);
    }

    @PatchMapping("/roles/{rolId}/estado")
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
    public RolDto cambiarEstado(@PathVariable UUID rolId, @Valid @RequestBody Peticiones.Estado cuerpo) {
        UsuarioPrincipal actor = UsuarioActual.obtener();
        return servicioRoles.cambiarEstado(actor.empresaId(), rolId, cuerpo.activo(), actor);
    }
}
