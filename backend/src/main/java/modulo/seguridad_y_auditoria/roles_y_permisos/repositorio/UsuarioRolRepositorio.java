package modulo.seguridad_y_auditoria.roles_y_permisos.repositorio;

import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.UsuarioRol;
import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Asignaciones de roles y, sobre todo, la consulta de permisos efectivos.
 *
 * <p>Esa consulta es el corazon de la autorizacion: une usuario_rol, rol, rol_permiso y
 * permiso, y descarta los roles inactivos. Un rol desactivado deja de otorgar permisos sin
 * necesidad de quitarle la asignacion a nadie.
 */
public interface UsuarioRolRepositorio extends JpaRepository<UsuarioRol, UsuarioRolId> {

    @Query("""
            select distinct p.codigo
            from RbacUsuarioRol ur
            join ur.rol r
            join r.permisos p
            where ur.id.usuarioId = :usuarioId
              and r.activo = true
            """)
    List<String> findCodigosDePermisosEfectivos(@Param("usuarioId") Long usuarioId);

    @Query("select ur.id.usuarioId from RbacUsuarioRol ur where ur.id.rolId = :rolId")
    List<Long> findUsuarioIdsPorRol(@Param("rolId") UUID rolId);

    List<UsuarioRol> findByIdUsuarioId(Long usuarioId);
}
