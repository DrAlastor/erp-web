package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.ActualizarDescuentoRequest;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.CrearDescuentoRequest;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.dto.DescuentoDTO;
import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.service.DescuentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Endpoints para parametrización de Descuentos y Promociones (CU14).
 */
@RestController
@RequestMapping({"/api/v1/descuentos", "/api/descuentos"})
@RequiredArgsConstructor
@Validated
public class DescuentoController {

    private final DescuentoService service;

    @GetMapping
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public List<DescuentoDTO> list(@RequestParam(required = false) Boolean soloActivos) {
        if (Boolean.TRUE.equals(soloActivos)) {
            return service.listarActivos();
        }
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('COMERCIAL','CONSULTAR')")
    public DescuentoDTO getById(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasPermission('COMERCIAL','CREAR')")
    public ResponseEntity<DescuentoDTO> create(@Valid @RequestBody CrearDescuentoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR')")
    public DescuentoDTO update(@PathVariable Integer id, @Valid @RequestBody ActualizarDescuentoRequest req) {
        return service.actualizar(id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR')")
    public DescuentoDTO changeStatus(@PathVariable Integer id, @RequestParam boolean activo) {
        return service.cambiarEstado(id, activo);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('COMERCIAL','MODIFICAR') or hasPermission('COMERCIAL','ELIMINAR')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
