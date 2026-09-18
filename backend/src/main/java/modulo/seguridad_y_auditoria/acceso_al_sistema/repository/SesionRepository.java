package modulo.seguridad_y_auditoria.acceso_al_sistema.repository;

import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, Long> {

    Optional<Sesion> findByRefreshTokenHash(String refreshTokenHash);
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically=true)
    @org.springframework.data.jpa.repository.Query("update Sesion s set s.fechaCierre=:fecha where s.usuario.id=:id and s.fechaCierre is null")
    int cerrarActivas(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("fecha") java.time.LocalDateTime fecha);
}

