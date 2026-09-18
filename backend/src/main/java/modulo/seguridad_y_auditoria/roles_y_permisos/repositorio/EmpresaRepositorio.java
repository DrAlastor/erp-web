package modulo.seguridad_y_auditoria.roles_y_permisos.repositorio;

import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmpresaRepositorio extends JpaRepository<Empresa, UUID> {
}
