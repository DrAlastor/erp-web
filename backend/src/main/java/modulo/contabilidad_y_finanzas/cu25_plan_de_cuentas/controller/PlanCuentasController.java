package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import modulo.contabilidad_y_finanzas.compartido.SolicitanteActual;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaActualizarRequest;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaNodoResponse;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaRequest;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaResponse;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.service.PlanCuentasService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

/** CU25 - Gestionar Plan de Cuentas. */
@RestController
@RequestMapping({"/api/v1/plan-cuentas", "/api/plan-cuentas"})
@Validated
public class PlanCuentasController {

    static final String CONSULTAR = "hasPermission('CONTABILIDAD','CONSULTAR')";
    static final String CREAR = "hasPermission('CONTABILIDAD','CREAR')";
    static final String MODIFICAR = "hasPermission('CONTABILIDAD','MODIFICAR')";

    private final PlanCuentasService service;

    public PlanCuentasController(PlanCuentasService service) {
        this.service = service;
    }

    @GetMapping("/arbol")
    @PreAuthorize(CONSULTAR)
    public List<CuentaNodoResponse> arbol() {
        return service.arbol();
    }

    @GetMapping
    @PreAuthorize(CONSULTAR)
    public Page<CuentaResponse> buscar(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(defaultValue = "") @Size(max = 30) String tipo,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "false") boolean soloMovimiento,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.buscar(search, tipo, activo, soloMovimiento, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(CONSULTAR)
    public CuentaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize(CREAR)
    public ResponseEntity<CuentaResponse> crear(@Valid @RequestBody CuentaRequest body,
                                                Principal actor, HttpServletRequest http) {
        CuentaResponse creada = service.crear(body, SolicitanteActual.de(actor, http));
        return ResponseEntity.created(URI.create("/api/v1/plan-cuentas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize(MODIFICAR)
    public CuentaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CuentaActualizarRequest body,
                                     Principal actor, HttpServletRequest http) {
        return service.actualizar(id, body, SolicitanteActual.de(actor, http));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize(MODIFICAR)
    public CuentaResponse desactivar(@PathVariable Long id, Principal actor, HttpServletRequest http) {
        return service.desactivar(id, SolicitanteActual.de(actor, http));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize(MODIFICAR)
    public CuentaResponse reactivar(@PathVariable Long id, Principal actor, HttpServletRequest http) {
        return service.reactivar(id, SolicitanteActual.de(actor, http));
    }
}
