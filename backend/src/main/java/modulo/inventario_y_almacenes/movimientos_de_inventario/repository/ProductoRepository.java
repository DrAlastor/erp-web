package modulo.inventario_y_almacenes.movimientos_de_inventario.repository;

import modulo.inventario_y_almacenes.movimientos_de_inventario.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findByCodigoSku(String codigoSku);
    Optional<Producto> findByCodigoBarra(String codigoBarra);
    List<Producto> findByActivoTrue();

    @Query("SELECT p FROM Producto p WHERE p.activo = true AND " +
           "(LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.codigoSku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.codigoBarra) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Producto> searchProductos(@Param("search") String search, Pageable pageable);
}
