package modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto;

import java.time.LocalDateTime;

public record BackupResponseDTO(
        Integer id,
        Integer empresaId,
        String codigoEmpresa,
        String razonSocial,
        String nombreDatabase,
        String nombreArchivo,
        String rutaStorage,
        Long tamanoBytes,
        String tamanoLegible,
        String estado,
        String tipoBackup,
        String solicitadoPor,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String checksumSha256,
        LocalDateTime retencionHasta,
        String jobId,
        String estadoVerificacion,
        String codigoError,
        LocalDateTime fechaCreacion
) {
    public static String formatearTamano(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 B";
        final String[] unidades = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitoGrupos = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitoGrupos), unidades[digitoGrupos]);
    }
}
