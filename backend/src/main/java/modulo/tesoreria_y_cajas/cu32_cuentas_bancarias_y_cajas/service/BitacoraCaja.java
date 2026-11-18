package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service;

import lombok.RequiredArgsConstructor;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService;
import org.springframework.stereotype.Component;

/**
 * Registra en la bitácora (CU08) la apertura, los movimientos y el cierre de caja con módulo
 * CAJA. Al registrar el evento enriquecido, el AuditoriaGlobalInterceptor no lo duplica.
 * Un fallo de la bitácora nunca interrumpe la operación de caja.
 */
@Component
@RequiredArgsConstructor
public class BitacoraCaja {

    private static final String MODULO = "CAJA";

    private final BitacoraAuditoriaService bitacora;

    public void registrar(String tipoOperacion, String detalle, String metadatosJson) {
        try {
            bitacora.registrarEventoActual(tipoOperacion, MODULO, detalle, "EXITO", "MEDIA", metadatosJson);
        } catch (Exception ignored) {
            // La bitácora es best-effort, igual que en VentaService (CU19).
        }
    }
}
