package modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.mapper;

import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto.AparienciaDto;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.entity.ConfiguracionApariencia;
import org.springframework.stereotype.Component;

/**
 * Convierte la configuración guardada en lo que ve el frontend.
 *
 * <p>Las columnas en {@code null} (por ejemplo, la fila que siembra el script de la base)
 * se completan con la marca NexoERP, así el frontend siempre recibe una apariencia completa.
 */
@Component
public class AparienciaMapper {

    public static final String TEMA = "CLARO";
    public static final String COLOR_PRIMARIO = "#007AFF";
    public static final String COLOR_SECUNDARIO = "#1D1D1F";
    public static final String COLOR_ACENTO = "#34C759";
    public static final String TAMANO_FUENTE = "NORMAL";
    public static final String TIPOGRAFIA = "INTER";
    public static final String BORDES = "SUAVES";

    public AparienciaDto toDto(ConfiguracionApariencia c) {
        if (c == null) {
            return porDefecto();
        }
        return new AparienciaDto(
                valorValido(c.getTema(), TEMA, "CLARO", "OSCURO"),
                colorValido(c.getColorPrimario(), COLOR_PRIMARIO),
                colorValido(c.getColorSecundario(), COLOR_SECUNDARIO),
                colorValido(c.getColorAcento(), COLOR_ACENTO),
                valorValido(c.getTamanoFuente(), TAMANO_FUENTE, "PEQUENO", "NORMAL", "GRANDE"),
                valorValido(c.getTipografia(), TIPOGRAFIA, "INTER", "ROBOTO", "POPPINS", "OPEN_SANS"),
                valorValido(c.getBordes(), BORDES, "RECTOS", "SUAVES", "REDONDEADOS"),
                c.tieneLogo(),
                c.getLogoVersion(),
                c.getEncabezadoFactura(),
                c.getPieFactura(),
                c.getFechaModificacion(),
                c.getModificadoPor());
    }

    /** La marca NexoERP, para una empresa que todavía no personalizó nada. */
    public AparienciaDto porDefecto() {
        return new AparienciaDto(TEMA, COLOR_PRIMARIO, COLOR_SECUNDARIO, COLOR_ACENTO,
                TAMANO_FUENTE, TIPOGRAFIA, BORDES, false, 0, null, null, null, null);
    }

    /** Vuelve la fila a la marca NexoERP. No toca el encabezado ni el pie de factura. */
    public void aplicarPorDefecto(ConfiguracionApariencia c) {
        c.setTema(TEMA);
        c.setColorPrimario(COLOR_PRIMARIO);
        c.setColorSecundario(COLOR_SECUNDARIO);
        c.setColorAcento(COLOR_ACENTO);
        c.setTamanoFuente(TAMANO_FUENTE);
        c.setTipografia(TIPOGRAFIA);
        c.setBordes(BORDES);
    }

    private String colorValido(String valor, String porDefecto) {
        return valor != null && valor.matches("^#[0-9A-Fa-f]{6}$") ? valor.toUpperCase() : porDefecto;
    }

    private String valorValido(String valor, String porDefecto, String... permitidos) {
        if (valor == null) return porDefecto;
        String normalizado = valor.trim().toUpperCase();
        for (String permitido : permitidos) {
            if (permitido.equals(normalizado)) return permitido;
        }
        return porDefecto;
    }
}
