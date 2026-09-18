package modulo.seguridad_y_auditoria.roles_y_permisos.repositorio;

import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.BitacoraEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BitacoraRepositorio extends JpaRepository<BitacoraEvento, Long> {
}
