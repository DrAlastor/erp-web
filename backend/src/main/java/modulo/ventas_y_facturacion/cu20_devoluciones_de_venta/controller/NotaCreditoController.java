package modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.NotaCreditoRequest;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.NotaCreditoResponse;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.NotaCreditoResumenResponse;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.dto.VentaDevolucionResponse;
import modulo.ventas_y_facturacion.cu20_devoluciones_de_venta.service.NotaCreditoService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU-20: devoluciones de venta y notas de crédito, protegidas con el módulo FACTURACION
 * del catálogo de la CU03.
 */
@RestController
@RequiredArgsConstructor
@Validated
public class NotaCreditoController {

    private final NotaCreditoService service;

    /** Ventas vigentes por número de factura o NIT, con las cantidades aún devolvibles. */
    @GetMapping("/api/ventas/buscar")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public List<VentaDevolucionResponse> buscarVentas(
            @RequestParam(required = false) @Min(1) Integer numeroFactura,
            @RequestParam(required = false) @Size(max = 30) String nitCliente) {
        return service.buscarVentas(numeroFactura, nitCliente);
    }

    @GetMapping("/api/notas-credito")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public Page<NotaCreditoResumenResponse> listar(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.listar(search, page, size);
    }

    @GetMapping("/api/notas-credito/{id}")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public NotaCreditoResponse detalle(@PathVariable Long id) {
        return service.detalle(id);
    }

    @PostMapping("/api/notas-credito")
    @PreAuthorize("hasPermission('FACTURACION','CREAR')")
    public ResponseEntity<NotaCreditoResponse> emitir(@Valid @RequestBody NotaCreditoRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.emitir(body));
    }
}
