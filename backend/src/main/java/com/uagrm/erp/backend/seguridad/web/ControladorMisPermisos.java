package com.uagrm.erp.backend.seguridad.web;

import com.uagrm.erp.backend.auth.UsuarioActual;
import com.uagrm.erp.backend.auth.UsuarioPrincipal;
import com.uagrm.erp.backend.seguridad.ServicioAsignaciones;
import com.uagrm.erp.backend.seguridad.ServicioAutorizacion;
import com.uagrm.erp.backend.seguridad.web.dto.AsignacionDto;
import com.uagrm.erp.backend.seguridad.web.dto.MisPermisosDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Permisos efectivos del usuario de la sesión. Es lo que el frontend usa para armar el menú
 * y decidir qué acciones mostrar.
 *
 * <p>No exige ningún permiso especial —cualquiera autenticado puede preguntar por lo suyo—
 * y devuelve siempre el estado actual, que es la razón por la que los permisos no viajan
 * dentro del token.
 */
@RestController
@RequestMapping("/api/seguridad")
public class ControladorMisPermisos {

    private final ServicioAutorizacion servicioAutorizacion;
    private final ServicioAsignaciones servicioAsignaciones;

    public ControladorMisPermisos(ServicioAutorizacion servicioAutorizacion,
                                  ServicioAsignaciones servicioAsignaciones) {
        this.servicioAutorizacion = servicioAutorizacion;
        this.servicioAsignaciones = servicioAsignaciones;
    }

    @GetMapping("/mis-permisos")
    public MisPermisosDto misPermisos() {
        UsuarioPrincipal usuario = UsuarioActual.obtener();

        List<String> roles = servicioAsignaciones.rolesDe(usuario.empresaId(), usuario.usuarioId()).stream()
                .filter(AsignacionDto::activo)
                .map(AsignacionDto::codigo)
                .toList();

        return new MisPermisosDto(
                usuario.usuarioId(),
                usuario.empresaId(),
                usuario.nombre(),
                servicioAutorizacion.permisosEfectivos(usuario.usuarioId()),
                roles);
    }
}
