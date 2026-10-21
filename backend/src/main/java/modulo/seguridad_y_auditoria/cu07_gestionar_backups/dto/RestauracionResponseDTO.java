package modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto;

import java.time.LocalDateTime;

public record RestauracionResponseDTO(
        Integer id,
        Integer empresaId,
        Integer backupId,
        String tipoRestauracion,
        String tablasSeleccionadas,
        String solicitadoPor,
        String estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        String detalles,
        Boolean reversionDisponible
) {}
