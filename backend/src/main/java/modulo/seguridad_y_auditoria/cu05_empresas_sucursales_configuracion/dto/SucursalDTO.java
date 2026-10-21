package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.dto;

import java.time.LocalTime;

public record SucursalDTO(
        Integer id,
        Integer ciudadId,
        String ciudadNombre,
        String nombre,
        String direccion,
        String telefono,
        LocalTime horaApertura,
        LocalTime horaCierre,
        boolean activo) {
}
