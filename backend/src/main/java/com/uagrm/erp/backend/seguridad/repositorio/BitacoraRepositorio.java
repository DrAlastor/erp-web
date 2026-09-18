package com.uagrm.erp.backend.seguridad.repositorio;

import com.uagrm.erp.backend.seguridad.dominio.BitacoraEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepositorio extends JpaRepository<BitacoraEvento, Long> {
}
