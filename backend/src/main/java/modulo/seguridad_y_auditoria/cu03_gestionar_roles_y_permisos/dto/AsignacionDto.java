package modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Un rol asignado a un usuario, con cuándo se le asignó. */
public record AsignacionDto(
        String rolId,
        String codigo,
        String nombre,
        boolean activo,
        OffsetDateTime asignadoEn) {
}
