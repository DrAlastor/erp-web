package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto.DecisionRequest;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto.PedidoResumenResponse;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.dto.ValidacionPedidoResponse;
import modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion.service.ValidacionComercialService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * CU17 - Validación Comercial y Aprobación. Consultar la bandeja y la evaluación de un pedido
 * pide COMERCIAL_CONSULTAR; aprobar o rechazar pide COMERCIAL_MODIFICAR.
 */
@RestController
@RequestMapping("/api/validacion-comercial")
@RequiredArgsConstructor
@Validated
public class ValidacionComercialController {

    private final ValidacionComercialService service;

    /** Bandeja de pedidos por estado (PENDIENTE por defecto, TODOS para ver el historial). */
    @GetMapping("/pedidos")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public ResponseEntity<Page<PedidoResumenResponse>> listar(
            @RequestParam(defaultValue = "PENDIENTE") String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(service.listar(estado, page, size));
    }

    /** Pedido con el resultado de cada regla comercial, calculado en este momento. */
    @GetMapping("/pedidos/{id}")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public ResponseEntity<ValidacionPedidoResponse> evaluar(@PathVariable Integer id) {
        return ResponseEntity.ok(service.evaluar(id));
    }

    @PostMapping("/pedidos/{id}/aprobar")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR')")
    public ResponseEntity<ValidacionPedidoResponse> aprobar(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) DecisionRequest request,
            Principal principal
    ) {
        String observacion = request != null ? request.getObservacion() : null;
        return ResponseEntity.ok(service.aprobar(id, observacion, actor(principal)));
    }

    @PostMapping("/pedidos/{id}/rechazar")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR')")
    public ResponseEntity<ValidacionPedidoResponse> rechazar(
            @PathVariable Integer id,
            @Valid @RequestBody DecisionRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(service.rechazar(id, request.getObservacion(), actor(principal)));
    }

    private static String actor(Principal principal) {
        return principal != null ? principal.getName() : "sistema";
    }
}
