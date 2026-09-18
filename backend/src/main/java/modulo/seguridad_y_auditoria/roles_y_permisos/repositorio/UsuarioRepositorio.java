package modulo.seguridad_y_auditoria.roles_y_permisos.repositorio;

import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailAndActivoTrue(String email);

    Optional<Usuario> findByIdAndEmpresaId(Long id, UUID empresaId);

    List<Usuario> findByEmpresaIdOrderByNombre(UUID empresaId);
}
