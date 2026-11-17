package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.repository;

import jakarta.persistence.LockModeType;
import modulo.ventas_y_facturacion.compartido.entity.CuentaPorCobrar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Acceso de escritura de la CU29 a la cuota por cobrar que crea la CU19 al vender a crédito. */
@Repository
public interface CuentaCobroRepository extends JpaRepository<CuentaPorCobrar, Long> {

    /** Bloquea la cuota mientras se aplica un cobro, para que dos cobros no superen el saldo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CuentaPorCobrar c WHERE c.id = :id")
    Optional<CuentaPorCobrar> findParaCobro(@Param("id") Long id);
}
