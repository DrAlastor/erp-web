package com.uagrm.erp.backend.seguridad.repositorio;

import com.uagrm.erp.backend.seguridad.dominio.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Roles por empresa. Toda busqueda por id incluye la empresa: es el aislamiento entre
 * inquilinos, para que nadie pueda tocar un rol de otra empresa conociendo su id.
 */
public interface RolRepositorio extends JpaRepository<Rol, UUID> {

    List<Rol> findByEmpresaIdOrderByNombre(UUID empresaId);

    Optional<Rol> findByIdAndEmpresaId(UUID id, UUID empresaId);

    Optional<Rol> findByEmpresaIdAndCodigo(UUID empresaId, String codigo);
}
