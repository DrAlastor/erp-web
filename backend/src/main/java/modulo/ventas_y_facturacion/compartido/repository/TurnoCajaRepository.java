package modulo.ventas_y_facturacion.compartido.repository;

import jakarta.persistence.LockModeType;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {

    List<TurnoCaja> findByEstadoOrderByFechaAperturaDesc(String estado);

    Optional<TurnoCaja> findFirstBySucursalIdAndCajeroIdAndEstadoOrderByFechaAperturaDesc(
            Integer sucursalId, Long cajeroId, String estado);

    Optional<TurnoCaja> findFirstBySucursalIdAndEstadoOrderByFechaAperturaDesc(Integer sucursalId, String estado);

    /** Turno abierto del cajero en cualquier sucursal (HU-34: un turno abierto por usuario). */
    Optional<TurnoCaja> findFirstByCajeroIdAndEstadoOrderByFechaAperturaDesc(Long cajeroId, String estado);

    /** Bloquea la fila del turno para que movimientos y cierre no se pisen (HU-34 / HU-35). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TurnoCaja t WHERE t.id = :id")
    Optional<TurnoCaja> bloquear(@Param("id") Long id);
}
