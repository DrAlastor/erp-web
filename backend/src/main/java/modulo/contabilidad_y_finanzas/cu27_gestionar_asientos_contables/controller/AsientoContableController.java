package modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AnularAsientoRequest;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AsientoRequest;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AsientoResponse;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AsientoResumenResponse;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.CuentaOpcionResponse;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.repository.AsientoConsultaRepository;
import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.service.AsientoContableService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * HU-27 / CU27: gestionar asientos contables.
 *
 * <p>Permisos del módulo CONTABILIDAD (catálogo de la CU03): consultar para el Libro
 * Diario, crear para registrar, modificar para corregir y contabilizar un borrador, y
 * anular (solo el Administrador en la matriz) para anular o revertir. Para escribir, el
 * servicio exige además Edición en la función «Gestionar Asientos Contables».
 */
@RestController
@RequestMapping("/api/asientos-contables")
@RequiredArgsConstructor
@Validated
public class AsientoContableController {

    private final AsientoContableService service;

    @GetMapping
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public Page<AsientoResumenResponse> list(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(defaultValue = "") @Size(max = 30) String tipo,
            @RequestParam(defaultValue = "") @Size(max = 20) String estado,
            @RequestParam(defaultValue = "") @Size(max = 20) String origen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.list(new AsientoConsultaRepository.Filtro(search, tipo, estado, origen, desde, hasta), page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public AsientoResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    @PreAuthorize("hasPermission('CONTABILIDAD','CREAR')")
    public ResponseEntity<AsientoResponse> registrar(
            @Valid @RequestBody AsientoRequest body, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(body, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('CONTABILIDAD','MODIFICAR')")
    public AsientoResponse modificar(
            @PathVariable Long id, @Valid @RequestBody AsientoRequest body, Authentication authentication) {
        return service.modificar(id, body, authentication);
    }

    @PostMapping("/{id}/contabilizar")
    @PreAuthorize("hasPermission('CONTABILIDAD','MODIFICAR')")
    public AsientoResponse contabilizar(@PathVariable Long id, Authentication authentication) {
        return service.contabilizar(id, authentication);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasPermission('CONTABILIDAD','ANULAR')")
    public AsientoResponse anular(
            @PathVariable Long id, @Valid @RequestBody AnularAsientoRequest body, Authentication authentication) {
        return service.anular(id, body, authentication);
    }

    @GetMapping("/opciones/cuentas")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public List<CuentaOpcionResponse> cuentas(@RequestParam(defaultValue = "") @Size(max = 150) String search) {
        return service.cuentas(search);
    }

    @GetMapping("/opciones/permisos")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public Map<String, Boolean> permisos(Authentication authentication) {
        return service.permisos(authentication);
    }

    @GetMapping("/opciones/tipos")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public List<String> tipos() {
        return service.tipos();
    }
}
