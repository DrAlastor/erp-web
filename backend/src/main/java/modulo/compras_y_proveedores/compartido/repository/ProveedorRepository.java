package modulo.compras_y_proveedores.compartido.repository;

import modulo.compras_y_proveedores.compartido.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
}
