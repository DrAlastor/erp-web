package modulo.compras_y_proveedores.cu23_ordenes_de_compra.repository;

import modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity.EstadoOrdenCompra;
import modulo.compras_y_proveedores.cu23_ordenes_de_compra.entity.OrdenCompra;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Integer> {

    @EntityGraph(attributePaths = {"proveedor"})
    @Query("select o from OrdenCompra o "
            + "where (:estado is null or o.estado = :estado) "
            + "and (:proveedorId is null or o.proveedor.id = :proveedorId) "
            + "and o.fecha >= :desde and o.fecha <= :hasta "
            + "and (:texto = '' or lower(o.numeroOrden) like lower(concat('%', :texto, '%')) "
            + "     or lower(o.proveedor.razonSocial) like lower(concat('%', :texto, '%')) "
            + "     or o.proveedor.nitCi like concat('%', :texto, '%'))")
    Page<OrdenCompra> buscar(@Param("texto") String texto,
                             @Param("estado") EstadoOrdenCompra estado,
                             @Param("proveedorId") Integer proveedorId,
                             @Param("desde") LocalDate desde,
                             @Param("hasta") LocalDate hasta,
                             Pageable pagina);

    @EntityGraph(attributePaths = {"proveedor", "detalles", "detalles.producto"})
    @Query("select o from OrdenCompra o where o.id = :id")
    Optional<OrdenCompra> buscarCompleta(@Param("id") Integer id);

    /**
     * Serializa la numeración: dos altas simultáneas esperan su turno en vez de calcular el
     * mismo correlativo. El candado se libera solo al terminar la transacción.
     */
    @Query(value = "select 1 from (select pg_advisory_xact_lock(7210022)) as candado", nativeQuery = true)
    Integer bloquearNumeracion();

    /** Mayor correlativo usado con el prefijo dado (por ejemplo 'OC-2026-'), o 0. */
    @Query(value = "select coalesce(max(cast(substring(numero_orden from char_length(:prefijo) + 1) as integer)), 0) "
            + "from ordenes_compra where numero_orden like concat(:prefijo, '%') "
            + "and substring(numero_orden from char_length(:prefijo) + 1) ~ '^[0-9]+$'", nativeQuery = true)
    Integer ultimoCorrelativo(@Param("prefijo") String prefijo);

    /**
     * Productos de la sucursal con stock por debajo del mínimo: id, código, nombre,
     * disponible, mínimo y costo de la última compra (o el costo del catálogo).
     */
    @Query(value = "select p.id, p.codigo, p.nombre, i.stock_disponible, i.stock_minimo, "
            + "coalesce((select cd.costo_unitario from compra_detalles cd join compras c on c.id = cd.compra_id "
            + "          where cd.producto_id = p.id and c.estado <> 'ANULADA' "
            + "          order by c.fecha desc, cd.id desc limit 1), p.costo_compra) "
            + "from inventario_sucursal i join productos p on p.id = i.producto_id "
            + "where i.sucursal_id = :sucursalId and p.activo = true and i.stock_disponible < i.stock_minimo "
            + "order by p.nombre", nativeQuery = true)
    List<Object[]> bajoStockMinimo(@Param("sucursalId") Integer sucursalId);

    /** Productos activos con el costo de su última compra: id, código, nombre, costo. */
    @Query(value = "select p.id, p.codigo, p.nombre, "
            + "coalesce((select cd.costo_unitario from compra_detalles cd join compras c on c.id = cd.compra_id "
            + "          where cd.producto_id = p.id and c.estado <> 'ANULADA' "
            + "          order by c.fecha desc, cd.id desc limit 1), p.costo_compra) "
            + "from productos p where p.activo = true order by p.nombre", nativeQuery = true)
    List<Object[]> productosConUltimoCosto();
}
