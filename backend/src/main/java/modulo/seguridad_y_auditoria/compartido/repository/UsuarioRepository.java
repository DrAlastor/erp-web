package modulo.seguridad_y_auditoria.compartido.repository;

import jakarta.persistence.LockModeType;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u " +
           "LEFT JOIN u.empleado e " +
           "WHERE LOWER(u.email) = LOWER(:valor) " +
           "   OR LOWER(COALESCE(u.username, '')) = LOWER(:valor) " +
           "   OR LOWER(u.email) LIKE LOWER(CONCAT(:valor, '@%')) " +
           "   OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT(:valor, '@%')) " +
           "   OR (e IS NOT NULL AND (" +
           "       LOWER(e.email) = LOWER(:valor) " +
           "       OR LOWER(e.email) LIKE LOWER(CONCAT(:valor, '@%')) " +
           "       OR LOWER(e.ci) = LOWER(:valor)" +
           "   )) " +
           "ORDER BY CASE " +
           "   WHEN LOWER(u.email) = LOWER(:valor) THEN 0 " +
           "   WHEN LOWER(u.email) = 'admin@asustech.bo' AND (LOWER(COALESCE(u.username, '')) = LOWER(:valor) OR LOWER(:valor) = 'admin') THEN 1 " +
           "   WHEN LOWER(COALESCE(u.username, '')) = LOWER(:valor) THEN 2 " +
           "   WHEN LOWER(u.email) LIKE LOWER(CONCAT(:valor, '@%')) THEN 3 " +
           "   ELSE 4 " +
           "END, u.id ASC")
    List<Usuario> findCandidatesByUsernameOrEmail(@Param("valor") String valor);

    default Optional<Usuario> findByUsernameOrEmail(String valor) {
        List<Usuario> list = findCandidatesByUsernameOrEmail(valor);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u " +
           "LEFT JOIN u.empleado e " +
           "WHERE LOWER(u.email) = LOWER(:valor) " +
           "   OR LOWER(COALESCE(u.username, '')) = LOWER(:valor) " +
           "   OR LOWER(u.email) LIKE LOWER(CONCAT(:valor, '@%')) " +
           "   OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT(:valor, '@%')) " +
           "   OR (e IS NOT NULL AND (" +
           "       LOWER(e.email) = LOWER(:valor) " +
           "       OR LOWER(e.email) LIKE LOWER(CONCAT(:valor, '@%')) " +
           "       OR LOWER(e.ci) = LOWER(:valor)" +
           "   )) " +
           "ORDER BY CASE " +
           "   WHEN LOWER(u.email) = LOWER(:valor) THEN 0 " +
           "   WHEN LOWER(u.email) = 'admin@asustech.bo' AND (LOWER(COALESCE(u.username, '')) = LOWER(:valor) OR LOWER(:valor) = 'admin') THEN 1 " +
           "   WHEN LOWER(COALESCE(u.username, '')) = LOWER(:valor) THEN 2 " +
           "   WHEN LOWER(u.email) LIKE LOWER(CONCAT(:valor, '@%')) THEN 3 " +
           "   ELSE 4 " +
           "END, u.id ASC")
    List<Usuario> findCandidatesForLogin(@Param("valor") String valor);

    default Optional<Usuario> findForLogin(String valor) {
        List<Usuario> list = findCandidatesForLogin(valor);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Usuario> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:username) OR LOWER(COALESCE(u.username, '')) = LOWER(:username) OR LOWER(u.email) LIKE LOWER(CONCAT(:username, '@%')) OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT(:username, '@%')) ORDER BY CASE WHEN LOWER(COALESCE(u.username, '')) = LOWER(:username) THEN 0 WHEN LOWER(u.email) = LOWER(:username) THEN 1 ELSE 2 END")
    List<Usuario> findCandidatesByUsername(@Param("username") String username);

    default Optional<Usuario> findByUsername(String username) {
        List<Usuario> list = findCandidatesByUsername(username);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE LOWER(u.email) = LOWER(:username) OR LOWER(COALESCE(u.username, '')) = LOWER(:username) OR LOWER(u.email) LIKE LOWER(CONCAT(:username, '@%')) OR LOWER(COALESCE(u.username, '')) LIKE LOWER(CONCAT(:username, '@%'))")
    boolean existsByUsernameIgnoreCase(@Param("username") String username);

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    Optional<Usuario> findByEmpleadoId(Long empleadoId);

    boolean existsByEmpleadoId(Long empleadoId);

    List<Usuario> findAllByEmpleadoId(Long empleadoId);
}
