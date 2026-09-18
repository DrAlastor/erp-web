package modulo.seguridad_y_auditoria.roles_y_permisos.mapper;

import modulo.seguridad_y_auditoria.roles_y_permisos.dto.RolDto;
import modulo.seguridad_y_auditoria.roles_y_permisos.entity.Permiso;
import modulo.seguridad_y_auditoria.roles_y_permisos.entity.Rol;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.TreeSet;

/**
 * Convierte la entidad {@link Rol} en el DTO que consume la pantalla de Roles y Permisos.
 *
 * <p>La matriz se ordena por código para que dos consultas seguidas devuelvan la misma
 * lista y el frontend pueda compararla sin ruido.
 */
@Component
public class RolMapper {

    public RolDto toDto(Rol rol) {
        Set<String> codigos = new TreeSet<>();
        for (Permiso permiso : rol.getPermisos()) {
            codigos.add(permiso.getCodigo());
        }
        return new RolDto(
                rol.getId(),
                rol.getCodigo(),
                rol.getNombre(),
                rol.getDescripcion(),
                rol.isActivo(),
                rol.isEsSistema(),
                codigos);
    }
}
