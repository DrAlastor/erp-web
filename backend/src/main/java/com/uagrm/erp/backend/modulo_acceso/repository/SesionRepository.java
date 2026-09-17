package com.uagrm.erp.backend.modulo_acceso.repository;

import com.uagrm.erp.backend.modulo_acceso.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, Long> {

    Optional<Sesion> findByRefreshTokenHash(String refreshTokenHash);
}
