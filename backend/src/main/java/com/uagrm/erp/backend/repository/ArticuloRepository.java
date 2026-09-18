package com.uagrm.erp.backend.repository;

import com.uagrm.erp.backend.entity.Articulo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    @Query("SELECT a FROM Articulo a LEFT JOIN FETCH a.categoria")
    List<Articulo> findAllWithCategoria();

    @Query("SELECT a FROM Articulo a LEFT JOIN FETCH a.categoria WHERE a.id = :id")
    Optional<Articulo> findByIdWithCategoria(@Param("id") Long id);

    @Query("SELECT a FROM Articulo a LEFT JOIN FETCH a.categoria WHERE a.categoria.id = :categoriaId")
    List<Articulo> findByCategoriaIdWithCategoria(@Param("categoriaId") Long categoriaId);
}
