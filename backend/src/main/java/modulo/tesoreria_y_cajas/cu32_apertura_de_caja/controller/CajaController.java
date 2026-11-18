package modulo.tesoreria_y_cajas.cu32_apertura_de_caja.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.AperturaCajaRequest;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.CierreCajaRequest;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.CierreCajaResponse;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.EstadoCajaResponse;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.service.CajaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController("aperturaCajaController")
@RequestMapping("/api/cajas")
@RequiredArgsConstructor
public class CajaController {
    private final CajaService cajaService;

    @GetMapping("/estado")
    @PreAuthorize("hasPermission('TESORERIA','CONSULTAR') or hasRole('CAJERO')")
    public EstadoCajaResponse estado(@RequestParam(required = false) Integer sucursalId) {
        return cajaService.estado(sucursalId);
    }

    @PostMapping("/apertura")
    @PreAuthorize("hasPermission('TESORERIA','CREAR') or hasRole('CAJERO')")
    public EstadoCajaResponse.TurnoActivo abrir(@Valid @RequestBody AperturaCajaRequest request) {
        return cajaService.abrir(request);
    }

    @PostMapping("/{turnoId}/cierre")
    @PreAuthorize("hasPermission('TESORERIA','ANULAR') or hasRole('CAJERO')")
    public CierreCajaResponse cerrar(@PathVariable Long turnoId, @Valid @RequestBody CierreCajaRequest request) {
        return cajaService.cerrar(turnoId, request);
    }
}
