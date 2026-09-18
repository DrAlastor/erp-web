package modulo.seguridad_y_auditoria.compartido.repository;

import modulo.seguridad_y_auditoria.compartido.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Catálogo de permisos legacy de CU01: modulo, pantalla y acción. */
@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Integer> {

    Optional<Permiso> findByModuloAndPantallaAndAccion(String modulo, String pantalla, String accion);
}