package modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.TurnoResponse;
import modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.dto.ArqueoResponse;
import modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.dto.CerrarTurnoRequest;
import modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.dto.DetalleTurnoResponse;
import modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.service.CierreCajaService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * HU-35 / CU35: arqueo, cierre y consulta de turnos de caja (misma base /api/caja que la HU-34).
 * Permisos de caja mapeados a FACTURACION: CONSULTAR en lecturas, CREAR para cerrar.
 */
@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
@Validated
public class ArqueoCajaController {

    private final CierreCajaService service;

    @GetMapping("/turnos/{id}/arqueo")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public ArqueoResponse arqueo(@PathVariable Long id, Authentication authentication) {
        return service.arqueo(id, authentication);
    }

    @PostMapping("/turnos/{id}/cierre")
    @PreAuthorize("hasPermission('FACTURACION','CREAR')")
    public TurnoResponse cerrar(@PathVariable Long id, @Valid @RequestBody CerrarTurnoRequest body,
                                Authentication authentication) {
        return service.cerrar(id, body, authentication);
    }

    @GetMapping("/turnos")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public Page<TurnoResponse> historial(
            @RequestParam(required = false) Integer sucursalId,
            @RequestParam(defaultValue = "") @Size(max = 20) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            Authentication authentication) {
        return service.historial(sucursalId, estado, desde, hasta, page, size, authentication);
    }

    @GetMapping("/turnos/{id}")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public DetalleTurnoResponse detalle(@PathVariable Long id, Authentication authentication) {
        return service.detalle(id, authentication);
    }
}
