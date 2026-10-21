package modulo.seguridad_y_auditoria.cu07_gestionar_backups.dto;

import java.util.List;

public record RestaurarBackupRequest(
        String tipoRestauracion, // COMPLETA o SELECTIVA
        List<String> tablasSeleccionadas,
        boolean confirmacion,
        String motivo
) {}
