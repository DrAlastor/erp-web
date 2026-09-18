package modulo.seguridad_y_auditoria.roles_y_permisos.controller;

import modulo.seguridad_y_auditoria.roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.roles_y_permisos.security.UsuarioPrincipal;
import modulo.seguridad_y_auditoria.roles_y_permisos.service.ServicioAsignaciones;
import modulo.seguridad_y_auditoria.roles_y_permisos.dto.AsignacionDto;
import modulo.seguridad_y_auditoria.roles_y_permisos.dto.Peticiones;
import modulo.seguridad_y_auditoria.roles_y_permisos.dto.UsuarioResumenDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Asignación de roles a los usuarios de la empresa. */
@RestController
@RequestMapping("/api/seguridad/usuarios")
public class ControladorAsignaciones {

    private final ServicioAsignaciones servicioAsignaciones;

    public ControladorAsignaciones(ServicioAsignaciones servicioAsignaciones) {
        this.servicioAsignaciones = servicioAsignaciones;
    }

    @GetMapping
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR')")
    public List<UsuarioResumenDto> listarUsuarios() {
        return servicioAsignaciones.usuariosDeLaEmpresa(UsuarioActual.obtener().empresaId());
    }

    @GetMapping("/{usuarioId}/roles")
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR')")
    public List<AsignacionDto> rolesDelUsuario(@PathVariable Long usuarioId) {
        return servicioAsignaciones.rolesDe(UsuarioActual.obtener().empresaId(), usuarioId);
    }

    @PostMapping("/{usuarioId}/roles")
    @PreAuthorize("hasPermission('SEGURIDAD','CREAR')")
    @ResponseStatus(HttpStatus.CREATED)
    public void asignarRol(@PathVariable Long usuarioId, @Valid @RequestBody Peticiones.Asignacion cuerpo) {
        UsuarioPrincipal actor = UsuarioActual.obtener();
        servicioAsignaciones.asignar(actor.empresaId(), usuarioId, cuerpo.rolId(), actor);
    }

    @DeleteMapping("/{usuarioId}/roles/{rolId}")
    @PreAuthorize("hasPermission('SEGURIDAD','ANULAR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitarRol(@PathVariable Long usuarioId, @PathVariable UUID rolId) {
        UsuarioPrincipal actor = UsuarioActual.obtener();
        servicioAsignaciones.quitar(actor.empresaId(), usuarioId, rolId, actor);
    }
}
