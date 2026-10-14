package modulo.ventas_y_facturacion.compartido.repository;

import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

    List<TurnoCaja> findByEstadoOrderByFechaAperturaDesc(String estado);

    Optional<TurnoCaja> findFirstBySucursalIdAndCajeroIdAndEstadoOrderByFechaAperturaDesc(
            Integer sucursalId, Long cajeroId, String estado);

    Optional<TurnoCaja> findFirstBySucursalIdAndEstadoOrderByFechaAperturaDesc(Integer sucursalId, String estado);
}
