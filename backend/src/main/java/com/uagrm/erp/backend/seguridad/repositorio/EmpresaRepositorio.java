package com.uagrm.erp.backend.seguridad.repositorio;

import com.uagrm.erp.backend.seguridad.dominio.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmpresaRepositorio extends JpaRepository<Empresa, UUID> {
}
