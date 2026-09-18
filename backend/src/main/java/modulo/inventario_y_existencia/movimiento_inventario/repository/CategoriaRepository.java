package modulo.inventario_y_existencia.movimiento_inventario.repository;

import modulo.inventario_y_existencia.movimiento_inventario.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    Optional<Categoria> findByNombreIgnoreCase(String nombre);
    List<Categoria> findByActivoTrue();
}
