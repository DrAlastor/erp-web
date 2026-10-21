package comun.saas.superadmin.dto;

import java.time.LocalDateTime;

public record TenantMonitorDTO(
        Integer id,
        String codigoEmpresa,
        String razonSocial,
        String nombreComercial,
        String nit,
        String email,
        String ciudad,
        String pais,
        String estadoEmpresa,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaSuspension,
        String nombreDatabase,
        String host,
        Integer port,
        String estadoDatabase,
        String versionEsquema,
        LocalDateTime ultimaMigracion,
        String planCodigo,
        String planNombre,
        String estadoSuscripcion,
        Boolean esPrueba,
        LocalDateTime fechaFinSuscripcion
) {}
