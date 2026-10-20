package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.repository;

import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.entity.EmpresaPerfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmpresaPerfilRepository extends JpaRepository<EmpresaPerfil, UUID> {

    boolean existsByNitAndIdNot(String nit, UUID id);
}
