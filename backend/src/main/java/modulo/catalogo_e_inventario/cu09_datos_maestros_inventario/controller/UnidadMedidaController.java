package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.ActualizarUnidadMedidaRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CambiarEstadoRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CrearUnidadMedidaRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.UnidadMedidaDTO;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.service.UnidadMedidaService;
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
@RequestMapping({"/api/v1/unidades-medida", "/api/unidades-medida"})
@RequiredArgsConstructor
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UnidadMedidaDTO>> listar() {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        return ResponseEntity.ok(unidadMedidaService.listarUnidades(empresaId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UnidadMedidaDTO> obtenerPorId(@PathVariable Integer id) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        return ResponseEntity.ok(unidadMedidaService.obtenerUnidad(id, empresaId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UnidadMedidaDTO> crear(
            @Valid @RequestBody CrearUnidadMedidaRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        String actorNombre = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();

        UnidadMedidaDTO creada = unidadMedidaService.crearUnidad(request, empresaId, actorNombre, ip);
        return ResponseEntity.created(URI.create("/api/v1/unidades-medida/" + creada.getId())).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UnidadMedidaDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarUnidadMedidaRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        String actorNombre = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();

        UnidadMedidaDTO actualizada = unidadMedidaService.actualizarUnidad(id, request, empresaId, actorNombre, ip);
        return ResponseEntity.ok(actualizada);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UnidadMedidaDTO> cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody CambiarEstadoRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        String actorNombre = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();

        UnidadMedidaDTO actualizada = unidadMedidaService.cambiarEstado(id, request.getActivo(), empresaId, actorNombre, ip);
        return ResponseEntity.ok(actualizada);
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

        unidadMedidaService.eliminarUnidad(id, empresaId, actorNombre, ip);
        return ResponseEntity.noContent().build();
    }
}
