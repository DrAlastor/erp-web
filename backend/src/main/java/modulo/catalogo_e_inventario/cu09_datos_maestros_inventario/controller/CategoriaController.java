package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.ActualizarCategoriaRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CambiarEstadoRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CategoriaDTO;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CategoriaNodoDTO;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto.CrearCategoriaRequest;
import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.service.CategoriaService;
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
@RequestMapping({"/api/v1/categorias", "/api/categorias"})
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CategoriaDTO>> listar() {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        return ResponseEntity.ok(categoriaService.listarCategorias(empresaId));
    }

    /**
     * Endpoint optimizado para entregar el árbol anidado JSON en una sola petición.
     */
    @GetMapping("/arbol")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CategoriaNodoDTO>> obtenerArbol() {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        return ResponseEntity.ok(categoriaService.obtenerArbol(empresaId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoriaDTO> obtenerPorId(@PathVariable Integer id) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        return ResponseEntity.ok(categoriaService.obtenerCategoria(id, empresaId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoriaDTO> crear(
            @Valid @RequestBody CrearCategoriaRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        String actorNombre = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();

        CategoriaDTO creada = categoriaService.crearCategoria(request, empresaId, actorNombre, ip);
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + creada.getId())).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoriaDTO> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarCategoriaRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        String actorNombre = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();

        CategoriaDTO actualizada = categoriaService.actualizarCategoria(id, request, empresaId, actorNombre, ip);
        return ResponseEntity.ok(actualizada);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CategoriaDTO> cambiarEstado(
            @PathVariable Integer id,
            @Valid @RequestBody CambiarEstadoRequest request,
            Principal actor,
            HttpServletRequest servletRequest) {
        UUID empresaId = UsuarioActual.buscar().map(UsuarioPrincipal::empresaId).orElse(null);
        String actorNombre = actor != null ? actor.getName() : "admin@asustech.bo";
        String ip = servletRequest.getRemoteAddr();

        CategoriaDTO actualizada = categoriaService.cambiarEstado(id, request.getActivo(), empresaId, actorNombre, ip);
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

        categoriaService.eliminarCategoria(id, empresaId, actorNombre, ip);
        return ResponseEntity.noContent().build();
    }
}
