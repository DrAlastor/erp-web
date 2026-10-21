package comun.saas.superadmin.dto;

import java.time.LocalDateTime;

public record BackupItemDTO(
        Integer id,
        Integer empresaId,
        String codigoEmpresa,
        String razonSocial,
        String nombreDatabase,
        String nombreArchivo,
        String rutaStorage,
        Long tamanoBytes,
        String estado,
        LocalDateTime fechaCreacion
) {}
