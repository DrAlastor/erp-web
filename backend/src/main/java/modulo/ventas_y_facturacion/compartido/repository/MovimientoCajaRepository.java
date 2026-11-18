package modulo.ventas_y_facturacion.compartido.repository;

import modulo.ventas_y_facturacion.compartido.entity.MovimientoCaja;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    List<MovimientoCaja> findByTurnoCajaIdOrderByFechaHoraAsc(Long turnoCajaId);

    /** Movimientos del turno, del más reciente al más antiguo (HU-34). */
    @EntityGraph(attributePaths = {"formaPago"})
    List<MovimientoCaja> findByTurnoCajaIdOrderByFechaHoraDescIdDesc(Long turnoCajaId);

    long countByTurnoCajaId(Long turnoCajaId);
}
