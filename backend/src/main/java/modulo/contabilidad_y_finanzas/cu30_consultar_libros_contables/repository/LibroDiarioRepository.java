package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.repository;

import modulo.ventas_y_facturacion.compartido.entity.AsientoContable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibroDiarioRepository extends JpaRepository<AsientoContable, Long>, JpaSpecificationExecutor<AsientoContable> {

    @Override
    @EntityGraph(attributePaths = {"detalles", "detalles.cuentaContable"})
    List<AsientoContable> findAll(Specification<AsientoContable> spec);
}
