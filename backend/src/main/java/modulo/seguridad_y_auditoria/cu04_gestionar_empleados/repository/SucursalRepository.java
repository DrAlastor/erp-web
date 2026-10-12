package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.repository;

import modulo.seguridad_y_auditoria.cu04_gestionar_empleados.entity.Sucursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SucursalRepository extends JpaRepository<Sucursal, Integer> {

    List<Sucursal> findByActivoTrueOrderByNombreAsc();
}
