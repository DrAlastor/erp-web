package com.uagrm.erp.backend.seguridad.repositorio;

import com.uagrm.erp.backend.seguridad.dominio.UsuarioRol;
import com.uagrm.erp.backend.seguridad.dominio.UsuarioRolId;
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
            from UsuarioRol ur
            join ur.rol r
            join r.permisos p
            where ur.id.usuarioId = :usuarioId
              and r.activo = true
            """)
    List<String> findCodigosDePermisosEfectivos(@Param("usuarioId") UUID usuarioId);

    @Query("select ur.id.usuarioId from UsuarioRol ur where ur.id.rolId = :rolId")
    List<UUID> findUsuarioIdsPorRol(@Param("rolId") UUID rolId);

    List<UsuarioRol> findByIdUsuarioId(UUID usuarioId);
}
