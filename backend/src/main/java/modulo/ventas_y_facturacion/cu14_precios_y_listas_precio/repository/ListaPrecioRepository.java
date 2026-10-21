package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.repository;

import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.entity.ListaPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListaPrecioRepository extends JpaRepository<ListaPrecio, Integer> {

    Optional<ListaPrecio> findByCodigo(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Integer id);

    Optional<ListaPrecio> findFirstByEsPrincipalTrueAndActivoTrue();

    List<ListaPrecio> findAllByOrderByEsPrincipalDescNombreAsc();

    @Modifying
    @Query("UPDATE ListaPrecio l SET l.esPrincipal = false WHERE l.id <> :id")
    int desmarcarOtrasComoPrincipal(@Param("id") Integer id);
}
