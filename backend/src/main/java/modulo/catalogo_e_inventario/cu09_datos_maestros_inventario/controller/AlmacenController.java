package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.ActualizarAlmacenRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.AlmacenDTO;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CambiarEstadoRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CrearAlmacenRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.service.AlmacenService;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/almacenes", "/api/almacenes"})
@RequiredArgsConstructor
public class AlmacenController {

    private final AlmacenService almacenService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AlmacenDTO>> listar() {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        return ResponseEntity.ok(almacenService.listarAlmacenes(empresaId));
    }

    @GetMapping("/ciudades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<java.util.Map<String, Object>>> listarCiudades() {
        return ResponseEntity.ok(almacenService.listarCiudades());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlmacenDTO> obtenerPorId(@PathVariable Integer id) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        return ResponseEntity.ok(almacenService.obtenerAlmacen(id, empresaId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlmacenDTO> crear(
            @Valid @RequestBody CrearAlmacenRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        String actorNombre = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();

        AlmacenDTO creado = almacenService.crearAlmacen(request, empresaId, actorNombre, ip);
        return ResponseEntity.created(URI.create("/api/v1/almacenes/" + creado.getId())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlmacenDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarAlmacenRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        String actorNombre = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();

        AlmacenDTO actualizado = almacenService.actualizarAlmacen(id, request, empresaId, actorNombre, ip);
        return ResponseEntity.ok(actualizado);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlmacenDTO> cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody CambiarEstadoRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        String actorNombre = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();

        AlmacenDTO actualizado = almacenService.cambiarEstado(id, request.getActivo(), empresaId, actorNombre, ip);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> eliminar(
            @PathVariable Integer id,
            Principal actor,
            HttpServletRequest servletRequest) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        String actorNombre = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();

        almacenService.eliminarAlmacen(id, empresaId, actorNombre, ip);
        return ResponseEntity.noContent().build();
    }
}
