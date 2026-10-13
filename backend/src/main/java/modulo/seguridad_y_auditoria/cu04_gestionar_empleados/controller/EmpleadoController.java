package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.dto.*;
import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.service.EmpleadoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/v1/empleados", "/api/empleados"})
@RequiredArgsConstructor
@Validated
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    /**
     * Listado de empleados con filtros por sucursal, cargo, estado activo y búsqueda libre.
     */
    @GetMapping
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<EmpleadoDTO>> listar(
            @RequestParam(required = false) Integer sucursalId,
            @RequestParam(required = false) String cargo,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String search) {
        List<EmpleadoDTO> empleados = empleadoService.listarEmpleados(sucursalId, cargo, activo, search);
        return ResponseEntity.ok(empleados);
    }

    /**
     * Sucursales activas para poblar los selectores del formulario de registro y filtros.
     */
    @GetMapping("/sucursales")
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<SucursalItemDTO>> listarSucursales() {
        return ResponseEntity.ok(empleadoService.listarSucursalesActivas());
    }

    /**
     * Empleados activos que aún no tienen cuenta de usuario asignada.
     */
    @GetMapping("/disponibles")
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<EmpleadoDTO>> listarDisponibles() {
        return ResponseEntity.ok(empleadoService.listarDisponiblesParaUsuario());
    }

    /**
     * Detalle de un empleado por ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmpleadoDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(empleadoService.obtenerPorId(id));
    }

    /**
     * Registrar nuevo empleado en el maestro de personal.
     */
    @PostMapping
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmpleadoDTO> crear(
            @Valid @RequestBody CrearEmpleadoRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        String actorEmail = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();
        EmpleadoDTO creado = empleadoService.crearEmpleado(request, actorEmail, ip);
        return ResponseEntity.created(URI.create("/api/v1/empleados/" + creado.getId())).body(creado);
    }

    /**
     * Editar información de contacto, cargo y sucursal.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmpleadoDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEmpleadoRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        String actorEmail = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();
        EmpleadoDTO actualizado = empleadoService.actualizarEmpleado(id, request, actorEmail, ip);
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Cambiar estado / Baja lógica de un empleado.
     * Si se desactiva, inhabilita automáticamente su usuario asociado en cascada.
     */
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmpleadoDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoEmpleadoRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        String actorEmail = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();
        EmpleadoDTO actualizado = empleadoService.cambiarEstado(id, request, actorEmail, ip);
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Eliminar empleado o aplicar baja lógica si posee referencias o usuario vinculado.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('SEGURIDAD','ELIMINAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            Principal actor,
            HttpServletRequest servletRequest) {
        String actorEmail = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();
        empleadoService.eliminarEmpleado(id, actorEmail, ip);
        return ResponseEntity.noContent().build();
    }
}
