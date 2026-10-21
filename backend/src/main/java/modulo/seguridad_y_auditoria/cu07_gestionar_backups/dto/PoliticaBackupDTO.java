package modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto;

import java.time.LocalDateTime;

public record PoliticaBackupDTO(
        Integer empresaId,
        String frecuencia, // DIARIO, SEMANAL, MENSUAL
        String horaEjecucion, // formato "02:00"
        Integer diasRetencion,
        Boolean activo,
        String notificarEmail,
        LocalDateTime ultimoBackupAutomatico,
        LocalDateTime fechaModificacion
) {}
