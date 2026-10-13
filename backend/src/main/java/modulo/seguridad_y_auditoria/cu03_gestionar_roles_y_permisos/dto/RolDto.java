package modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto;

import java.util.Set;
import java.util.UUID;

/**
 * Un rol con su matriz de permisos, como la muestra la pantalla de Roles y Permisos.
 *
 * <p>La conversión desde la entidad la hace
 * {@link modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.mapper.RolMapper}.
 */
public record RolDto(
        String id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        boolean esSistema,
        Set<String> permisos) {
}

