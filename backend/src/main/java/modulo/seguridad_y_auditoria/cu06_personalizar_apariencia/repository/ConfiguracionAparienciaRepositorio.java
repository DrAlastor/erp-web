package modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.repository;

import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.entity.ConfiguracionApariencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionAparienciaRepositorio extends JpaRepository<ConfiguracionApariencia, Integer> {

    /** La tabla tiene una sola fila por base de empresa; si hubiera más, manda la primera. */
    Optional<ConfiguracionApariencia> findFirstByOrderByIdAsc();
}
