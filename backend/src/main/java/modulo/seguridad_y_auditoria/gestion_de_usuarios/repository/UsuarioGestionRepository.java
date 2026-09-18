package modulo.seguridad_y_auditoria.gestion_de_usuarios.repository;

import jakarta.persistence.LockModeType;

import modulo.seguridad_y_auditoria.compartido.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Cuentas de usuario del listado y del detalle de la CU-02.
 *
 * <p>Es un repositorio aparte del compartido a propósito: el CRUD de la CU-02 necesita
 * consultas que la autenticación no debe tener —bloqueo pesimista para editar sin carreras,
 * comprobaciones de unicidad por empresa— y así cada caso de uso expone solo lo suyo.
 */
public interface UsuarioGestionRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<Usuario> findByIdAndEmpresaId(Long id, UUID empresaId);

    /**
     * Cuenta bloqueada para administrarla. Serializa dos ediciones simultáneas sobre la
     * misma cuenta dentro de su transacción.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id and u.empresaId = :empresaId")
    Optional<Usuario> findForAdministration(@Param("id") Long id, @Param("empresaId") UUID empresaId);
}
