package modulo.compras_y_proveedores.cu21_gestionar_proveedores.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import modulo.compras_y_proveedores.compartido.service.Solicitante;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.DesactivarProveedorRequest;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.HistorialProveedorResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorOpcionResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorRequest;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.service.ProveedorService;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;

import org.springframework.data.domain.Page;
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
import java.util.List;

/**
 * CU21 - Gestionar Proveedores.
 *
 * <p>Consultar exige INVENTARIO_CONSULTAR (módulo "Inventarios y Compras"), así el
 * Almacenero ve los proveedores. Registrar, modificar, desactivar y reactivar quedan
 * para el Administrador: el Almacenero tiene todo INVENTARIO, por eso además del permiso
 * se exige el rol.
 */
@RestController
@RequestMapping({"/api/v1/proveedores", "/api/proveedores"})
@Validated
public class ProveedorController {

    static final String CONSULTAR =
            "hasPermission('INVENTARIO','CONSULTAR') or hasAnyRole('ADMIN','ADMINISTRADOR','SUPERADMIN')";
    static final String SOLO_ADMIN = "hasAnyRole('ADMIN','ADMINISTRADOR','SUPERADMIN')";

    private final ProveedorService service;

    public ProveedorController(ProveedorService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(CONSULTAR)
    public Page<ProveedorResponse> buscar(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "") @Size(max = 100) String ciudad,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.buscar(search, activo, ciudad, page, size);
    }

    /** Proveedores activos en forma corta, para los combos de órdenes de compra y compras. */
    @GetMapping("/activos")
    @PreAuthorize(CONSULTAR)
    public List<ProveedorOpcionResponse> activos() {
        return service.activos();
    }

    @GetMapping("/ciudades")
    @PreAuthorize(CONSULTAR)
    public List<String> ciudades() {
        return service.ciudades();
    }

    @GetMapping("/{id}")
    @PreAuthorize(CONSULTAR)
    public ProveedorResponse obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize(CONSULTAR)
    public HistorialProveedorResponse historial(@PathVariable Integer id) {
        return service.historial(id);
    }

    @PostMapping
    @PreAuthorize(SOLO_ADMIN)
    public ResponseEntity<ProveedorResponse> registrar(@Valid @RequestBody ProveedorRequest body,
                                                       Principal actor, HttpServletRequest http) {
        ProveedorResponse creado = service.registrar(body, solicitante(actor, http));
        return ResponseEntity.created(URI.create("/api/v1/proveedores/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize(SOLO_ADMIN)
    public ProveedorResponse modificar(@PathVariable Integer id, @Valid @RequestBody ProveedorRequest body,
                                       Principal actor, HttpServletRequest http) {
        return service.modificar(id, body, solicitante(actor, http));
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize(SOLO_ADMIN)
    public ProveedorResponse desactivar(@PathVariable Integer id, @Valid @RequestBody DesactivarProveedorRequest body,
                                        Principal actor, HttpServletRequest http) {
        return service.desactivar(id, body.motivo(), solicitante(actor, http));
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize(SOLO_ADMIN)
    public ProveedorResponse reactivar(@PathVariable Integer id, Principal actor, HttpServletRequest http) {
        return service.reactivar(id, solicitante(actor, http));
    }

    private static Solicitante solicitante(Principal actor, HttpServletRequest http) {
        UsuarioPrincipal usuario = UsuarioActual.obtener();
        String nombre = actor != null ? actor.getName() : usuario.email();
        return new Solicitante(usuario.usuarioId(), nombre, http.getRemoteAddr());
    }
}
