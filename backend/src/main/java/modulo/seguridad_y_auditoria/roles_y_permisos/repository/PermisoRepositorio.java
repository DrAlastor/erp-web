package modulo.seguridad_y_auditoria.roles_y_permisos.repository;

import modulo.seguridad_y_auditoria.roles_y_permisos.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Catalogo de permisos persistido. */
public interface PermisoRepositorio extends JpaRepository<Permiso, UUID> {

    Optional<Permiso> findByCodigo(String codigo);

    List<Permiso> findByCodigoIn(Collection<String> codigos);

    List<Permiso> findAllByOrderByModuloAscAccionAsc();
}
