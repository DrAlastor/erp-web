package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.repository;

import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.entity.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlmacenMaestroRepository extends JpaRepository<Almacen, Integer> {

    List<Almacen> findAllByOrderByEsPrincipalDescNombreAsc();

    List<Almacen> findByEmpresaIdOrEmpresaIdIsNullOrderByEsPrincipalDescNombreAsc(UUID empresaId);

    List<Almacen> findBySucursalId(Integer sucursalId);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Integer id);

    boolean existsByCodigoIgnoreCaseAndEmpresaId(String codigo, UUID empresaId);

    boolean existsByCodigoIgnoreCaseAndEmpresaIdAndIdNot(String codigo, UUID empresaId, Integer id);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);

    boolean existsByNombreIgnoreCaseAndEmpresaId(String nombre, UUID empresaId);

    boolean existsByNombreIgnoreCaseAndEmpresaIdAndIdNot(String nombre, UUID empresaId, Integer id);
}
