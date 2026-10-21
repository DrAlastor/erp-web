package modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto;

import java.time.LocalDateTime;

/**
 * Apariencia de la empresa tal como la consume el frontend.
 *
 * <p>El logo no viaja aquí: se pide aparte a {@code /api/seguridad/apariencia/logo}.
 * {@code logoVersion} cambia con cada logo nuevo, para que el navegador no use una copia vieja.
 */
public record AparienciaDto(
        String tema,
        String colorPrimario,
        String colorSecundario,
        String colorAcento,
        String tamanoFuente,
        String tipografia,
        String bordes,
        boolean tieneLogo,
        int logoVersion,
        String encabezadoFactura,
        String pieFactura,
        LocalDateTime fechaModificacion,
        String modificadoPor) {
}
