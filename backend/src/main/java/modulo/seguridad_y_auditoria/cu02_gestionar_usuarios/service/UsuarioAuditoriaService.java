package modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.service;

import lombok.RequiredArgsConstructor;

import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;

import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService;
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
    private final BitacoraAuditoriaService bitacoraAuditoriaService;

    /** Registra la acción dentro de la transacción de quien la llama. */
    @Transactional
    public void registrar(String accion, String actor, Long usuarioId, String ip) {
        try {
            bitacoraLogRepository.save(new BitacoraLog(accion, actor, usuarioId, ip));
        } catch (Exception ignored) {
            // No interrumpir la operación principal de la cuenta si la tabla de bitácora no está disponible
        }

        try {
            String severidad = (accion != null && (accion.contains("DESACTIVADO") || accion.contains("ELIMINADO") || accion.contains("BLOQUEADO")))
                    ? "ALTA"
                    : "MEDIA";

            bitacoraAuditoriaService.registrarEventoActual(
                    accion != null ? accion : "ACCION_USUARIO",
                    "SEGURIDAD",
                    String.format("Operación %s sobre el usuario ID #%d efectuada por '%s'.", accion, usuarioId, actor),
                    "EXITO",
                    severidad,
                    String.format("{\"usuarioId\":%d,\"actor\":\"%s\",\"ip\":\"%s\"}", usuarioId, actor, ip != null ? ip : "127.0.0.1")
            );
        } catch (Exception ignored) {}
    }
}
