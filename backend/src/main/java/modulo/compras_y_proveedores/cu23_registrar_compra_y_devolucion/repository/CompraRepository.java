package modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.repository;

import modulo.compras_y_proveedores.cu23_registrar_compra_y_devolucion.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Integer> {

    boolean existsByProveedorIdAndNumeroFacturaIgnoreCaseAndEstadoNot(Integer proveedorId, String numeroFactura, String estado);

    List<Compra> findAllByOrderByFechaDescIdDesc();
}
