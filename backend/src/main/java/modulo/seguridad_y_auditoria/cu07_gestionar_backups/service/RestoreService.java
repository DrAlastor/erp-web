package modulo.seguridad_y_auditoria.cu07_gestionar_backups.service;

import comun.multitenancy.TenantDataSourceProvider;
import comun.multitenancy.TenantDescriptor;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.BackupResponseDTO;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.RestauracionResponseDTO;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.RestaurarBackupRequest;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.VerificacionIntegridadDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RestoreService {

    private static final Logger log = LoggerFactory.getLogger(RestoreService.class);

    private final JdbcTemplate masterJdbcTemplate;
    private final TenantDataSourceProvider tenantDataSourceProvider;
    private final BackupService backupService;
    private final BitacoraAuditoriaService auditService;

    public RestoreService(
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
            TenantDataSourceProvider tenantDataSourceProvider,
            BackupService backupService,
            BitacoraAuditoriaService auditService) {
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.tenantDataSourceProvider = tenantDataSourceProvider;
        this.backupService = backupService;
        this.auditService = auditService;
    }

    public List<String> obtenerTablasDisponibles() {
        return List.of(
                "clientes",
                "productos",
                "categorias",
                "unidades_medida",
                "proveedores",
                "almacenes",
                "listas_precio",
                "pedidos",
                "cotizaciones",
                "ventas",
                "empleados",
                "usuario",
                "rol"
        );
    }

    public RestauracionResponseDTO restaurarCompleta(
            Integer backupId,
            Integer empresaId,
            String solicitadoPor,
            String ip,
            String motivo) {

        BackupResponseDTO backup = backupService.obtenerPorId(backupId);

        // FA-06: Verificación estricta de pertenencia de tenant
        if (empresaId != null && !backup.empresaId().equals(empresaId)) {
            auditService.registrarEvento(
                    empresaId, null, solicitadoPor,
                    "ACCESO_DENEGADO_BACKUP", "SEGURIDAD",
                    String.format("Intento de restaurar respaldo ID %d de otra empresa", backupId),
                    ip, "RECHAZADO", null);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene autorización para restaurar este respaldo");
        }

        // FA-05: Verificación de integridad previa mediante SHA-256
        VerificacionIntegridadDTO verificacion = backupService.verificarIntegridad(backupId, empresaId);
        if (!verificacion.integro()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El archivo de respaldo está corrupto o su firma SHA-256 no coincide. Operación cancelada.");
        }

        File file = backupService.obtenerArchivoFisico(backupId, empresaId);
        LocalDateTime inicio = LocalDateTime.now();

        // Registrar inicio de restauración en estado EN_PROCESO
        Integer restauracionId = masterJdbcTemplate.queryForObject("""
                INSERT INTO historial_restauraciones
                (empresa_id, backup_id, tipo_restauracion, tablas_seleccionadas, solicitado_por, estado, fecha_inicio, detalles, reversion_disponible)
                VALUES (?, ?, 'COMPLETA', 'TODAS', ?, 'EN_PROCESO', ?, ?, TRUE) RETURNING id
                """, Integer.class,
                backup.empresaId(), backupId, solicitadoPor, inicio, "Iniciando recuperación completa: " + (motivo != null ? motivo : "Sin motivo"));

        // Pre-restore snapshot automático para reversión (Safety Checkpoint)
        try {
            log.info("Generando snapshot de reversión previo a la restauración del tenant {}", backup.codigoEmpresa());
            backupService.generarBackup(backup.empresaId(), "REVERSION_PRE_RESTORE", "LOGICO", ip);
        } catch (Exception ex) {
            log.warn("Nota: No se pudo generar snapshot previo: {}", ex.getMessage());
        }

        try {
            TenantDescriptor descriptor = new TenantDescriptor((long) backup.empresaId(), backup.codigoEmpresa());
            DataSource ds = tenantDataSourceProvider.get(descriptor);

            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {

                // Desactivar temporalmente constraints para carga masiva
                stmt.execute("SET session_replication_role = replica;");

                StringBuilder batch = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("--")) {
                        continue;
                    }
                    batch.append(line).append(" ");
                    if (line.endsWith(";")) {
                        String sql = batch.toString();
                        batch.setLength(0);
                        try {
                            stmt.execute(sql);
                        } catch (Exception ex) {
                            // Si es una clave duplicada o constraint transitorio, registrar
                            log.debug("Aviso ejecutando sentencia de restauración: {}", ex.getMessage());
                        }
                    }
                }

                stmt.execute("SET session_replication_role = DEFAULT;");
            }

            LocalDateTime fin = LocalDateTime.now();
            String detalles = String.format("Restauración completa ejecutada exitosamente desde archivo %s. Motivo: %s",
                    backup.nombreArchivo(), motivo != null ? motivo : "Restaura estándar");

            masterJdbcTemplate.update("""
                    UPDATE historial_restauraciones
                    SET estado = 'COMPLETADO', fecha_fin = ?, detalles = ?
                    WHERE id = ?
                    """, fin, detalles, restauracionId);

            auditService.registrarEvento(
                    backup.empresaId(), backup.codigoEmpresa(), solicitadoPor,
                    "RESTAURACION_COMPLETA", "BACKUPS",
                    detalles, ip, "COMPLETADO",
                    String.format("{\"backupId\": %d, \"restauracionId\": %d}", backupId, restauracionId));

            return new RestauracionResponseDTO(
                    restauracionId, backup.empresaId(), backupId, "COMPLETA",
                    "TODAS", solicitadoPor, "COMPLETADO", inicio, fin, detalles, true);

        } catch (Exception e) {
            log.error("Fallo durante restauración completa de tenant {}: {}", backup.codigoEmpresa(), e.getMessage(), e);
            LocalDateTime fin = LocalDateTime.now();
            String errorMsg = "Fallo en ejecución: " + e.getMessage();

            masterJdbcTemplate.update("""
                    UPDATE historial_restauraciones
                    SET estado = 'FALLIDO', fecha_fin = ?, detalles = ?
                    WHERE id = ?
                    """, fin, errorMsg, restauracionId);

            auditService.registrarEvento(
                    backup.empresaId(), backup.codigoEmpresa(), solicitadoPor,
                    "RESTAURACION_FALLIDA", "BACKUPS",
                    "Fallo al restaurar: " + e.getMessage(), ip, "FALLIDO",
                    String.format("{\"backupId\": %d, \"restauracionId\": %d}", backupId, restauracionId));

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error ejecutando la restauración: " + e.getMessage());
        }
    }

    public RestauracionResponseDTO restaurarSelectiva(
            Integer backupId,
            Integer empresaId,
            RestaurarBackupRequest request,
            String solicitadoPor,
            String ip) {

        if (request.tablasSeleccionadas() == null || request.tablasSeleccionadas().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar al menos una tabla para restauración selectiva");
        }

        BackupResponseDTO backup = backupService.obtenerPorId(backupId);
        if (empresaId != null && !backup.empresaId().equals(empresaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene autorización para restaurar este respaldo");
        }

        VerificacionIntegridadDTO verificacion = backupService.verificarIntegridad(backupId, empresaId);
        if (!verificacion.integro()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Archivo de respaldo corrupto");
        }

        File file = backupService.obtenerArchivoFisico(backupId, empresaId);
        Set<String> tablasTarget = new HashSet<>();
        for (String t : request.tablasSeleccionadas()) {
            tablasTarget.add(t.toLowerCase().trim());
        }

        LocalDateTime inicio = LocalDateTime.now();
        String tablasStr = String.join(", ", request.tablasSeleccionadas());

        Integer restauracionId = masterJdbcTemplate.queryForObject("""
                INSERT INTO historial_restauraciones
                (empresa_id, backup_id, tipo_restauracion, tablas_seleccionadas, solicitado_por, estado, fecha_inicio, detalles, reversion_disponible)
                VALUES (?, ?, 'SELECTIVA', ?, ?, 'EN_PROCESO', ?, ?, TRUE) RETURNING id
                """, Integer.class,
                backup.empresaId(), backupId, tablasStr, solicitadoPor, inicio, "Recuperación selectiva de: " + tablasStr);

        try {
            TenantDescriptor descriptor = new TenantDescriptor((long) backup.empresaId(), backup.codigoEmpresa());
            DataSource ds = tenantDataSourceProvider.get(descriptor);

            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {

                stmt.execute("SET session_replication_role = replica;");

                // Truncar únicamente las tablas seleccionadas
                for (String t : tablasTarget) {
                    try {
                        stmt.execute("TRUNCATE TABLE \"" + t + "\" CASCADE;");
                    } catch (Exception ex) {
                        log.warn("Aviso truncando tabla selectiva {}: {}", t, ex.getMessage());
                    }
                }

                // Cargar únicamente los INSERTs de las tablas seleccionadas
                StringBuilder batch = new StringBuilder();
                String line;
                int insertsEjecutados = 0;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("--")) {
                        continue;
                    }
                    batch.append(line).append(" ");
                    if (line.endsWith(";")) {
                        String sql = batch.toString();
                        batch.setLength(0);

                        boolean aplica = false;
                        for (String t : tablasTarget) {
                            if (sql.contains("INSERT INTO \"" + t + "\"") || sql.contains("INSERT INTO " + t + " ")) {
                                aplica = true;
                                break;
                            }
                        }

                        if (aplica) {
                            stmt.execute(sql);
                            insertsEjecutados++;
                        }
                    }
                }

                stmt.execute("SET session_replication_role = DEFAULT;");
                log.info("Restauración selectiva completada: {} sentencias ejecutadas.", insertsEjecutados);
            }

            LocalDateTime fin = LocalDateTime.now();
            String detalles = String.format("Restauración selectiva de [%s] completada con éxito. Motivo: %s",
                    tablasStr, request.motivo() != null ? request.motivo() : "Sin motivo");

            masterJdbcTemplate.update("""
                    UPDATE historial_restauraciones
                    SET estado = 'COMPLETADO', fecha_fin = ?, detalles = ?
                    WHERE id = ?
                    """, fin, detalles, restauracionId);

            auditService.registrarEvento(
                    backup.empresaId(), backup.codigoEmpresa(), solicitadoPor,
                    "RESTAURACION_SELECTIVA", "BACKUPS",
                    detalles, ip, "COMPLETADO",
                    String.format("{\"backupId\": %d, \"tablas\": \"%s\"}", backupId, tablasStr));

            return new RestauracionResponseDTO(
                    restauracionId, backup.empresaId(), backupId, "SELECTIVA",
                    tablasStr, solicitadoPor, "COMPLETADO", inicio, fin, detalles, true);

        } catch (Exception e) {
            log.error("Fallo durante restauración selectiva: {}", e.getMessage(), e);
            LocalDateTime fin = LocalDateTime.now();
            String errorMsg = "Fallo selectivo: " + e.getMessage();

            masterJdbcTemplate.update("""
                    UPDATE historial_restauraciones
                    SET estado = 'FALLIDO', fecha_fin = ?, detalles = ?
                    WHERE id = ?
                    """, fin, errorMsg, restauracionId);

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error en recuperación selectiva: " + e.getMessage());
        }
    }

    public List<RestauracionResponseDTO> listarRestauraciones(Integer empresaId) {
        String sql = (empresaId != null)
                ? "SELECT * FROM historial_restauraciones WHERE empresa_id = ? ORDER BY fecha_inicio DESC"
                : "SELECT * FROM historial_restauraciones ORDER BY fecha_inicio DESC";

        var args = (empresaId != null) ? new Object[]{empresaId} : new Object[]{};

        return masterJdbcTemplate.query(sql,
                (rs, rowNum) -> new RestauracionResponseDTO(
                        rs.getInt("id"),
                        rs.getInt("empresa_id"),
                        rs.getInt("backup_id"),
                        rs.getString("tipo_restauracion"),
                        rs.getString("tablas_seleccionadas"),
                        rs.getString("solicitado_por"),
                        rs.getString("estado"),
                        rs.getTimestamp("fecha_inicio").toLocalDateTime(),
                        rs.getTimestamp("fecha_fin") != null ? rs.getTimestamp("fecha_fin").toLocalDateTime() : null,
                        rs.getString("detalles"),
                        rs.getBoolean("reversion_disponible")
                ),
                args);
    }
}
