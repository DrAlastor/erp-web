package modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.service;

import modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto.AsientoRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reglas de partida doble de la CU27, sin dependencias para poder probarlas solas.
 *
 * <p>Los asientos manuales son los de tipo DIARIO, INGRESO, EGRESO, TRASPASO y AJUSTE.
 * Los que genera el sistema (VENTAS, ANULACION, NOTA_CREDITO, COBRO, REVERSION...) se
 * consultan aquí pero solo se corrigen desde su documento de origen.
 */
public final class ReglasAsiento {

    public static final String BORRADOR = "BORRADOR";
    public static final String MAYORIZADO = "MAYORIZADO";
    public static final String ANULADO = "ANULADO";
    public static final String REVERSION = "REVERSION";

    public static final List<String> TIPOS_MANUALES = List.of("DIARIO", "INGRESO", "EGRESO", "TRASPASO", "AJUSTE");
    public static final Set<String> ESTADOS = Set.of(BORRADOR, MAYORIZADO, ANULADO);

    private ReglasAsiento() {
    }

    /** Totales ya validados de un asiento. */
    public record Totales(BigDecimal debe, BigDecimal haber) {}

    /** Error de regla de negocio: el servicio lo traduce a 422. */
    public static class AsientoInvalido extends RuntimeException {
        public AsientoInvalido(String mensaje) {
            super(mensaje);
        }
    }

    public static boolean esManual(String tipo) {
        return tipo != null && TIPOS_MANUALES.contains(tipo.toUpperCase(Locale.ROOT));
    }

    /** Normaliza el tipo pedido; solo se aceptan los tipos manuales. */
    public static String tipoManual(String tipo) {
        String t = tipo == null ? "" : tipo.trim().toUpperCase(Locale.ROOT);
        if (!TIPOS_MANUALES.contains(t)) {
            throw new AsientoInvalido("Tipo de asiento no válido: " + tipo
                    + ". Use " + String.join(", ", TIPOS_MANUALES) + ".");
        }
        return t;
    }

    /**
     * Valida las líneas: cada una con importe en el debe o en el haber (no en ambos ni en
     * ninguno), al menos una al debe y otra al haber, y debe = haber.
     */
    public static Totales validarLineas(List<AsientoRequest.Linea> lineas) {
        if (lineas == null || lineas.size() < 2) {
            throw new AsientoInvalido("El asiento debe tener al menos dos líneas.");
        }
        BigDecimal debe = BigDecimal.ZERO;
        BigDecimal haber = BigDecimal.ZERO;
        int n = 0;
        for (AsientoRequest.Linea linea : lineas) {
            n++;
            BigDecimal d = escala(linea.debe());
            BigDecimal h = escala(linea.haber());
            if (d.signum() < 0 || h.signum() < 0) {
                throw new AsientoInvalido("Línea " + n + ": los importes no pueden ser negativos.");
            }
            if (d.signum() > 0 && h.signum() > 0) {
                throw new AsientoInvalido("Línea " + n + ": el importe va en el debe o en el haber, no en ambos.");
            }
            if (d.signum() == 0 && h.signum() == 0) {
                throw new AsientoInvalido("Línea " + n + ": falta el importe.");
            }
            debe = debe.add(d);
            haber = haber.add(h);
        }
        if (debe.signum() == 0 || haber.signum() == 0) {
            throw new AsientoInvalido("El asiento necesita al menos una línea al debe y otra al haber.");
        }
        if (debe.compareTo(haber) != 0) {
            throw new AsientoInvalido("El asiento no cuadra: debe " + debe.toPlainString()
                    + " / haber " + haber.toPlainString() + " (diferencia "
                    + debe.subtract(haber).abs().toPlainString() + ").");
        }
        return new Totales(debe, haber);
    }

    public static BigDecimal escala(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
