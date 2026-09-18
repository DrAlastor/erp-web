package modulo.seguridad_y_auditoria.acceso_al_sistema.repository;

import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Integer> {

    Optional<Permiso> findByModuloAndPantallaAndAccion(String modulo, String pantalla, String accion);
}
