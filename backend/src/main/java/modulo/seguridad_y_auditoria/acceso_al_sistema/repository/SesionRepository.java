package modulo.seguridad_y_auditoria.acceso_al_sistema.repository;

import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, Long> {

    Optional<Sesion> findByRefreshTokenHash(String refreshTokenHash);
}
