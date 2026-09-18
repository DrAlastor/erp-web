package modulo.seguridad_y_auditoria.roles_y_permisos.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Un rol asignado a un usuario, con cuándo se le asignó. */
public record AsignacionDto(
        UUID rolId,
        String codigo,
        String nombre,
        boolean activo,
        OffsetDateTime asignadoEn) {
}
