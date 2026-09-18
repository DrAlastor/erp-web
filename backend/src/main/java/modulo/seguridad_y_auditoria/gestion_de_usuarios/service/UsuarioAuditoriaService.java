package modulo.seguridad_y_auditoria.gestion_de_usuarios.service;

import lombok.RequiredArgsConstructor;

import modulo.seguridad_y_auditoria.gestion_de_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.repository.BitacoraLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auditoría de las operaciones de la CU-02.
 *
 * <p>Está separado del servicio de cuentas porque lo usan todas las operaciones de escritura
 * y porque es el único punto que conoce la forma del registro de bitácora: si cambia el
 * contrato de {@code bitacora_logs}, cambia acá.
 */
@Service
@RequiredArgsConstructor
public class UsuarioAuditoriaService {

    private final BitacoraLogRepository bitacoraLogRepository;

    /** Registra la acción dentro de la transacción de quien la llama. */
    @Transactional
    public void registrar(String accion, String actor, Long usuarioId, String ip) {
        bitacoraLogRepository.save(new BitacoraLog(accion, actor, usuarioId, ip));
    }
}
