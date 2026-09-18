package modulo.seguridad_y_auditoria.roles_y_permisos.mapper;

import modulo.seguridad_y_auditoria.roles_y_permisos.dto.AsignacionDto;
import modulo.seguridad_y_auditoria.roles_y_permisos.entity.UsuarioRol;

import org.springframework.stereotype.Component;

/** Convierte una asignación de rol en el DTO que lista los roles de un usuario. */
@Component
public class AsignacionMapper {

    public AsignacionDto toDto(UsuarioRol asignacion) {
        return new AsignacionDto(
                asignacion.getRol().getId(),
                asignacion.getRol().getCodigo(),
                asignacion.getRol().getNombre(),
                asignacion.getRol().isActivo(),
                asignacion.getAsignadoEn());
    }
}
