package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.AsistenteVozResponse;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ConsultaReporteVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ConversarRequest;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.OpcionesReporteVozResponse;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ReporteVozResponse;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.service.ReporteVozService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * CU39 - Reportes por comando de voz. Todos los endpoints son de lectura y piden el permiso
 * REPORTES_CONSULTAR (Administrador y Vendedor). La transcripción de voz a texto la hace el
 * cliente (Web o Móvil); aquí llega la frase ya confirmada por el usuario, y la respuesta
 * vuelve como texto para que el cliente la lea en voz alta.
 */
@RestController
@RequestMapping("/api/reportes/voz")
@RequiredArgsConstructor
@PreAuthorize("hasPermission('REPORTES','CONSULTAR')")
public class ReporteVozController {

    private final ReporteVozService service;

    /** Métricas, agrupaciones, sucursales y frases de ejemplo para la pantalla. */
    @GetMapping("/opciones")
    public ResponseEntity<OpcionesReporteVozResponse> opciones() {
        return ResponseEntity.ok(service.opciones());
    }

    /** Responde el mensaje dictado con el asistente (Gemini o intérprete local) y su reporte. */
    @PostMapping("/conversar")
    public ResponseEntity<AsistenteVozResponse> conversar(@Valid @RequestBody ConversarRequest request) {
        return ResponseEntity.ok(service.conversar(request));
    }

    /** Ejecuta una consulta estructurada; la usa la pantalla al refinar el reporte. */
    @PostMapping("/ejecutar")
    public ResponseEntity<ReporteVozResponse> ejecutar(@Valid @RequestBody ConsultaReporteVoz consulta) {
        return ResponseEntity.ok(service.ejecutar(consulta));
    }

    /** El reporte de la consulta, en CSV para Excel. */
    @PostMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@Valid @RequestBody ConsultaReporteVoz consulta) {
        byte[] csv = service.exportarCsv(consulta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("reporte-voz-" + consulta.getMetrica().name().toLowerCase() + ".csv",
                                StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }
}
