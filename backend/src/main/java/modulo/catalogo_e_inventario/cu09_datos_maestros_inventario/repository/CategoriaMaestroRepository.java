package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.repository;

import modulo.catalogo_e_inventario.compartido.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoriaMaestroRepository extends JpaRepository<Categoria, Integer> {

    List<Categoria> findAllByOrderByNombreAsc();

    List<Categoria> findByEmpresaIdOrEmpresaIdIsNullOrderByNombreAsc(UUID empresaId);

    List<Categoria> findByPadreId(Integer padreId);

    long countByPadreId(Integer padreId);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);

    boolean existsByNombreIgnoreCaseAndEmpresaId(String nombre, UUID empresaId);

    boolean existsByNombreIgnoreCaseAndEmpresaIdAndIdNot(String nombre, UUID empresaId, Integer id);
}
