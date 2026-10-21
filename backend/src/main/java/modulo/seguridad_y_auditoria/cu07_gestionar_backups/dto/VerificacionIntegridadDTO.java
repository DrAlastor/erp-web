package modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto;

public record VerificacionIntegridadDTO(
        Integer backupId,
        String nombreArchivo,
        String checksumRegistrado,
        String checksumCalculado,
        boolean integro,
        String estadoVerificacion,
        String mensaje
) {}
