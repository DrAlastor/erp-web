package modulo.compras_y_proveedores.compartido.repository;

import jakarta.persistence.LockModeType;
import modulo.compras_y_proveedores.compartido.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("compraCompartidoRepository")
public interface CompraRepository extends JpaRepository<Compra, Long> {

    /** Bloquea la compra para que dos usuarios no generen sus cuotas a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CompraCompartido c WHERE c.id = :id")
    Optional<Compra> findParaActualizar(@Param("id") Long id);
}
