package comun.saas.superadmin.dto;

import java.time.LocalDateTime;

public record BitacoraItemDTO(
        Long id,
        LocalDateTime fecha,
        String accion,
        String origen,
        String detalle,
        String usuario,
        String codigoEmpresa,
        String estado
) {}
