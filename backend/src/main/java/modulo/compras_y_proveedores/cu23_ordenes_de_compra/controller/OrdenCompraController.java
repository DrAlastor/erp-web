package modulo.compras_y_proveedores.cu23_ordenes_de_compra.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import modulo.compras_y_proveedores.compartido.service.Solicitante;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.MotivoRequest;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OpcionesOrdenCompraResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OrdenCompraRequest;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OrdenCompraResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.OrdenCompraResumenResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.dto.SugerenciaReposicionResponse;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity.EstadoOrdenCompra;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.service.OrdenCompraService;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Gestionar Órdenes de Compra (CU22 del documento del Sprint Especial).
 *
 * <p>El Almacenero registra, modifica y consulta con los permisos INVENTARIO de la matriz.
 * Aprobar, rechazar, enviar y anular son del Administrador: como el Almacenero tiene todo
 * INVENTARIO (incluido ANULAR), esas acciones exigen el rol y no sólo el permiso.
 */
@RestController
@RequestMapping({"/api/v1/ordenes-compra", "/api/ordenes-compra"})
@Validated
public class OrdenCompraController {

    private static final String ADMIN = "hasAnyRole('ADMIN','ADMINISTRADOR','SUPERADMIN')";
    private static final String CONSULTAR = "hasPermission('INVENTARIO','CONSULTAR') or " + ADMIN;
    private static final String CREAR = "hasPermission('INVENTARIO','CREAR') or " + ADMIN;
    private static final String MODIFICAR = "hasPermission('INVENTARIO','MODIFICAR') or " + ADMIN;

    private final OrdenCompraService service;

    public OrdenCompraController(OrdenCompraService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(CONSULTAR)
    public Page<OrdenCompraResumenResponse> listar(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(required = false) EstadoOrdenCompra estado,
            @RequestParam(required = false) Integer proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.listar(search, estado, proveedorId, desde, hasta, page, size);
    }

    @GetMapping("/opciones")
    @PreAuthorize(CONSULTAR)
    public OpcionesOrdenCompraResponse opciones() {
        return service.opciones();
    }

    @GetMapping("/sugerencias")
    @PreAuthorize(CONSULTAR)
    public List<SugerenciaReposicionResponse> sugerencias(@RequestParam Integer sucursalId) {
        return service.sugerencias(sucursalId);
    }

    @GetMapping("/{id}")
    @PreAuthorize(CONSULTAR)
    public OrdenCompraResponse detalle(@PathVariable Integer id) {
        return service.detalle(id, UsuarioActual.obtener().usuarioId());
    }

    @PostMapping
    @PreAuthorize(CREAR)
    public ResponseEntity<OrdenCompraResponse> registrar(@Valid @RequestBody OrdenCompraRequest body,
                                                         Principal actor, HttpServletRequest http) {
        OrdenCompraResponse creada = service.registrar(body, solicitante(actor, http));
        return ResponseEntity.created(URI.create("/api/v1/ordenes-compra/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize(MODIFICAR)
    public OrdenCompraResponse modificar(@PathVariable Integer id, @Valid @RequestBody OrdenCompraRequest body,
                                         Principal actor, HttpServletRequest http) {
        return service.modificar(id, body, solicitante(actor, http));
    }

    @PostMapping("/{id}/solicitar-aprobacion")
    @PreAuthorize(CREAR)
    public OrdenCompraResponse solicitarAprobacion(@PathVariable Integer id, Principal actor, HttpServletRequest http) {
        return service.solicitarAprobacion(id, solicitante(actor, http));
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize(ADMIN)
    public OrdenCompraResponse aprobar(@PathVariable Integer id, Principal actor, HttpServletRequest http) {
        return service.aprobar(id, solicitante(actor, http));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize(ADMIN)
    public OrdenCompraResponse rechazar(@PathVariable Integer id, @Valid @RequestBody MotivoRequest body,
                                        Principal actor, HttpServletRequest http) {
        return service.rechazar(id, body.motivo(), solicitante(actor, http));
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize(ADMIN)
    public OrdenCompraResponse enviar(@PathVariable Integer id, Principal actor, HttpServletRequest http) {
        return service.enviar(id, solicitante(actor, http));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize(ADMIN)
    public OrdenCompraResponse anular(@PathVariable Integer id, @Valid @RequestBody MotivoRequest body,
                                      Principal actor, HttpServletRequest http) {
        return service.anular(id, body.motivo(), solicitante(actor, http));
    }

    private static Solicitante solicitante(Principal actor, HttpServletRequest http) {
        UsuarioPrincipal usuario = UsuarioActual.obtener();
        String nombre = actor != null ? actor.getName() : usuario.email();
        return new Solicitante(usuario.usuarioId(), nombre, http.getRemoteAddr());
    }
}
