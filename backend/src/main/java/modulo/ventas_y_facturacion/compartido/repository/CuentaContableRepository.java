package modulo.ventas_y_facturacion.compartido.repository;

import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaContableRepository extends JpaRepository<CuentaContable, Long> {

    Optional<CuentaContable> findByCodigo(String codigo);
}
