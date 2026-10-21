package modulo.seguridad_y_auditoria.cu08_consultar_bitacora.controller;

import comun.multitenancy.TenantContext;
import comun.multitenancy.TenantDescriptor;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controlador de Bitácora de Auditoría para el Administrador de Empresa / Tenant (CU08).
 * Nivel 1 de Supervisión: Cada empresa consulta únicamente la actividad de su propia instancia
 * con acceso de solo lectura, verificación de inmutabilidad SHA-256 y replicación en Amazon CloudWatch Logs.
 */
@RestController
@RequestMapping("/api/bitacora")
@PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR') or hasRole('ADMINISTRADOR') or hasRole('GERENCIA')")
public class TenantBitacoraController {

    private final BitacoraAuditoriaService bitacoraAuditoriaService;

    public TenantBitacoraController(BitacoraAuditoriaService bitacoraAuditoriaService) {
        this.bitacoraAuditoriaService = bitacoraAuditoriaService;
    }

    @GetMapping
    public List<Map<String, Object>> listarBitacoraTenant(
            @RequestParam(value = "modulo", required = false) String modulo,
            @RequestParam(value = "tipoOperacion", required = false) String tipoOperacion,
            @RequestParam(value = "severidad", required = false) String severidad,
            @RequestParam(value = "resultado", required = false) String resultado,
            @RequestParam(value = "busqueda", required = false) String busqueda,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        TenantDescriptor tenant = obtenerTenantActual();
        return bitacoraAuditoriaService.listarEventosTenant(
                tenant.empresaId().intValue(),
                modulo,
                tipoOperacion,
                severidad,
                resultado,
                busqueda,
                desde,
                hasta);
    }

    @GetMapping("/verificar-integridad")
    public ResponseEntity<Map<String, Object>> verificarIntegridadCadena() {
        TenantDescriptor tenant = obtenerTenantActual();
        Map<String, Object> resultado = bitacoraAuditoriaService.verificarIntegridadCadenaDetallada(tenant.empresaId().intValue());
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/cloudwatch-status")
    public ResponseEntity<Map<String, Object>> obtenerEstadoCloudWatch() {
        return ResponseEntity.ok(bitacoraAuditoriaService.obtenerEstadoCloudWatch());
    }

    @GetMapping("/exportar")
    public ResponseEntity<Map<String, Object>> exportarInformeAuditoria() {
        TenantDescriptor tenant = obtenerTenantActual();
        Map<String, Object> reporte = bitacoraAuditoriaService.exportarInformeAuditoria(
                tenant.empresaId().intValue(),
                tenant.codigoEmpresa());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData(
                "attachment",
                String.format("auditoria_%s_%s.json", tenant.codigoEmpresa(), LocalDate.now()));

        return new ResponseEntity<>(reporte, headers, HttpStatus.OK);
    }

    private TenantDescriptor obtenerTenantActual() {
        return TenantContext.current()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión sin tenant activo"));
    }
}
