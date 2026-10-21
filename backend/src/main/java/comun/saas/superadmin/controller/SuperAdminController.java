package comun.saas.superadmin.controller;

import comun.saas.superadmin.dto.BackupItemDTO;
import comun.saas.superadmin.dto.BitacoraItemDTO;
import comun.saas.superadmin.dto.GenerarBackupRequest;
import comun.saas.superadmin.dto.SuperAdminLoginRequest;
import comun.saas.superadmin.dto.SuperAdminLoginResponse;
import comun.saas.superadmin.dto.TenantMonitorDTO;
import comun.saas.superadmin.service.SuperAdminService;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.BackupResponseDTO;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.RestauracionResponseDTO;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.RestaurarBackupRequest;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.VerificacionIntegridadDTO;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BackupService;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.RestoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final BackupService backupService;
    private final RestoreService restoreService;
    private final BitacoraAuditoriaService bitacoraAuditoriaService;

    @PostMapping("/auth/login")
    public ResponseEntity<SuperAdminLoginResponse> login(@Valid @RequestBody SuperAdminLoginRequest request) {
        return ResponseEntity.ok(superAdminService.login(request));
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<TenantMonitorDTO>> listarTenants() {
        return ResponseEntity.ok(superAdminService.listarTenants());
    }

    @PostMapping("/tenants/{id}/suspender")
    public ResponseEntity<Map<String, String>> suspenderTenant(@PathVariable("id") Integer id) {
        superAdminService.suspenderTenant(id);
        return ResponseEntity.ok(Map.of("mensaje", "Empresa suspendida exitosamente"));
    }

    @PostMapping("/tenants/{id}/reactivar")
    public ResponseEntity<Map<String, String>> reactivarTenant(@PathVariable("id") Integer id) {
        superAdminService.reactivarTenant(id);
        return ResponseEntity.ok(Map.of("mensaje", "Empresa reactivada exitosamente"));
    }

    @GetMapping("/backups")
    public ResponseEntity<List<BackupResponseDTO>> listarBackups() {
        return ResponseEntity.ok(backupService.listarTodosBackupsSuperAdmin());
    }

    @PostMapping("/backups/generar")
    public ResponseEntity<BackupResponseDTO> generarBackup(@Valid @RequestBody GenerarBackupRequest request) {
        return ResponseEntity.ok(backupService.generarBackup(request.empresaId(), "SUPER ADMIN", "BAJO_DEMANDA", "127.0.0.1"));
    }

    @GetMapping("/backups/{id}/verificar")
    public ResponseEntity<VerificacionIntegridadDTO> verificarIntegridad(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(backupService.verificarIntegridad(id, null));
    }

    @PostMapping("/backups/{id}/restaurar")
    public ResponseEntity<RestauracionResponseDTO> restaurarBackup(
            @PathVariable("id") Integer id,
            @RequestBody(required = false) RestaurarBackupRequest req) {
        if (req != null && "SELECTIVA".equalsIgnoreCase(req.tipoRestauracion()) && req.tablasSeleccionadas() != null && !req.tablasSeleccionadas().isEmpty()) {
            return ResponseEntity.ok(restoreService.restaurarSelectiva(id, null, req, "SUPER ADMIN", "127.0.0.1"));
        }
        String motivo = req != null ? req.motivo() : "Restauración global autorizada por Super Admin";
        return ResponseEntity.ok(restoreService.restaurarCompleta(id, null, "SUPER ADMIN", "127.0.0.1", motivo));
    }

    @GetMapping("/bitacora")
    public ResponseEntity<List<BitacoraItemDTO>> listarBitacora() {
        return ResponseEntity.ok(superAdminService.listarBitacora());
    }

    @GetMapping("/bitacora/inmutable")
    public ResponseEntity<List<Map<String, Object>>> listarBitacoraInmutable(
            @RequestParam(value = "empresaId", required = false) Integer empresaId,
            @RequestParam(value = "modulo", required = false) String modulo,
            @RequestParam(value = "tipoOperacion", required = false) String tipoOperacion,
            @RequestParam(value = "severidad", required = false) String severidad,
            @RequestParam(value = "resultado", required = false) String resultado,
            @RequestParam(value = "busqueda", required = false) String busqueda,
            @RequestParam(value = "desde", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
            @RequestParam(value = "hasta", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta) {
        return ResponseEntity.ok(bitacoraAuditoriaService.listarEventosSuperAdmin(
                empresaId, modulo, tipoOperacion, severidad, resultado, busqueda, desde, hasta));
    }

    @GetMapping("/bitacora/cloudwatch-status")
    public ResponseEntity<Map<String, Object>> obtenerEstadoCloudWatchSuperAdmin() {
        return ResponseEntity.ok(bitacoraAuditoriaService.obtenerEstadoCloudWatch());
    }

    @GetMapping("/bitacora/verificar-cadena")
    public ResponseEntity<Map<String, Object>> verificarCadenaBitacora() {
        return ResponseEntity.ok(bitacoraAuditoriaService.verificarIntegridadCadenaDetallada(null));
    }
}


