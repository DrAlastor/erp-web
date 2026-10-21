package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.ActualizarListaPrecioRequest;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.ActualizarPreciosMatrizRequest;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.CrearListaPrecioRequest;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.ItemPrecioProductoDTO;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.ListaPrecioDTO;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.service.ListaPrecioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * CU14 - Gestionar Precios y Listas de Precio.
 *
 * Administración de esquemas de precios comerciales y tarifas por producto (RN19).
 */
@RestController
@RequestMapping({"/api/v1/listas-precios", "/api/listas-precios"})
@RequiredArgsConstructor
@Validated
public class ListaPrecioController {

    private final ListaPrecioService service;

    @GetMapping
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public List<ListaPrecioDTO> list() {
        return service.listarListas();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public ListaPrecioDTO getById(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasPermission('COMERCIAL','CREAR')")
    public ResponseEntity<ListaPrecioDTO> create(@Valid @RequestBody CrearListaPrecioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearLista(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR')")
    public ListaPrecioDTO update(@PathVariable Integer id, @Valid @RequestBody ActualizarListaPrecioRequest req) {
        return service.actualizarLista(id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR')")
    public ListaPrecioDTO changeStatus(@PathVariable Integer id, @RequestParam boolean activo) {
        return service.cambiarEstado(id, activo);
    }

    @GetMapping("/{id}/matriz-productos")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public List<ItemPrecioProductoDTO> getMatrizProductos(@PathVariable Integer id) {
        return service.obtenerMatrizProductos(id);
    }

    @PutMapping("/{id}/precios")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR')")
    public ResponseEntity<Void> updatePreciosMatriz(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarPreciosMatrizRequest req) {
        service.actualizarPreciosMatriz(id, req);
        return ResponseEntity.noContent().build();
    }
}
