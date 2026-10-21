package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.CompraSinCuentaResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.CuentaPorPagarResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.CuentaPorPagarResumenResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.FormaPagoOpcionResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.GenerarCuotasRequest;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.ProveedorOpcionResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.RegistrarPagoRequest;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto.ResumenCxpResponse;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.service.CuentaPorPagarService;
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

/**
 * HU-24 / CU34: gestionar cuentas por pagar a proveedores.
 *
 * <p>Es una obligacion financiera, asi que los permisos se piden al modulo CONTABILIDAD
 * del catalogo de la CU03: consultar para las lecturas y crear para registrar pagos y
 * generar cuotas. No existe borrado ni anulacion en esta CU.
 */
@RestController
@RequestMapping("/api/cuentas-por-pagar")
@RequiredArgsConstructor
@Validated
public class CuentaPorPagarController {

    private final CuentaPorPagarService service;

    @GetMapping
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public Page<CuentaPorPagarResumenResponse> list(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(defaultValue = "") @Size(max = 30) String estado,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.list(search, estado, proveedorId, page, size);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public ResumenCxpResponse resumen() {
        return service.resumen();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public CuentaPorPagarResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/{id}/pagos")
    @PreAuthorize("hasPermission('CONTABILIDAD','CREAR')")
    public ResponseEntity<CuentaPorPagarResponse> registrarPago(
            @PathVariable Long id, @Valid @RequestBody RegistrarPagoRequest body,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrarPago(id, body, authentication));
    }

    @PostMapping("/generar")
    @PreAuthorize("hasPermission('CONTABILIDAD','CREAR')")
    public ResponseEntity<List<CuentaPorPagarResumenResponse>> generar(
            @Valid @RequestBody GenerarCuotasRequest body, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.generar(body, authentication));
    }

    @GetMapping("/opciones/compras-sin-cuenta")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public List<CompraSinCuentaResponse> comprasSinCuenta(
            @RequestParam(defaultValue = "") @Size(max = 150) String search) {
        return service.comprasSinCuenta(search);
    }

    @GetMapping("/opciones/proveedores")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public List<ProveedorOpcionResponse> proveedores(
            @RequestParam(defaultValue = "") @Size(max = 150) String search) {
        return service.proveedores(search);
    }

    @GetMapping("/opciones/formas-pago")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public List<FormaPagoOpcionResponse> formasPago() {
        return service.formasPago();
    }
}
