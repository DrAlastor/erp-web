package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.controller;

import lombok.RequiredArgsConstructor;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto.BalanceGeneralResponse;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.dto.EstadoResultadosResponse;
import modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.service.InformacionFinancieraService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contabilidad/financiera")
@RequiredArgsConstructor
public class InformacionFinancieraController {

    private final InformacionFinancieraService service;

    @GetMapping("/gestiones")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public ResponseEntity<List<Integer>> listarGestiones() {
        return ResponseEntity.ok(service.listarGestionesDisponibles());
    }

    @GetMapping("/balance-general")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public ResponseEntity<BalanceGeneralResponse> obtenerBalanceGeneral(
            @RequestParam(required = false) Integer gestion,
            @RequestParam(required = false) Integer mesCorte,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCorte,
            @RequestParam(required = false, defaultValue = "4") Integer nivel
    ) {
        return ResponseEntity.ok(service.generarBalanceGeneral(gestion, mesCorte, fechaCorte, nivel));
    }

    @GetMapping("/estado-resultados")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public ResponseEntity<EstadoResultadosResponse> obtenerEstadoResultados(
            @RequestParam(required = false) Integer gestion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "4") Integer nivel
    ) {
        return ResponseEntity.ok(service.generarEstadoResultados(gestion, fechaInicio, fechaFin, nivel));
    }

    @GetMapping("/balance-general/exportar")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public ResponseEntity<byte[]> exportarBalance(
            @RequestParam(required = false) Integer gestion,
            @RequestParam(required = false) Integer mesCorte,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCorte,
            @RequestParam(required = false, defaultValue = "4") Integer nivel
    ) {
        byte[] csv = service.exportarBalanceCsv(gestion, mesCorte, fechaCorte, nivel);
        String filename = "Balance_General_" + (gestion != null ? gestion : "Actual") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/estado-resultados/exportar")
    @PreAuthorize("hasPermission('CONTABILIDAD','CONSULTAR')")
    public ResponseEntity<byte[]> exportarEstadoResultados(
            @RequestParam(required = false) Integer gestion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "4") Integer nivel
    ) {
        byte[] csv = service.exportarEstadoResultadosCsv(gestion, fechaInicio, fechaFin, nivel);
        String filename = "Estado_Resultados_" + (gestion != null ? gestion : "Actual") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
