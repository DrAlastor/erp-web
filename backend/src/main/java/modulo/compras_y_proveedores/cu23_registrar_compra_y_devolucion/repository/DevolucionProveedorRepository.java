package modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.repository;

import modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.entity.DevolucionProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DevolucionProveedorRepository extends JpaRepository<DevolucionProveedor, Integer> {

    List<DevolucionProveedor> findByCompraIdOrderByIdDesc(Integer compraId);

    List<DevolucionProveedor> findAllByOrderByIdDesc();

    boolean existsByCompraId(Integer compraId);

    /** Cantidad ya devuelta de una linea de compra. */
    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DevolucionDetalle d WHERE d.compraDetalleId = :detalleId")
    BigDecimal cantidadDevuelta(@Param("detalleId") Integer detalleId);
}
