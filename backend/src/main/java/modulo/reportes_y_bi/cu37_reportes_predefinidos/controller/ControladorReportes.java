package modulo.reportes_y_bi.cu37_reportes_predefinidos.controller;

import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.FiltrosReporte;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.OpcionesReporteDto;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.ReporteDefinicionDto;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.dto.ReporteResultadoDto;
import modulo.reportes_y_bi.cu37_reportes_predefinidos.service.ServicioReportes;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * CU37 - Gestionar Reportes Predefinidos.
 *
 * <p>Basta con estar autenticado para entrar; el permiso de cada reporte depende de su
 * categoría (Ventas: {@code FACTURACION_CONSULTAR}, Inventario: {@code INVENTARIO_CONSULTAR})
 * y lo comprueba el servicio, que responde 403 si falta.
 */
@RestController
@RequestMapping("/api/reportes/predefinidos")
@PreAuthorize("isAuthenticated()")
public class ControladorReportes {

    private final ServicioReportes servicio;

    public ControladorReportes(ServicioReportes servicio) {
        this.servicio = servicio;
    }

    /** Los reportes que puede generar el usuario de la sesión. */
    @GetMapping
    public List<ReporteDefinicionDto> catalogo(Authentication usuario) {
        return servicio.catalogo(usuario);
    }

    /** Sucursales y categorías para los filtros. */
    @GetMapping("/opciones")
    public OpcionesReporteDto opciones() {
        return servicio.opciones();
    }

    @GetMapping("/{codigo}")
    public ReporteResultadoDto generar(
            @PathVariable String codigo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer sucursalId,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Integer limite,
            Authentication usuario) {
        return servicio.generar(codigo, new FiltrosReporte(desde, hasta, sucursalId, categoriaId, limite), usuario);
    }

    /** El mismo reporte en CSV para Excel. */
    @GetMapping("/{codigo}/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @PathVariable String codigo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer sucursalId,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Integer limite,
            Authentication usuario) {
        byte[] csv = servicio.exportarCsv(codigo, new FiltrosReporte(desde, hasta, sucursalId, categoriaId, limite), usuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(servicio.nombreArchivo(codigo), StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }
}
