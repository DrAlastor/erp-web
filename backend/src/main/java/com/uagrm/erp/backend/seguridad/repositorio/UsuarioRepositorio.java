package com.uagrm.erp.backend.seguridad.repositorio;

import com.uagrm.erp.backend.seguridad.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailAndActivoTrue(String email);

    Optional<Usuario> findByIdAndEmpresaId(UUID id, UUID empresaId);

    List<Usuario> findByEmpresaIdOrderByNombre(UUID empresaId);
}
