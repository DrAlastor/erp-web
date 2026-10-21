package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.repository;

import jakarta.persistence.LockModeType;
import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.entity.CuentaPorPagar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaPorPagarRepository extends JpaRepository<CuentaPorPagar, Long> {

    /** Bloqueo pesimista: dos pagos simultaneos no pueden dejar el saldo negativo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CuentaPorPagar c WHERE c.id = :id")
    Optional<CuentaPorPagar> findParaPago(@Param("id") Long id);

    boolean existsByCompraId(Long compraId);
}
