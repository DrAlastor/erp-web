package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.repository;

import modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.entity.ParametroSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParametroSistemaRepository extends JpaRepository<ParametroSistema, Integer> {

    List<ParametroSistema> findByEmpresaIdOrderByClaveAsc(UUID empresaId);
}
