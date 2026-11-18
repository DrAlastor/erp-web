package modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.dto.VentaPosRequest;
import modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.dto.VentaPosResponse;
import modulo.tesoreria_y_cajas.cu33_venta_en_caja_pos.service.VentaPosService;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.ClienteVentaResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.ProductoVentaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CU33 - Venta en Caja (POS). Los permisos se piden al módulo TESORERIA (o rol CAJERO),
 * igual que la apertura de caja, para que el cajero no dependa de permisos de Facturación.
 */
@RestController
@RequestMapping("/api/cajas/pos")
@RequiredArgsConstructor
@Validated
public class VentaPosController {

    private final VentaPosService service;

    @GetMapping("/productos")
    @PreAuthorize("hasPermission('TESORERIA','CONSULTAR') or hasRole('CAJERO')")
    public List<ProductoVentaResponse> productos(@RequestParam(defaultValue = "") @Size(max = 150) String search) {
        return service.productos(search);
    }

    @GetMapping("/clientes")
    @PreAuthorize("hasPermission('TESORERIA','CONSULTAR') or hasRole('CAJERO')")
    public List<ClienteVentaResponse> clientes(@RequestParam(defaultValue = "") @Size(max = 150) String search) {
        return service.clientes(search);
    }

    @PostMapping("/ventas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('TESORERIA','CREAR') or hasRole('CAJERO')")
    public VentaPosResponse vender(@Valid @RequestBody VentaPosRequest request) {
        return service.vender(request);
    }
}
