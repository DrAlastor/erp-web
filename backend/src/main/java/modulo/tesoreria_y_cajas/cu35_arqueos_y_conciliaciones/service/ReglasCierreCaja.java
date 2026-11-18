package modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.service;

import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.CalculadoraArqueo;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/**
 * Reglas puras del cierre de caja (HU-35), sin base de datos.
 *
 * <p>Diferencia = contado − esperado (positivo sobrante, negativo faltante). Si la diferencia
 * no es cero las observaciones son obligatorias (400). Las observaciones de cierre se
 * agregan a las de apertura para no perderlas.
 */
public final class ReglasCierreCaja {

    private ReglasCierreCaja() {}

    public record ResultadoCierre(BigDecimal montoContado, BigDecimal diferencia, String observaciones) {}

    public static ResultadoCierre evaluar(BigDecimal efectivoEsperado, BigDecimal montoContado, String observaciones) {
        if (montoContado == null || montoContado.signum() < 0) {
            throw invalido("El monto contado es obligatorio y no puede ser negativo");
        }
        BigDecimal contado = CalculadoraArqueo.escala(montoContado);
        BigDecimal diferencia = CalculadoraArqueo.diferencia(contado, efectivoEsperado);
        String obs = observaciones == null || observaciones.isBlank() ? null : observaciones.trim();
        if (diferencia.signum() != 0 && obs == null) {
            throw invalido("Hay un " + (diferencia.signum() > 0 ? "sobrante" : "faltante") + " de Bs "
                    + diferencia.abs().toPlainString() + ": las observaciones son obligatorias para cerrar");
        }
        return new ResultadoCierre(contado, diferencia, obs);
    }

    public static String descripcion(BigDecimal diferencia) {
        if (diferencia.signum() == 0) return "cuadrada";
        return (diferencia.signum() > 0 ? "sobrante" : "faltante") + " de Bs " + diferencia.abs().toPlainString();
    }

    /** "apertura | Cierre: cierre", o la que exista. */
    public static String observacionesFinales(String apertura, String cierre) {
        boolean hayApertura = apertura != null && !apertura.isBlank();
        if (cierre == null || cierre.isBlank()) return hayApertura ? apertura : null;
        return hayApertura ? apertura.trim() + " | Cierre: " + cierre.trim() : "Cierre: " + cierre.trim();
    }

    private static ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
