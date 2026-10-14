package modulo.catalogo_e_inventario.cu12_consultar_kardex.controller;

import lombok.RequiredArgsConstructor;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.dto.KardexConsultaResponse;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.dto.KardexValoracionResponse;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.service.ConsultaKardexService;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.service.MetodoCosteo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * CU12 - Consultar Kardex. Todos los endpoints son de lectura y piden el permiso
 * INVENTARIO_CONSULTAR. Los combos de producto y almacén los publica el CU11 en
 * {@code /api/inventario/productos} y {@code /api/inventario/almacenes}.
 */
@RestController
@RequestMapping("/api/kardex")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasPermission('INVENTARIO','CONSULTAR')")
public class KardexController {

    private final ConsultaKardexService service;

    /** Kardex valorado de un producto en un almacén, con saldo inicial, movimientos y totales. */
    @GetMapping
    public ResponseEntity<KardexConsultaResponse> consultar(
            @RequestParam Long productoId,
            @RequestParam Integer almacenId,
            @RequestParam(defaultValue = "PROMEDIO") MetodoCosteo metodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) String tipoMovimiento
    ) {
        return ResponseEntity.ok(service.consultar(productoId, almacenId, metodo, fechaInicio, fechaFin, tipoMovimiento));
    }

    /** Existencias valoradas de un almacén según el método de costeo. */
    @GetMapping("/valoracion")
    public ResponseEntity<KardexValoracionResponse> valorar(
            @RequestParam Integer almacenId,
            @RequestParam(defaultValue = "PROMEDIO") MetodoCosteo metodo
    ) {
        return ResponseEntity.ok(service.valorar(almacenId, metodo));
    }

    /** El mismo Kardex de la consulta, en CSV para Excel. */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam Long productoId,
            @RequestParam Integer almacenId,
            @RequestParam(defaultValue = "PROMEDIO") MetodoCosteo metodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) String tipoMovimiento
    ) {
        byte[] csv = service.exportarCsv(productoId, almacenId, metodo, fechaInicio, fechaFin, tipoMovimiento);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("kardex-" + productoId + "-" + almacenId + ".csv", StandardCharsets.UTF_8)
                        .build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }
}
