package modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto;

public record CrearBackupRequest(
        String tipoBackup, // BAJO_DEMANDA o LOGICO
        String observaciones
) {}
