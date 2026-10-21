package modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.dto.ComprasDtos.*;
import modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.service.CompraService;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/compras", "/api/compras"})
@RequiredArgsConstructor
public class CompraController {

    private static final String CONSULTAR =
            "hasPermission('INVENTARIO','CONSULTAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')";
    private static final String CREAR =
            "hasPermission('INVENTARIO','CREAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')";
    private static final String MODIFICAR =
            "hasPermission('INVENTARIO','MODIFICAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')";

    private final CompraService service;

    private static String actor(Principal p) {
        return p != null ? p.getName() : null;
    }

    @GetMapping("/catalogos")
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<CatalogosDTO> catalogos() {
        return ResponseEntity.ok(service.catalogos());
    }

    @GetMapping
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<List<CompraDTO>> listar(
            @RequestParam(required = false) Integer proveedorId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.listarCompras(proveedorId, estado, desde, hasta, search));
    }

    @GetMapping("/{id}")
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<CompraDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtenerCompra(id));
    }

    @PostMapping
    @PreAuthorize(CREAR)
    public ResponseEntity<CompraDTO> registrar(
            @Valid @RequestBody RegistrarCompraRequest request, Principal actor, HttpServletRequest http) {
        UsuarioPrincipal usuario = UsuarioActual.obtener();
        CompraDTO creada = service.registrarCompra(request, usuario.empresaId(), usuario.usuarioId(),
                actor(actor), http.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/v1/compras/" + creada.id())).body(creada);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize(MODIFICAR)
    public ResponseEntity<CompraDTO> anular(
            @PathVariable Integer id, @Valid @RequestBody AnularCompraRequest request,
            Principal actor, HttpServletRequest http) {
        return ResponseEntity.ok(service.anularCompra(id, request, actor(actor), http.getRemoteAddr()));
    }

    @GetMapping("/devoluciones")
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<List<DevolucionDTO>> listarDevoluciones(@RequestParam(required = false) Integer compraId) {
        return ResponseEntity.ok(service.listarDevoluciones(compraId));
    }

    @PostMapping("/{id}/devoluciones")
    @PreAuthorize(CREAR)
    public ResponseEntity<DevolucionDTO> registrarDevolucion(
            @PathVariable Integer id, @Valid @RequestBody RegistrarDevolucionRequest request,
            Principal actor, HttpServletRequest http) {
        UsuarioPrincipal usuario = UsuarioActual.obtener();
        DevolucionDTO creada = service.registrarDevolucion(id, request, usuario.usuarioId(),
                actor(actor), http.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/v1/compras/devoluciones?compraId=" + id)).body(creada);
    }
}
