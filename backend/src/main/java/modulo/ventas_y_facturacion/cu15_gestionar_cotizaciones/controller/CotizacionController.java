package modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.AnularCotizacionRequest;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.ConvertirCotizacionRequest;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.CotizacionRequest;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.CotizacionResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.CotizacionResumenResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.OpcionesCotizacionResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.dto.PedidoGeneradoResponse;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.entity.EstadoCotizacion;
import modulo.ventas_y_facturacion.cu15_gestionar_cotizaciones.service.CotizacionService;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

/**
 * CU15 - Gestionar Cotizaciones.
 *
 * <p>Cada acción pide su permiso del módulo COMERCIAL al catálogo de la CU03, según la
 * matriz de roles: el Cajero y el Preventista registran, consultan y convierten; modificar
 * y anular quedan para quien tenga COMERCIAL_MODIFICAR y COMERCIAL_ANULAR.
 */
@RestController
@RequestMapping("/api/cotizaciones")
@Validated
public class CotizacionController {

    private final CotizacionService service;

    public CotizacionController(CotizacionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public Page<CotizacionResumenResponse> listar(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(required = false) EstadoCotizacion estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.listar(search, estado, clienteId, page, size);
    }

    @GetMapping("/opciones")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public OpcionesCotizacionResponse opciones() {
        return service.opciones();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public CotizacionResponse detalle(@PathVariable Integer id) {
        return service.detalle(id);
    }

    @PostMapping
    @PreAuthorize("hasPermission('COMERCIAL','CREAR')")
    public ResponseEntity<CotizacionResponse> registrar(@Valid @RequestBody CotizacionRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrar(body, UsuarioActual.obtener().usuarioId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR')")
    public CotizacionResponse modificar(@PathVariable Integer id,
                                        @Valid @RequestBody CotizacionRequest body) {
        return service.modificar(id, body);
    }

    /** Emite un borrador: es la misma acción que registrar una cotización ya emitida. */
    @PostMapping("/{id}/emitir")
    @PreAuthorize("hasPermission('COMERCIAL','CREAR')")
    public CotizacionResponse emitir(@PathVariable Integer id) {
        return service.emitir(id);
    }

    @PostMapping("/{id}/aceptar")
    @PreAuthorize("hasPermission('COMERCIAL','CREAR')")
    public CotizacionResponse aceptar(@PathVariable Integer id) {
        return service.aceptar(id);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasPermission('COMERCIAL','ANULAR')")
    public CotizacionResponse anular(@PathVariable Integer id,
                                     @Valid @RequestBody AnularCotizacionRequest body) {
        return service.anular(id, body.motivo());
    }

    @PostMapping("/{id}/convertir")
    @PreAuthorize("hasPermission('COMERCIAL','CREAR')")
    public ResponseEntity<PedidoGeneradoResponse> convertir(
            @PathVariable Integer id, @Valid @RequestBody ConvertirCotizacionRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                service.convertirEnPedido(id, body.sucursalId(), UsuarioActual.obtener().usuarioId()));
    }
}
