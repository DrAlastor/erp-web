package modulo.ventas_y_facturacion.cu13_gestionar_clientes.repository;

import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository
        extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    boolean existsByNitCi(String nitCi);

    boolean existsByNitCiAndIdNot(String nitCi, Long id);

    @Query("SELECT c FROM Cliente c WHERE LOWER(c.email) = LOWER(:username) OR LOWER(c.email) LIKE LOWER(CONCAT(:username, '@%'))")
    Optional<Cliente> findByUsuarioUsername(@Param("username") String username);
}
