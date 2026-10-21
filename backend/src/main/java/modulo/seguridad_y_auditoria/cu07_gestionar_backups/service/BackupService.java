package modulo.seguridad_y_auditoria.cu07_gestionar_backups.service;

import comun.multitenancy.TenantDataSourceProvider;
import comun.multitenancy.TenantDescriptor;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.BackupResponseDTO;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.PoliticaBackupDTO;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto.VerificacionIntegridadDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final JdbcTemplate masterJdbcTemplate;
    private final TenantDataSourceProvider tenantDataSourceProvider;
    private final BitacoraAuditoriaService auditService;

    public BackupService(
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
            TenantDataSourceProvider tenantDataSourceProvider,
            BitacoraAuditoriaService auditService) {
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.tenantDataSourceProvider = tenantDataSourceProvider;
        this.auditService = auditService;
    }

    public List<BackupResponseDTO> listarBackupsTenant(Integer empresaId) {
        return masterJdbcTemplate.query("""
                SELECT hb.id, hb.empresa_id, e.codigo_empresa, e.razon_social, td.nombre_database,
                       hb.nombre_archivo, hb.ruta_storage, hb.tamano_bytes, hb.estado, hb.fecha_creacion,
                       hb.tipo_backup, hb.solicitado_por, hb.fecha_inicio, hb.fecha_fin, hb.checksum_sha256,
                       hb.retencion_hasta, hb.job_id, hb.estado_verificacion, hb.codigo_error
                FROM historial_backups hb
                JOIN empresa e ON e.id = hb.empresa_id
                JOIN tenant_database td ON td.id = hb.tenant_database_id
                WHERE hb.empresa_id = ?
                ORDER BY hb.fecha_creacion DESC
                """,
                (rs, rowNum) -> mapBackupRow(rs),
                empresaId);
    }

    public List<BackupResponseDTO> listarTodosBackupsSuperAdmin() {
        return masterJdbcTemplate.query("""
                SELECT hb.id, hb.empresa_id, e.codigo_empresa, e.razon_social, td.nombre_database,
                       hb.nombre_archivo, hb.ruta_storage, hb.tamano_bytes, hb.estado, hb.fecha_creacion,
                       hb.tipo_backup, hb.solicitado_por, hb.fecha_inicio, hb.fecha_fin, hb.checksum_sha256,
                       hb.retencion_hasta, hb.job_id, hb.estado_verificacion, hb.codigo_error
                FROM historial_backups hb
                JOIN empresa e ON e.id = hb.empresa_id
                JOIN tenant_database td ON td.id = hb.tenant_database_id
                ORDER BY hb.fecha_creacion DESC
                """,
                (rs, rowNum) -> mapBackupRow(rs));
    }

    public BackupResponseDTO generarBackup(Integer empresaId, String solicitadoPor, String tipoBackup, String ip) {
        LocalDateTime inicio = LocalDateTime.now();
        String jobId = "JOB-BKP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        var tenantInfo = masterJdbcTemplate.query("""
                SELECT td.id AS tenant_database_id, td.nombre_database, e.codigo_empresa, e.razon_social
                FROM tenant_database td
                JOIN empresa e ON e.id = td.empresa_id
                WHERE e.id = ?
                """,
                rs -> {
                    if (!rs.next()) return null;
                    return new Object[]{
                            rs.getInt("tenant_database_id"),
                            rs.getString("nombre_database"),
                            rs.getString("codigo_empresa"),
                            rs.getString("razon_social")
                    };
                },
                empresaId);

        if (tenantInfo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Base de datos o empresa no encontrada");
        }

        int tenantDatabaseId = (int) tenantInfo[0];
        String dbName = (String) tenantInfo[1];
        String codigoEmpresa = (String) tenantInfo[2];
        String razonSocial = (String) tenantInfo[3];

        String timestamp = inicio.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nombreArchivo = String.format("%s_backup_%s.sql", dbName, timestamp);
        File backupDir = new File("backups/" + codigoEmpresa);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        File backupFile = new File(backupDir, nombreArchivo);

        String tipoFinal = (tipoBackup != null && !tipoBackup.isBlank()) ? tipoBackup.toUpperCase() : "BAJO_DEMANDA";
        String checksum = null;
        long tamanoBytes = 0;
        String estado = "COMPLETADO";
        String estadoVerificacion = "VERIFICADO";
        String codigoError = null;

        try {
            TenantDescriptor descriptor = new TenantDescriptor((long) empresaId, codigoEmpresa);
            DataSource dataSource = tenantDataSourceProvider.get(descriptor);

            try (Connection conn = dataSource.getConnection();
                 FileWriter writer = new FileWriter(backupFile, StandardCharsets.UTF_8);
                 Statement stmt = conn.createStatement()) {

                writer.write("-- ====================================================================\n");
                writer.write(String.format("-- NEXO ERP SAAS - RESPALDO LOGICO DEL TENANT\n"));
                writer.write(String.format("-- EMPRESA: %s (%s) | BASE DE DATOS: %s\n", razonSocial, codigoEmpresa, dbName));
                writer.write(String.format("-- FECHA UTC: %s | JOB ID: %s | TIPO: %s\n", inicio, jobId, tipoFinal));
                writer.write(String.format("-- SOLICITADO POR: %s\n", solicitadoPor));
                writer.write("-- ====================================================================\n\n");
                writer.write("SET session_replication_role = replica; -- Desactiva temporalmente FKs para restauracion segura\n\n");

                List<String> tables = new ArrayList<>();
                try (ResultSet tablesRs = conn.getMetaData().getTables(null, "public", "%", new String[]{"TABLE"})) {
                    while (tablesRs.next()) {
                        String t = tablesRs.getString("TABLE_NAME");
                        if (!"flyway_schema_history".equalsIgnoreCase(t)) {
                            tables.add(t);
                        }
                    }
                }

                for (String table : tables) {
                    writer.write(String.format("-- ==================== TABLA: %s ====================\n", table));
                    try (ResultSet dataRs = stmt.executeQuery("SELECT * FROM \"" + table + "\"")) {
                        var meta = dataRs.getMetaData();
                        int colCount = meta.getColumnCount();

                        List<String> colNames = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            colNames.add("\"" + meta.getColumnName(i) + "\"");
                        }
                        String colListStr = String.join(", ", colNames);

                        while (dataRs.next()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("INSERT INTO \"").append(table).append("\" (").append(colListStr).append(") VALUES (");
                            for (int i = 1; i <= colCount; i++) {
                                Object val = dataRs.getObject(i);
                                if (val == null) {
                                    sb.append("NULL");
                                } else if (val instanceof Number || val instanceof Boolean) {
                                    sb.append(val);
                                } else {
                                    sb.append("'").append(val.toString().replace("'", "''")).append("'");
                                }
                                if (i < colCount) sb.append(", ");
                            }
                            sb.append(");\n");
                            writer.write(sb.toString());
                        }
                    }
                    writer.write("\n");
                }

                writer.write("SET session_replication_role = DEFAULT; -- Reactiva verificacion de integridad\n");
            }

            tamanoBytes = backupFile.length();
            checksum = calcularChecksumArchivo(backupFile);

        } catch (Exception ex) {
            log.error("Fallo generando backup de tenant {}: {}", codigoEmpresa, ex.getMessage(), ex);
            estado = "FALLIDO";
            estadoVerificacion = "CORRUPTO";
            codigoError = ex.getMessage();
        }

        LocalDateTime fin = LocalDateTime.now();
        int diasRetencion = obtenerDiasRetencion(empresaId);
        LocalDateTime retencionHasta = inicio.plusDays(diasRetencion);
        String rutaStorage = "s3://erp-backups/" + codigoEmpresa + "/" + nombreArchivo;

        Integer backupId = masterJdbcTemplate.queryForObject("""
                INSERT INTO historial_backups
                (empresa_id, tenant_database_id, nombre_archivo, ruta_storage, tamano_bytes, estado, fecha_creacion,
                 tipo_backup, solicitado_por, fecha_inicio, fecha_fin, checksum_sha256, retencion_hasta, job_id,
                 estado_verificacion, codigo_error)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Integer.class,
                empresaId, tenantDatabaseId, nombreArchivo, rutaStorage, tamanoBytes, estado, fin,
                tipoFinal, solicitadoPor, inicio, fin, checksum, retencionHasta, jobId,
                estadoVerificacion, codigoError);

        // Registro de auditoría inmutable externa (CU08)
        String detalleAudit = String.format("Respaldo %s generado para %s (%s). Tamaño: %s. Checksum: %s",
                tipoFinal, razonSocial, codigoEmpresa, BackupResponseDTO.formatearTamano(tamanoBytes), checksum);
        auditService.registrarEvento(
                empresaId, codigoEmpresa, solicitadoPor,
                "BACKUP_" + tipoFinal, "BACKUPS",
                detalleAudit, ip, estado,
                String.format("{\"backupId\": %d, \"jobId\": \"%s\", \"tamanoBytes\": %d}", backupId, jobId, tamanoBytes));

        return new BackupResponseDTO(
                backupId, empresaId, codigoEmpresa, razonSocial, dbName,
                nombreArchivo, rutaStorage, tamanoBytes, BackupResponseDTO.formatearTamano(tamanoBytes),
                estado, tipoFinal, solicitadoPor, inicio, fin, checksum, retencionHasta,
                jobId, estadoVerificacion, codigoError, fin);
    }

    public VerificacionIntegridadDTO verificarIntegridad(Integer backupId, Integer optionalEmpresaId) {
        var backup = obtenerPorId(backupId);
        if (optionalEmpresaId != null && !backup.empresaId().equals(optionalEmpresaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso no autorizado al respaldo de otra empresa");
        }

        File file = new File("backups/" + backup.codigoEmpresa() + "/" + backup.nombreArchivo());
        if (!file.exists()) {
            masterJdbcTemplate.update("UPDATE historial_backups SET estado_verificacion = 'CORRUPTO' WHERE id = ?", backupId);
            return new VerificacionIntegridadDTO(backupId, backup.nombreArchivo(), backup.checksumSha256(), null, false, "CORRUPTO", "Archivo físico no encontrado en almacenamiento");
        }

        String calculated = calcularChecksumArchivo(file);
        boolean integro = calculated.equalsIgnoreCase(backup.checksumSha256());
        String nuevoEstado = integro ? "VERIFICADO" : "CORRUPTO";

        masterJdbcTemplate.update("UPDATE historial_backups SET estado_verificacion = ? WHERE id = ?", nuevoEstado, backupId);

        auditService.registrarEvento(
                backup.empresaId(), backup.codigoEmpresa(), "SISTEMA",
                "VERIFICACION_INTEGRIDAD", "BACKUPS",
                String.format("Verificación de integridad de backup ID %d: %s", backupId, nuevoEstado),
                "127.0.0.1", nuevoEstado,
                String.format("{\"backupId\": %d, \"esperado\": \"%s\", \"calculado\": \"%s\"}", backupId, backup.checksumSha256(), calculated));

        return new VerificacionIntegridadDTO(
                backupId, backup.nombreArchivo(), backup.checksumSha256(), calculated,
                integro, nuevoEstado, integro ? "Integridad de datos validada con SHA-256." : "Fallo de integridad: Checksum no coincide.");
    }

    public File obtenerArchivoFisico(Integer backupId, Integer optionalEmpresaId) {
        var backup = obtenerPorId(backupId);
        if (optionalEmpresaId != null && !backup.empresaId().equals(optionalEmpresaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso no autorizado al archivo de otra empresa");
        }
        File file = new File("backups/" + backup.codigoEmpresa() + "/" + backup.nombreArchivo());
        if (!file.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo de respaldo no existe en el almacenamiento");
        }
        return file;
    }

    public PoliticaBackupDTO obtenerPolitica(Integer empresaId) {
        List<PoliticaBackupDTO> list = masterJdbcTemplate.query("""
                SELECT empresa_id, frecuencia, hora_ejecucion, dias_retencion, activo, notificar_email,
                       ultimo_backup_automatico, fecha_modificacion
                FROM politica_backup_tenant
                WHERE empresa_id = ?
                """,
                (rs, rowNum) -> new PoliticaBackupDTO(
                        rs.getInt("empresa_id"),
                        rs.getString("frecuencia"),
                        rs.getString("hora_ejecucion"),
                        rs.getInt("dias_retencion"),
                        rs.getBoolean("activo"),
                        rs.getString("notificar_email"),
                        rs.getTimestamp("ultimo_backup_automatico") != null ? rs.getTimestamp("ultimo_backup_automatico").toLocalDateTime() : null,
                        rs.getTimestamp("fecha_modificacion").toLocalDateTime()
                ),
                empresaId);

        if (list.isEmpty()) {
            return new PoliticaBackupDTO(empresaId, "DIARIO", "02:00", 30, true, "admin@asustech.bo", null, LocalDateTime.now());
        }
        return list.getFirst();
    }

    public PoliticaBackupDTO guardarPolitica(Integer empresaId, PoliticaBackupDTO dto, String solicitadoPor, String ip) {
        masterJdbcTemplate.update("""
                INSERT INTO politica_backup_tenant
                (empresa_id, frecuencia, hora_ejecucion, dias_retencion, activo, notificar_email, fecha_modificacion)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (empresa_id) DO UPDATE SET
                    frecuencia = EXCLUDED.frecuencia,
                    hora_ejecucion = EXCLUDED.hora_ejecucion,
                    dias_retencion = EXCLUDED.dias_retencion,
                    activo = EXCLUDED.activo,
                    notificar_email = EXCLUDED.notificar_email,
                    fecha_modificacion = NOW()
                """,
                empresaId,
                dto.frecuencia() != null ? dto.frecuencia() : "DIARIO",
                dto.horaEjecucion() != null ? dto.horaEjecucion() : "02:00",
                dto.diasRetencion() != null && dto.diasRetencion() > 0 ? dto.diasRetencion() : 30,
                dto.activo() != null ? dto.activo() : true,
                dto.notificarEmail());

        auditService.registrarEvento(
                empresaId, null, solicitadoPor,
                "CONFIGURAR_POLITICA_BACKUP", "BACKUPS",
                String.format("Política actualizada: %s a las %s, retención %d días", dto.frecuencia(), dto.horaEjecucion(), dto.diasRetencion()),
                ip, "COMPLETADO", null);

        return obtenerPolitica(empresaId);
    }

    @Scheduled(cron = "0 0 * * * *") // Se ejecuta al inicio de cada hora
    public void ejecutarBackupsAutomaticos() {
        LocalTime now = LocalTime.now();
        String currentHourPrefix = String.format("%02d:", now.getHour());

        List<Integer> empresas = masterJdbcTemplate.query(
                "SELECT empresa_id FROM politica_backup_tenant WHERE activo = TRUE AND hora_ejecucion LIKE ?",
                (rs, rowNum) -> rs.getInt("empresa_id"),
                currentHourPrefix + "%");

        for (Integer empId : empresas) {
            try {
                log.info("Iniciando respaldo automático para empresa ID {}", empId);
                generarBackup(empId, "SISTEMA_CRON", "AUTOMATICO", "127.0.0.1");
                masterJdbcTemplate.update("UPDATE politica_backup_tenant SET ultimo_backup_automatico = NOW() WHERE empresa_id = ?", empId);
            } catch (Exception e) {
                log.error("Error en respaldo automático de empresa {}: {}", empId, e.getMessage());
            }
        }
    }

    public BackupResponseDTO obtenerPorId(Integer backupId) {
        List<BackupResponseDTO> list = masterJdbcTemplate.query("""
                SELECT hb.id, hb.empresa_id, e.codigo_empresa, e.razon_social, td.nombre_database,
                       hb.nombre_archivo, hb.ruta_storage, hb.tamano_bytes, hb.estado, hb.fecha_creacion,
                       hb.tipo_backup, hb.solicitado_por, hb.fecha_inicio, hb.fecha_fin, hb.checksum_sha256,
                       hb.retencion_hasta, hb.job_id, hb.estado_verificacion, hb.codigo_error
                FROM historial_backups hb
                JOIN empresa e ON e.id = hb.empresa_id
                JOIN tenant_database td ON td.id = hb.tenant_database_id
                WHERE hb.id = ?
                """,
                (rs, rowNum) -> mapBackupRow(rs),
                backupId);

        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Respaldo no encontrado");
        }
        return list.getFirst();
    }

    private int obtenerDiasRetencion(Integer empresaId) {
        try {
            Integer dias = masterJdbcTemplate.queryForObject(
                    "SELECT dias_retencion FROM politica_backup_tenant WHERE empresa_id = ?",
                    Integer.class, empresaId);
            return (dias != null && dias > 0) ? dias : 30;
        } catch (Exception e) {
            return 30;
        }
    }

    private String calcularChecksumArchivo(File file) {
        try (InputStream is = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException("Error calculando SHA-256 de archivo", e);
        }
    }

    private BackupResponseDTO mapBackupRow(ResultSet rs) throws java.sql.SQLException {
        long bytes = rs.getLong("tamano_bytes");
        return new BackupResponseDTO(
                rs.getInt("id"),
                rs.getInt("empresa_id"),
                rs.getString("codigo_empresa"),
                rs.getString("razon_social"),
                rs.getString("nombre_database"),
                rs.getString("nombre_archivo"),
                rs.getString("ruta_storage"),
                bytes,
                BackupResponseDTO.formatearTamano(bytes),
                rs.getString("estado"),
                rs.getString("tipo_backup"),
                rs.getString("solicitado_por"),
                rs.getTimestamp("fecha_inicio") != null ? rs.getTimestamp("fecha_inicio").toLocalDateTime() : null,
                rs.getTimestamp("fecha_fin") != null ? rs.getTimestamp("fecha_fin").toLocalDateTime() : null,
                rs.getString("checksum_sha256"),
                rs.getTimestamp("retencion_hasta") != null ? rs.getTimestamp("retencion_hasta").toLocalDateTime() : null,
                rs.getString("job_id"),
                rs.getString("estado_verificacion"),
                rs.getString("codigo_error"),
                rs.getTimestamp("fecha_creacion").toLocalDateTime()
        );
    }
}
