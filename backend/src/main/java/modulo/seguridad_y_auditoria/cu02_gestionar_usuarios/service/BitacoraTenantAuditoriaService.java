package modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio transaccional independiente para registrar auditorías locales en bitacora_logs.
 * Al usar Propagation.REQUIRES_NEW, se garantiza que cualquier falla transitoria de auditoría
 * jamás marque la transacción de negocio principal como rollback-only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BitacoraTenantAuditoriaService {

    private final BitacoraLogRepository bitacoraLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, String actor, String entidad, Long entidadId, String ip) {
        try {
            String responsable = (actor != null && !actor.isBlank()) ? actor : "sistema";
            bitacoraLogRepository.save(new BitacoraLog(accion, responsable, entidad, entidadId, ip));
        } catch (Exception e) {
            log.warn("No se pudo persistir registro en bitacora_logs: {}", e.getMessage());
        }
    }
}

