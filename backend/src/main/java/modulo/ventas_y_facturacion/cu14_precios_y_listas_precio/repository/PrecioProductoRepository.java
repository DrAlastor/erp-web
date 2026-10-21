package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.repository;

import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.entity.PrecioProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrecioProductoRepository extends JpaRepository<PrecioProducto, Integer> {

    List<PrecioProducto> findByListaPrecioId(Integer listaPrecioId);

    Optional<PrecioProducto> findByListaPrecioIdAndProductoId(Integer listaPrecioId, Long productoId);

    @Query("SELECT COUNT(p) FROM PrecioProducto p WHERE p.listaPrecio.id = :listaId")
    long countByListaPrecioId(@Param("listaId") Integer listaId);

    void deleteByListaPrecioId(Integer listaPrecioId);
}
