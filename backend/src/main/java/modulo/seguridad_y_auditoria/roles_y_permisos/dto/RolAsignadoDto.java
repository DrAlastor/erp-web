package modulo.seguridad_y_auditoria.roles_y_permisos.dto;

import java.util.UUID;

/**
 * Rol asignado a un usuario, en la forma plana que necesita el listado de la CU-02 para
 * resolver los roles de una página completa de cuentas en una sola consulta.
 */
public record RolAsignadoDto(Long usuarioId, UUID rolId, String nombre) {
}
