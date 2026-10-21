package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.CatalogoQbeResponse;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.PlantillaQbeRequest;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.PlantillaQbeResponse;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.ResultadoQbeResponse;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service.ReporteQbeService;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service.ReporteQbeService.Archivo;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service.ReporteQbeService.Formato;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CU38 - Constructor de reportes QBE. Solo para el Administrador: todos los endpoints piden
 * REPORTES_MODIFICAR, que el Vendedor no tiene.
 */
@RestController
@RequestMapping("/api/reportes/qbe")
@RequiredArgsConstructor
@PreAuthorize("hasPermission('REPORTES','MODIFICAR')")
public class ReporteQbeController {

    private final ReporteQbeService service;

    /** Entidades habilitadas con sus campos, operadores por tipo de dato y agregaciones. */
    @GetMapping("/catalogo")
    public ResponseEntity<CatalogoQbeResponse> catalogo() {
        return ResponseEntity.ok(service.catalogo());
    }

    /** Ejecuta la definición y devuelve una página del resultado. */
    @PostMapping("/vista-previa")
    public ResponseEntity<ResultadoQbeResponse> vistaPrevia(
            @Valid @RequestBody DefinicionQbe definicion,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamanio) {
        return ResponseEntity.ok(service.vistaPrevia(definicion, pagina, tamanio));
    }

    /** Exporta la definición (sin guardarla) a PDF o XLSX. */
    @PostMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @Valid @RequestBody DefinicionQbe definicion,
            @RequestParam(defaultValue = "XLSX") Formato formato,
            @RequestParam(required = false) String nombre) {
        return descarga(service.exportar(definicion, nombre, formato));
    }

    @GetMapping("/plantillas")
    public ResponseEntity<List<PlantillaQbeResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/plantillas/{id}")
    public ResponseEntity<PlantillaQbeResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping("/plantillas")
    public ResponseEntity<PlantillaQbeResponse> crear(
            @Valid @RequestBody PlantillaQbeRequest request, Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, autenticacion));
    }

    @PutMapping("/plantillas/{id}")
    public ResponseEntity<PlantillaQbeResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody PlantillaQbeRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/plantillas/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Ejecuta una plantilla guardada y la exporta. */
    @GetMapping("/plantillas/{id}/exportar")
    public ResponseEntity<byte[]> exportarPlantilla(
            @PathVariable Long id, @RequestParam(defaultValue = "XLSX") Formato formato) {
        return descarga(service.exportarPlantilla(id, formato));
    }

    private static ResponseEntity<byte[]> descarga(Archivo archivo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .contentType(archivo.tipo())
                .body(archivo.contenido());
    }
}
