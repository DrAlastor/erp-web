package modulo.seguridad_y_auditoria.gestion_de_usuarios.repository;

import modulo.seguridad_y_auditoria.gestion_de_usuarios.entity.BitacoraLog;

import org.springframework.data.jpa.repository.JpaRepository;

/** Bitácora de auditoría de la CU-02. Solo se escribe: la consulta es la CU-04. */
public interface BitacoraLogRepository extends JpaRepository<BitacoraLog, Long> {
}
