package modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository;

import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlmacenRepository extends JpaRepository<Almacen, Integer> {
    List<Almacen> findByActivoTrue();
}
