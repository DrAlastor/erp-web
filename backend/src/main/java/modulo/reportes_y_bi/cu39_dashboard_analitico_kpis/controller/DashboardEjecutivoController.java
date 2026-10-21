package modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.controller;

import lombok.RequiredArgsConstructor;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.SucursalOpcionResponse;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.service.DashboardEjecutivoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU-36 / CU39: dashboard ejecutivo.
 *
 * <p>Solo lectura; requiere el permiso REPORTES / CONSULTAR del catalogo de la CU03.
 * {@code sucursalId} es opcional y filtra los bloques cuyas tablas tienen sucursal.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardEjecutivoController {

    private final DashboardEjecutivoService service;

    @GetMapping("/ejecutivo")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public DashboardEjecutivoResponse ejecutivo(@RequestParam(required = false) Long sucursalId) {
        return service.ejecutivo(sucursalId);
    }

    @GetMapping("/ejecutivo/opciones/sucursales")
    @PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
    public List<SucursalOpcionResponse> sucursales() {
        return service.sucursales();
    }
}
