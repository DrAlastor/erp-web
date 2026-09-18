package modulo.Comercial_y_Preventa.Gestion_de_Clientes.repository;

import modulo.Comercial_y_Preventa.Gestion_de_Clientes.entity.Cliente;
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
