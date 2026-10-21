package modulo.compras_y_proveedores.cu21_gestionar_proveedores.repository;

import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.Proveedor;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository("proveedorCu21Repository")
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    @Query("select p from Proveedor p "
            + "where (:activo is null or p.activo = :activo) "
            + "and (:ciudad = '' or lower(p.ciudad) = lower(:ciudad)) "
            + "and (:texto = '' or lower(p.razonSocial) like lower(concat('%', :texto, '%')) "
            + "     or p.nitCi like concat('%', :texto, '%'))")
    Page<Proveedor> buscar(@Param("texto") String texto,
                           @Param("activo") Boolean activo,
                           @Param("ciudad") String ciudad,
                           Pageable pagina);

    List<Proveedor> findByActivoTrueOrderByRazonSocialAsc();

    /**
     * Lee el proveedor bloqueándolo para escribir (SELECT ... FOR UPDATE): dos cambios de
     * estado simultáneos esperan su turno en vez de pisarse.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Proveedor p where p.id = :id")
    Optional<Proveedor> bloquear(@Param("id") Integer id);

    /**
     * Lee el proveedor con un bloqueo compartido (FOR SHARE): mientras se emite o aprueba
     * una orden, nadie puede desactivarlo a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from Proveedor p where p.id = :id")
    Optional<Proveedor> compartir(@Param("id") Integer id);

    boolean existsByNitCi(String nitCi);

    boolean existsByNitCiAndIdNot(String nitCi, Integer id);

    @Query("select distinct p.ciudad from Proveedor p where p.ciudad is not null and p.ciudad <> '' order by p.ciudad")
    List<String> ciudades();

    /** Órdenes que todavía comprometen al proveedor: impiden desactivarlo. */
    @Query(value = "select count(*) from ordenes_compra "
            + "where proveedor_id = :id and estado in ('PENDIENTE', 'APROBADA', 'ENVIADA')", nativeQuery = true)
    long contarOrdenesAbiertas(@Param("id") Integer id);

    /** Lo que se le sigue debiendo al proveedor en cuentas por pagar. */
    @Query(value = "select coalesce(sum(saldo), 0) from cuentas_por_pagar "
            + "where proveedor_id = :id and saldo > 0", nativeQuery = true)
    BigDecimal saldoPendiente(@Param("id") Integer id);

    /** Últimas órdenes de compra: id, número, fecha, total, estado. */
    @Query(value = "select id, numero_orden, fecha, total, estado from ordenes_compra "
            + "where proveedor_id = :id order by fecha desc, id desc limit 50", nativeQuery = true)
    List<Object[]> ultimasOrdenes(@Param("id") Integer id);

    /** Últimas compras: id, número de factura, fecha, total, estado. */
    @Query(value = "select id, numero_factura, fecha, total, estado from compras "
            + "where proveedor_id = :id order by fecha desc, id desc limit 50", nativeQuery = true)
    List<Object[]> ultimasCompras(@Param("id") Integer id);
}
