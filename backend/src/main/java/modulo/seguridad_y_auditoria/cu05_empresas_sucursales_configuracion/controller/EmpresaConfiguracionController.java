package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.dto.*;
import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.service.EmpresaConfiguracionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1", "/api"})
@RequiredArgsConstructor
public class EmpresaConfiguracionController {

    private static final String CONSULTAR =
            "hasPermission('SEGURIDAD','CONSULTAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')";
    private static final String MODIFICAR =
            "hasPermission('SEGURIDAD','MODIFICAR') or hasRole('ADMIN') or hasAuthority('ROLE_ADMIN')";

    private final EmpresaConfiguracionService service;

    private static UUID empresaActual() {
        return UsuarioActual.obtener().empresaId();
    }

    private static String actor(Principal principal) {
        return principal != null ? principal.getName() : null;
    }

    // ---------------------------------------------------------------- Empresa

    @GetMapping("/empresa")
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<EmpresaDTO> obtenerEmpresa() {
        return ResponseEntity.ok(service.obtenerEmpresa(empresaActual()));
    }

    @PutMapping("/empresa")
    @PreAuthorize(MODIFICAR)
    public ResponseEntity<EmpresaDTO> actualizarEmpresa(
            @Valid @RequestBody ActualizarEmpresaRequest request, Principal actor, HttpServletRequest http) {
        return ResponseEntity.ok(service.actualizarEmpresa(empresaActual(), request, actor(actor), http.getRemoteAddr()));
    }

    // ------------------------------------------------------------- Sucursales

    @GetMapping("/sucursales")
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<List<SucursalDTO>> listarSucursales(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.listarSucursales(activo, search));
    }

    @GetMapping("/sucursales/ciudades")
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<List<CiudadDTO>> listarCiudades() {
        return ResponseEntity.ok(service.listarCiudades());
    }

    @GetMapping("/sucursales/{id}")
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<SucursalDTO> obtenerSucursal(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtenerSucursal(id));
    }

    @PostMapping("/sucursales")
    @PreAuthorize(MODIFICAR)
    public ResponseEntity<SucursalDTO> crearSucursal(
            @Valid @RequestBody GuardarSucursalRequest request, Principal actor, HttpServletRequest http) {
        SucursalDTO creada = service.crearSucursal(request, actor(actor), http.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/v1/sucursales/" + creada.id())).body(creada);
    }

    @PutMapping("/sucursales/{id}")
    @PreAuthorize(MODIFICAR)
    public ResponseEntity<SucursalDTO> actualizarSucursal(
            @PathVariable Integer id, @Valid @RequestBody GuardarSucursalRequest request,
            Principal actor, HttpServletRequest http) {
        return ResponseEntity.ok(service.actualizarSucursal(id, request, actor(actor), http.getRemoteAddr()));
    }

    @PatchMapping("/sucursales/{id}/estado")
    @PreAuthorize(MODIFICAR)
    public ResponseEntity<SucursalDTO> cambiarEstadoSucursal(
            @PathVariable Integer id, @Valid @RequestBody CambiarEstadoSucursalRequest request,
            Principal actor, HttpServletRequest http) {
        return ResponseEntity.ok(
                service.cambiarEstadoSucursal(id, request.activo(), actor(actor), http.getRemoteAddr()));
    }

    // ---------------------------------------------------------- Configuracion

    @GetMapping("/configuracion")
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<List<ParametroDTO>> obtenerConfiguracion() {
        return ResponseEntity.ok(service.obtenerConfiguracion(empresaActual()));
    }

    @PutMapping("/configuracion")
    @PreAuthorize(MODIFICAR)
    public ResponseEntity<List<ParametroDTO>> guardarConfiguracion(
            @Valid @RequestBody GuardarConfiguracionRequest request, Principal actor, HttpServletRequest http) {
        return ResponseEntity.ok(
                service.guardarConfiguracion(empresaActual(), request, actor(actor), http.getRemoteAddr()));
    }
}
