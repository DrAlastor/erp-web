package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.CuentaPorCobrarResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.CuentaPorCobrarResumenResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.EstadoCuentaClienteResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.OpcionResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.RegistrarCobroRequest;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.RegistrarCobroResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto.ResumenCxcResponse;
import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.service.CuentaPorCobrarService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import java.util.List;
import java.util.Map;

/**
 * HU-29 / CU29: gestionar cuentas por cobrar y cobros.
 *
 * <p>Permisos del módulo CONTABILIDAD (catálogo de la CU03): consultar para cartera,
 * detalle y estado de cuenta; modificar para registrar un cobro, porque el cobro cambia
 * el saldo de la cuota (el Cajero tiene CONTABILIDAD_MODIFICAR en la matriz).
 */
@RestController
@RequestMapping("/api/cuentas-por-cobrar")
@RequiredArgsConstructor
@Validated
public class CuentaPorCobrarController {

    private final CuentaPorCobrarService service;

    @GetMapping
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public Page<CuentaPorCobrarResumenResponse> list(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(defaultValue = "") @Size(max = 30) String estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.list(search, estado, clienteId, page, size);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public ResumenCxcResponse resumen() {
        return service.resumen();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public CuentaPorCobrarResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/{id}/cobros")
    @PreAuthorize("hasPermission('CONTABILIDAD','MODIFICAR')")
    public ResponseEntity<RegistrarCobroResponse> registrarCobro(
            @PathVariable Long id, @Valid @RequestBody RegistrarCobroRequest body, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarCobro(id, body, authentication));
    }

    @GetMapping("/clientes/{clienteId}/estado-cuenta")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public EstadoCuentaClienteResponse estadoCuenta(@PathVariable Long clienteId) {
        return service.estadoCuenta(clienteId);
    }

    @GetMapping("/opciones/clientes")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public List<OpcionResponse> clientes(@RequestParam(defaultValue = "") @Size(max = 150) String search) {
        return service.clientes(search);
    }

    @GetMapping("/opciones/permisos")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public Map<String, Boolean> permisos(Authentication authentication) {
        return service.permisos(authentication);
    }

    @GetMapping("/opciones/formas-pago")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public List<OpcionResponse> formasPago() {
        return service.formasPago();
    }
}
