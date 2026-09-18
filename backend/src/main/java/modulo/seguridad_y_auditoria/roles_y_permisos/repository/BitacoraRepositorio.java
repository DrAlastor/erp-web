package modulo.seguridad_y_auditoria.roles_y_permisos.repository;

import modulo.seguridad_y_auditoria.roles_y_permisos.entity.BitacoraEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepositorio extends JpaRepository<BitacoraEvento, Long> {
}
