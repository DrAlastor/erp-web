package modulo.ventas_y_facturacion.compartido.repository;

import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PeriodoContableRepository extends JpaRepository<PeriodoContable, Long> {

    Optional<PeriodoContable> findByGestionAndMes(Integer gestion, Integer mes);
}
