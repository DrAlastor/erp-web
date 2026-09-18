package modulo.seguridad_y_auditoria.roles_y_permisos.web.dto;

import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.Permiso;
import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.Rol;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Un rol con su matriz de permisos, como la muestra la pantalla de Roles y Permisos. */
public record RolDto(
        UUID id,
        String codigo,
        String nombre,
        String descripcion,
        boolean activo,
        boolean esSistema,
        Set<String> permisos) {

    public static RolDto de(Rol rol) {
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
