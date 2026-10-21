package modulo.seguridad_y_auditoria.cu07_gestionar_backups.controller;

import comun.multitenancy.TenantContext;
import comun.multitenancy.TenantDescriptor;
import jakarta.servlet.http.HttpServletRequest;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.*;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BackupService;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.RestoreService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.util.List;

/**
 * Controlador de Backups y Restauración para el Administrador de Empresa / Tenant (CU07).
 * Asegura aislamiento total: solo permite consultar y operar sobre la instancia propia del tenant.
 */
@RestController
@RequestMapping("/api/backups")
@PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR') or hasRole('ADMINISTRADOR') or hasRole('GERENCIA')")
public class TenantBackupController {

    private final BackupService backupService;
    private final RestoreService restoreService;

    public TenantBackupController(BackupService backupService, RestoreService restoreService) {
        this.backupService = backupService;
        this.restoreService = restoreService;
    }

    @GetMapping
    public List<BackupResponseDTO> listarBackups() {
        TenantDescriptor tenant = obtenerTenantActual();
        return backupService.listarBackupsTenant(tenant.empresaId().intValue());
    }

    @GetMapping("/puntos-restauracion")
    public List<BackupResponseDTO> listarPuntosRestauracion() {
        TenantDescriptor tenant = obtenerTenantActual();
        return backupService.listarBackupsTenant(tenant.empresaId().intValue())
                .stream()
                .filter(b -> "COMPLETADO".equalsIgnoreCase(b.estado()))
                .toList();
    }

    @PostMapping
    public BackupResponseDTO generarBackupBajoDemanda(
            @RequestBody(required = false) CrearBackupRequest request,
            HttpServletRequest httpRequest) {
        TenantDescriptor tenant = obtenerTenantActual();
        String usuario = obtenerUsuarioActual();
        String ip = obtenerIpCliente(httpRequest);
        String tipo = (request != null && request.tipoBackup() != null) ? request.tipoBackup() : "BAJO_DEMANDA";

        return backupService.generarBackup(tenant.empresaId().intValue(), usuario, tipo, ip);
    }

    @GetMapping("/{id}/verificar")
    public VerificacionIntegridadDTO verificarIntegridad(@PathVariable Integer id) {
        TenantDescriptor tenant = obtenerTenantActual();
        return backupService.verificarIntegridad(id, tenant.empresaId().intValue());
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable Integer id) {
        TenantDescriptor tenant = obtenerTenantActual();
        File file = backupService.obtenerArchivoFisico(id, tenant.empresaId().intValue());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }

    @PostMapping("/{id}/restaurar-completa")
    public RestauracionResponseDTO restaurarCompleta(
            @PathVariable Integer id,
            @RequestBody(required = false) RestaurarBackupRequest request,
            HttpServletRequest httpRequest) {
        TenantDescriptor tenant = obtenerTenantActual();
        String usuario = obtenerUsuarioActual();
        String ip = obtenerIpCliente(httpRequest);
        String motivo = (request != null) ? request.motivo() : "Restauración completa solicitada por Administrador";

        return restoreService.restaurarCompleta(id, tenant.empresaId().intValue(), usuario, ip, motivo);
    }

    @PostMapping("/{id}/restaurar-selectiva")
    public RestauracionResponseDTO restaurarSelectiva(
            @PathVariable Integer id,
            @RequestBody RestaurarBackupRequest request,
            HttpServletRequest httpRequest) {
        TenantDescriptor tenant = obtenerTenantActual();
        String usuario = obtenerUsuarioActual();
        String ip = obtenerIpCliente(httpRequest);

        return restoreService.restaurarSelectiva(id, tenant.empresaId().intValue(), request, usuario, ip);
    }

    @GetMapping("/restauraciones")
    public List<RestauracionResponseDTO> listarRestauraciones() {
        TenantDescriptor tenant = obtenerTenantActual();
        return restoreService.listarRestauraciones(tenant.empresaId().intValue());
    }

    @GetMapping("/politica")
    public PoliticaBackupDTO obtenerPolitica() {
        TenantDescriptor tenant = obtenerTenantActual();
        return backupService.obtenerPolitica(tenant.empresaId().intValue());
    }

    @PutMapping("/politica")
    public PoliticaBackupDTO guardarPolitica(
            @RequestBody PoliticaBackupDTO dto,
            HttpServletRequest httpRequest) {
        TenantDescriptor tenant = obtenerTenantActual();
        String usuario = obtenerUsuarioActual();
        String ip = obtenerIpCliente(httpRequest);

        return backupService.guardarPolitica(tenant.empresaId().intValue(), dto, usuario, ip);
    }

    @GetMapping("/tablas-disponibles")
    public List<String> listarTablasDisponibles() {
        return restoreService.obtenerTablasDisponibles();
    }

    private TenantDescriptor obtenerTenantActual() {
        return TenantContext.current()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión sin tenant activo"));
    }

    private String obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "admin@tenant";
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf != null && !xf.isBlank()) ? xf.split(",")[0] : request.getRemoteAddr();
    }
}
