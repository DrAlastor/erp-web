package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.repository;

import modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.entity.UnidadMedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Integer> {

    List<UnidadMedida> findAllByOrderByNombreAsc();

    List<UnidadMedida> findByEmpresaIdOrEmpresaIdIsNullOrderByNombreAsc(UUID empresaId);

    Optional<UnidadMedida> findByIdAndEmpresaId(Integer id, UUID empresaId);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Integer id);

    boolean existsByAbreviaturaIgnoreCase(String abreviatura);

    boolean existsByAbreviaturaIgnoreCaseAndIdNot(String abreviatura, Integer id);

    boolean existsByCodigoIgnoreCaseAndEmpresaId(String codigo, UUID empresaId);

    boolean existsByCodigoIgnoreCaseAndEmpresaIdAndIdNot(String codigo, UUID empresaId, Integer id);

    boolean existsByAbreviaturaIgnoreCaseAndEmpresaId(String abreviatura, UUID empresaId);

    boolean existsByAbreviaturaIgnoreCaseAndEmpresaIdAndIdNot(String abreviatura, UUID empresaId, Integer id);
}
