package modulo.inventario_y_almacenes.movimientos_de_inventario.repository;

import modulo.inventario_y_almacenes.movimientos_de_inventario.entity.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlmacenRepository extends JpaRepository<Almacen, Integer> {
    List<Almacen> findByActivoTrue();
    Optional<Almacen> findByEsPrincipalTrue();
}
