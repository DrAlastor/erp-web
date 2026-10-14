package modulo.ventas_y_facturacion.cu16_pedidos_de_venta.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.ClienteOpcionResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoAnularRequest;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoRequest;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.PedidoResumenResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.ProductoOpcionResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.dto.SucursalOpcionResponse;
import modulo.ventas_y_facturacion.cu16_pedidos_de_venta.service.PedidoVentaService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU-16 / CU16: gestionar pedidos de venta.
 *
 * <p>Los permisos se piden al catalogo de la CU03 dentro del modulo COMERCIAL: consultar,
 * crear, modificar y anular. Aprobar el pedido corresponde a la CU17. No existe borrado fisico.
 */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Validated
public class PedidoVentaController {

    private final PedidoVentaService service;

    @GetMapping
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public Page<PedidoResumenResponse> list(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(defaultValue = "") @Size(max = 30) String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.list(search, estado, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public PedidoResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    @PreAuthorize("hasPermission('COMERCIAL','CREAR')")
    public ResponseEntity<PedidoResponse> create(
            @Valid @RequestBody PedidoRequest body, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR')")
    public PedidoResponse update(@PathVariable Long id, @Valid @RequestBody PedidoRequest body) {
        return service.update(id, body);
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasPermission('COMERCIAL','ANULAR')")
    public PedidoResponse anular(
            @PathVariable Long id, @Valid @RequestBody PedidoAnularRequest body) {
        return service.anular(id, body.motivo());
    }

    @GetMapping("/opciones/clientes")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public List<ClienteOpcionResponse> clientes(
            @RequestParam(defaultValue = "") @Size(max = 150) String search) {
        return service.clientes(search);
    }

    @GetMapping("/opciones/sucursales")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public List<SucursalOpcionResponse> sucursales() {
        return service.sucursales();
    }

    @GetMapping("/opciones/productos")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public List<ProductoOpcionResponse> productos(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(required = false) Long sucursalId) {
        return service.productos(search, sucursalId);
    }
}
