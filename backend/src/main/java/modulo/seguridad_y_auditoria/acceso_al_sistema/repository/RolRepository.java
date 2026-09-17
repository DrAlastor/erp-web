package modulo.seguridad_y_auditoria.acceso_al_sistema.repository;

import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(String nombre);
}
