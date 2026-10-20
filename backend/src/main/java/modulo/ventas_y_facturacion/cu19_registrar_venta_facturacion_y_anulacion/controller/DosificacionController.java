package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.CambiarEstadoDosificacionRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.DosificacionRequest;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.dto.DosificacionResponse;
import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.service.DosificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CU19: dosificaciones del SIN (secuencia_facturacion). Registrarlas o activarlas es
 * configuración fiscal, así que pide MODIFICAR en FACTURACION; el cajero solo las consulta.
 */
@RestController
@RequestMapping("/api/ventas/dosificaciones")
@RequiredArgsConstructor
public class DosificacionController {

    private final DosificacionService service;

    @GetMapping
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public List<DosificacionResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @PreAuthorize("hasPermission('FACTURACION','MODIFICAR')")
    public ResponseEntity<DosificacionResponse> registrar(@Valid @RequestBody DosificacionRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(body));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasPermission('FACTURACION','MODIFICAR')")
    public DosificacionResponse cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody CambiarEstadoDosificacionRequest body) {
        return service.cambiarEstado(id, body.activo());
    }
}
