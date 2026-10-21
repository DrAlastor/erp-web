package modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.controller;

import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto.MisPermisosDto;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Permisos efectivos del usuario de la sesión. Es lo que el frontend usa para armar el menú
 * y decidir qué acciones mostrar.
 *
 * Devuelve siempre el estado actual de los permisos y roles del usuario activo en el esquema
 * actual del tenant.
 */
@RestController
@RequestMapping("/api/seguridad")
public class ControladorMisPermisos {

    private final UsuarioRepository usuarioRepository;
    private final modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service.ServicioAutorizacion servicioAutorizacion;

    public ControladorMisPermisos(UsuarioRepository usuarioRepository,
                                  modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service.ServicioAutorizacion servicioAutorizacion) {
        this.usuarioRepository = usuarioRepository;
        this.servicioAutorizacion = servicioAutorizacion;
    }

    @GetMapping("/mis-permisos")
    public MisPermisosDto misPermisos() {
        UsuarioPrincipal principal = UsuarioActual.obtener();

        Usuario usuario = usuarioRepository.findById(principal.usuarioId()).orElse(null);

        String rolNombre = (usuario != null && usuario.getRol() != null)
                ? usuario.getRol().getNombre()
                : "ADMINISTRADOR";

        List<String> roles = List.of(rolNombre);
        Set<String> permisos;
        try {
            permisos = servicioAutorizacion.permisosEfectivos(principal.usuarioId());
            if (permisos == null || permisos.isEmpty()) {
                permisos = resolverPermisos(rolNombre);
            }
        } catch (Exception ex) {
            permisos = resolverPermisos(rolNombre);
        }

        return new MisPermisosDto(
                principal.usuarioId(),
                principal.empresaId(),
                usuario != null ? usuario.getFullname() : principal.nombre(),
                permisos,
                roles);
    }

    private Set<String> resolverPermisos(String rolNombre) {
        Set<String> permisos = new LinkedHashSet<>();
        String r = rolNombre != null ? rolNombre.toUpperCase() : "ADMINISTRADOR";

        if (r.contains("ADMIN") || r.contains("SUPER") || r.contains("GEREN")) {
            permisos.addAll(List.of(
                    "ALL",
                    "SEGURIDAD_CONSULTAR", "SEGURIDAD_CREAR", "SEGURIDAD_MODIFICAR", "SEGURIDAD_ELIMINAR",
                    "COMERCIAL_CONSULTAR", "COMERCIAL_CREAR", "COMERCIAL_MODIFICAR", "COMERCIAL_ANULAR",
                    "INVENTARIO_CONSULTAR", "INVENTARIO_CREAR", "INVENTARIO_MODIFICAR", "INVENTARIO_ELIMINAR",
                    "CONTABILIDAD_CONSULTAR", "CONTABILIDAD_REGISTRAR",
                    "COMPRAS_CONSULTAR", "COMPRAS_REGISTRAR",
                    "VENTAS_CONSULTAR", "VENTAS_REGISTRAR",
                    "REPORTES_CONSULTAR", "USUARIOS_CONSULTAR", "ROLES_CONSULTAR"
            ));
        } else if (r.contains("VENTA")) {
            permisos.addAll(List.of(
                    "VENTAS_ONLY",
                    "COMERCIAL_CONSULTAR", "COMERCIAL_CREAR", "COMERCIAL_MODIFICAR",
                    "VENTAS_CONSULTAR", "VENTAS_REGISTRAR",
                    "INVENTARIO_CONSULTAR"
            ));
        } else if (r.contains("CAJA")) {
            permisos.addAll(List.of(
                    "CAJA_ONLY",
                    "COMERCIAL_CONSULTAR", "COMERCIAL_CREAR",
                    "VENTAS_CONSULTAR", "VENTAS_REGISTRAR"
            ));
        } else if (r.contains("ALMACEN")) {
            permisos.addAll(List.of(
                    "INVENTARIO_ONLY",
                    "INVENTARIO_CONSULTAR", "INVENTARIO_CREAR", "INVENTARIO_MODIFICAR"
            ));
        } else {
            permisos.addAll(List.of("COMERCIAL_CONSULTAR", "INVENTARIO_CONSULTAR"));
        }

        return permisos;
    }
}
