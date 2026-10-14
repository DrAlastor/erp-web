package modulo.ventas_y_facturacion.compartido.repository;

import modulo.ventas_y_facturacion.compartido.entity.Impuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImpuestoRepository extends JpaRepository<Impuesto, Long> {

    Optional<Impuesto> findByCodigoIgnoreCase(String codigo);
}
