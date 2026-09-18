package modulo.seguridad_y_auditoria.roles_y_permisos.repository;

import modulo.seguridad_y_auditoria.roles_y_permisos.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmpresaRepositorio extends JpaRepository<Empresa, UUID> {
}
