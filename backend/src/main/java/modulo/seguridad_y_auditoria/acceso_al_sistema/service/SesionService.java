package modulo.seguridad_y_auditoria.acceso_al_sistema.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.SesionRepository;
@Service @RequiredArgsConstructor
public class SesionService {
    private final SesionRepository sesiones;
    @Transactional public void cerrarActivas(Long usuarioId) {
        sesiones.cerrarActivas(usuarioId, java.time.LocalDateTime.now());
    }
}
