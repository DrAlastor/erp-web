package modulo.contabilidad_y_finanzas.cu26_periodos_contables.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import modulo.contabilidad_y_finanzas.compartido.PermisoFuncionContable;
import modulo.contabilidad_y_finanzas.compartido.Solicitante;
import modulo.contabilidad_y_finanzas.compartido.SolicitanteActual;
import modulo.contabilidad_y_finanzas.cu26_periodos_contables.dto.GenerarGestionRequest;
import modulo.contabilidad_y_finanzas.cu26_periodos_contables.dto.GestionResponse;
import modulo.contabilidad_y_finanzas.cu26_periodos_contables.dto.PeriodoResponse;
import modulo.contabilidad_y_finanzas.cu26_periodos_contables.service.PeriodosContablesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/** CU26 - Gestionar Periodos Contables. */
@RestController
@RequestMapping({"/api/v1/periodos-contables", "/api/periodos-contables"})
@Validated
public class PeriodosContablesController {

    static final String CONSULTAR = "hasPermission('CONTABILIDAD','CONSULTAR')";
    static final String CREAR = "hasPermission('CONTABILIDAD','CREAR')";
    static final String MODIFICAR = "hasPermission('CONTABILIDAD','MODIFICAR')";

    private final PeriodosContablesService service;
    private final PermisoFuncionContable permisos;

    public PeriodosContablesController(PeriodosContablesService service, PermisoFuncionContable permisos) {
        this.service = service;
        this.permisos = permisos;
    }

    /**
     * CONTABILIDAD_CREAR/MODIFICAR se concede por cualquier función de Contabilidad con Edición
     * (p. ej. el Cajero con «Cuentas por Cobrar y Cobros»): se exige además Edición en esta función.
     */
    private Solicitante editor(Principal actor, HttpServletRequest http) {
        Solicitante quien = SolicitanteActual.de(actor, http);
        permisos.exigirEdicion(quien.usuarioId(), PermisoFuncionContable.PERIODOS);
        return quien;
    }

    /** Periodos de una gestión (año) con su resumen. */
    @GetMapping
    @PreAuthorize(CONSULTAR)
    public GestionResponse gestion(@RequestParam @Min(2000) @Max(2100) int gestion) {
        return service.gestion(gestion);
    }

    /** Gestiones que tienen al menos un periodo, de la más reciente a la más antigua. */
    @GetMapping("/gestiones")
    @PreAuthorize(CONSULTAR)
    public List<Integer> gestiones() {
        return service.gestiones();
    }

    @GetMapping("/{id}")
    @PreAuthorize(CONSULTAR)
    public PeriodoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/gestiones")
    @PreAuthorize(CREAR)
    public GestionResponse generar(@Valid @RequestBody GenerarGestionRequest body,
                                   Principal actor, HttpServletRequest http) {
        return service.generarGestion(body.gestion(), editor(actor, http));
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize(MODIFICAR)
    public PeriodoResponse cerrar(@PathVariable Long id, Principal actor, HttpServletRequest http) {
        return service.cerrar(id, editor(actor, http));
    }

    @PatchMapping("/{id}/reabrir")
    @PreAuthorize(MODIFICAR)
    public PeriodoResponse reabrir(@PathVariable Long id, Principal actor, HttpServletRequest http) {
        return service.reabrir(id, editor(actor, http));
    }
}
