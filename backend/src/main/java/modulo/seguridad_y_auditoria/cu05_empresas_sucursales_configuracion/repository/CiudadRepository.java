package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.repository;

import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.entity.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CiudadRepository extends JpaRepository<Ciudad, Integer> {

    List<Ciudad> findAllByOrderByNombreAsc();
}
