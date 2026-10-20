package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.AnularVentaRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.CatalogosVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.ClienteVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.CobroStripeRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.CobroStripeResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.ProductoVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.VentaResumenResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.service.VentaService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CU19 (HU-19): registrar venta con emisión de factura o nota de venta, y anulación.
 *
 * <p>Se protege con el módulo FACTURACION del catálogo de la CU03: el cajero puede crear,
 * consultar y anular facturas (C L A), el resto de roles solo consultarlas.
 */
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Validated
public class VentaController {

    private final VentaService service;

    @GetMapping
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public Page<VentaResumenResponse> listar(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String comprobante,
            @RequestParam(required = false) Integer sucursalId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.listar(search, estado, comprobante, sucursalId, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public VentaResponse detalle(@PathVariable Long id) {
        return service.detalle(id);
    }

    @GetMapping("/catalogos")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public CatalogosVentaResponse catalogos() {
        return service.catalogos();
    }

    @GetMapping("/productos")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public List<ProductoVentaResponse> productos(
            @RequestParam Integer sucursalId,
            @RequestParam(defaultValue = "") @Size(max = 150) String search) {
        return service.productos(sucursalId, search);
    }

    @GetMapping("/clientes")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public List<ClienteVentaResponse> clientes(@RequestParam(defaultValue = "") @Size(max = 150) String search) {
        return service.clientes(search);
    }

    @PostMapping
    @PreAuthorize("hasPermission('FACTURACION','CREAR')")
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest body) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(body));
        } catch (RuntimeException ex) {
            // La emisión ya se revirtió; si el cliente pagó con tarjeta, se le devuelve el dinero.
            if (!service.reembolsarCobroNoFacturado(body)) throw ex;
            String aviso = " El cobro con tarjeta fue reembolsado en Stripe.";
            if (ex instanceof ResponseStatusException rechazo) {
                throw new ResponseStatusException(rechazo.getStatusCode(), rechazo.getReason() + aviso, ex);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo emitir la factura." + aviso, ex);
        }
    }

    /** Valida la venta y abre el cobro con tarjeta en Stripe Checkout; aún no emite la factura. */
    @PostMapping("/stripe/checkout")
    @PreAuthorize("hasPermission('FACTURACION','CREAR')")
    public CobroStripeResponse iniciarCobroStripe(@Valid @RequestBody CobroStripeRequest body) {
        return service.prepararCobroStripe(body);
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasPermission('FACTURACION','ANULAR')")
    public VentaResponse anular(@PathVariable Long id, @Valid @RequestBody AnularVentaRequest body) {
        return service.anular(id, body.motivo());
    }
}
