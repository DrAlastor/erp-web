package modulo.seguridad_y_auditoria.roles_y_permisos.mapper;

import modulo.seguridad_y_auditoria.roles_y_permisos.dto.PermisoDto;
import modulo.seguridad_y_auditoria.roles_y_permisos.entity.Permiso;

import org.springframework.stereotype.Component;

/**
 * Convierte un permiso del catálogo en el DTO de la pantalla de Roles y Permisos, con las
 * etiquetas legibles para que el frontend no tenga que traducir códigos.
 */
@Component
public class PermisoMapper {

    public PermisoDto toDto(Permiso permiso) {
        return new PermisoDto(
                permiso.getCodigo(),
                permiso.getModulo().name(),
                permiso.getAccion().name(),
                permiso.getModulo().etiqueta(),
                permiso.getAccion().etiqueta(),
                permiso.getDescripcion());
    }
}
