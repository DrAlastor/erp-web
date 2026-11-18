package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.controller;

import lombok.RequiredArgsConstructor;
import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto.*;
import modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.service.LibrosContablesService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contabilidad/libros")
@RequiredArgsConstructor
@PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
public class LibrosContablesController {

    private final LibrosContablesService service;

    @GetMapping("/periodos")
    public ResponseEntity<List<PeriodoContableSimpleDTO>> listarPeriodos() {
        return ResponseEntity.ok(service.listarPeriodos());
    }

    @GetMapping("/cuentas")
    public ResponseEntity<List<CuentaContableSimpleDTO>> listarCuentas() {
        return ResponseEntity.ok(service.listarCuentasMovimiento());
    }

    @GetMapping("/tipos-asiento")
    public ResponseEntity<List<String>> listarTiposAsiento() {
        return ResponseEntity.ok(service.listarTiposAsiento());
    }

    @GetMapping("/diario")
    public ResponseEntity<LibroDiarioResponse> consultarLibroDiario(
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String tipoAsiento,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda
    ) {
        return ResponseEntity.ok(service.obtenerLibroDiario(periodoId, fechaInicio, fechaFin, tipoAsiento, estado, busqueda));
    }

    @GetMapping("/mayor")
    public ResponseEntity<LibroMayorResponse> consultarLibroMayor(
            @RequestParam(required = false) Long cuentaId,
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        return ResponseEntity.ok(service.obtenerLibroMayor(cuentaId, periodoId, fechaInicio, fechaFin));
    }

    @GetMapping("/diario/exportar-csv")
    public ResponseEntity<byte[]> exportarDiarioCsv(
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String tipoAsiento,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda
    ) {
        byte[] csv = service.exportarLibroDiarioCsv(periodoId, fechaInicio, fechaFin, tipoAsiento, estado, busqueda);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("libro-diario.csv", StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/mayor/exportar-csv")
    public ResponseEntity<byte[]> exportarMayorCsv(
            @RequestParam(required = false) Long cuentaId,
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        byte[] csv = service.exportarLibroMayorCsv(cuentaId, periodoId, fechaInicio, fechaFin);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("libro-mayor.csv", StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }
}
