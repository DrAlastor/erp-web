package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.repository;

import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    boolean existsByCi(String ci);

    boolean existsByCiAndIdNot(String ci, Long id);

    Optional<Empleado> findByCi(String ci);

    @Query("""
        SELECT e FROM Empleado e
        WHERE (:sucursalId IS NULL OR e.sucursalId = :sucursalId)
          AND (:cargo IS NULL OR LOWER(e.cargo) LIKE LOWER(CONCAT('%', :cargo, '%')))
          AND (:activo IS NULL OR e.activo = :activo)
          AND (
            :search IS NULL OR :search = ''
            OR LOWER(e.ci) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(e.apellido) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        ORDER BY e.id ASC
    """)
    List<Empleado> buscarConFiltros(
            @Param("sucursalId") Integer sucursalId,
            @Param("cargo") String cargo,
            @Param("activo") Boolean activo,
            @Param("search") String search
    );
}
