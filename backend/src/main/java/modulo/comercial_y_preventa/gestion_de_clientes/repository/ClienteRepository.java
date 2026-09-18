package modulo.comercial_y_preventa.gestion_de_clientes.repository;

import modulo.comercial_y_preventa.gestion_de_clientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository
        extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    boolean existsByNitCi(String nitCi);

    boolean existsByNitCiAndIdNot(String nitCi, Long id);

    Optional<Cliente> findByUsuarioUsername(String username);
}
