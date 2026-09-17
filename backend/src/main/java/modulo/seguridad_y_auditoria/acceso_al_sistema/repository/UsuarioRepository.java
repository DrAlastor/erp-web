package modulo.seguridad_y_auditoria.acceso_al_sistema.repository;

import jakarta.persistence.LockModeType;

import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository
        extends JpaRepository<Usuario, Long>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<Usuario> {

    @Query("SELECT u FROM Usuario u WHERE u.username = :valor OR u.email = :valor")
    Optional<Usuario> findByUsernameOrEmail(@Param("valor") String valor);

    // Serializa los intentos de login de una misma cuenta dentro de la transaccion.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.username = :valor OR u.email = :valor")
    Optional<Usuario> findForLogin(@Param("valor") String valor);

    Optional<Usuario> findByUsername(String username);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.id = :id")
    Optional<Usuario> findForAdministration(@Param("id") Long id);
}
