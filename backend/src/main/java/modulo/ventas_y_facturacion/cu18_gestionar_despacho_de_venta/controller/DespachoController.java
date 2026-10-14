package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.AnularDespachoRequest;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DespachoRequest;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DespachoResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.DespachoResumenResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.FaltanteStock;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.PedidoPorDespacharResponse;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.entity.EstadoDespacho;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.exception.StockInsuficienteException;
import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.service.DespachoService;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CU18 - Gestionar Despacho de Venta.
 *
 * <p>Cada acción pide su permiso del módulo INVENTARIO al catálogo de la CU03, según la
 * matriz de roles: el Encargado de Almacén consulta, registra, confirma y entrega; anular
 * queda para quien tenga INVENTARIO_ANULAR, y el resto de los roles sólo consulta.
 */
@RestController
@RequestMapping("/api/despachos")
@Validated
public class DespachoController {

    private final DespachoService service;

    public DespachoController(DespachoService service) {
        this.service = service;
    }

    @GetMapping("/pedidos-pendientes")
    @PreAuthorize("hasPermission('INVENTARIO','CONSULTAR')")
    public List<PedidoPorDespacharResponse> pedidosPorDespachar(
            @RequestParam(required = false) Integer sucursalId) {
        return service.pedidosPorDespachar(sucursalId);
    }

    @GetMapping
    @PreAuthorize("hasPermission('INVENTARIO','CONSULTAR')")
    public Page<DespachoResumenResponse> listar(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(required = false) EstadoDespacho estado,
            @RequestParam(required = false) Integer pedidoId,
            @RequestParam(required = false) Integer sucursalId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.listar(search, estado, pedidoId, sucursalId, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('INVENTARIO','CONSULTAR')")
    public DespachoResponse detalle(@PathVariable Integer id) {
        return service.detalle(id);
    }

    @PostMapping
    @PreAuthorize("hasPermission('INVENTARIO','CREAR')")
    public ResponseEntity<DespachoResponse> registrar(@Valid @RequestBody DespachoRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrar(body, UsuarioActual.obtener().usuarioId()));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasPermission('INVENTARIO','MODIFICAR')")
    public DespachoResponse confirmar(@PathVariable Integer id) {
        return service.confirmar(id);
    }

    @PostMapping("/{id}/entregar")
    @PreAuthorize("hasPermission('INVENTARIO','MODIFICAR')")
    public DespachoResponse entregar(@PathVariable Integer id) {
        return service.marcarEntregado(id);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasPermission('INVENTARIO','ANULAR')")
    public DespachoResponse anular(@PathVariable Integer id,
                                   @Valid @RequestBody AnularDespachoRequest body) {
        return service.anular(id, body.motivo());
    }

    /** Respuesta del 409 por falta de stock: el error de siempre más la lista de faltantes. */
    public record StockInsuficienteResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            List<FaltanteStock> faltantes) {
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<StockInsuficienteResponse> stockInsuficiente(
            StockInsuficienteException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new StockInsuficienteResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "STOCK_INSUFICIENTE",
                ex.getMessage(),
                request.getRequestURI(),
                ex.getFaltantes()));
    }
}
