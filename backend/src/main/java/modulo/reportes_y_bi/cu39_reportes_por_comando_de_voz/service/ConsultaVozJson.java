package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.service;

import com.fasterxml.jackson.databind.JsonNode;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.AgrupacionVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.MetricaVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ConsultaReporteVoz;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/**
 * Convierte el JSON que propone la IA (los argumentos de la herramienta) en una
 * {@link ConsultaReporteVoz}. Ese JSON es dato no confiable: solo se aceptan valores del
 * catálogo y tipos estrictos; lo demás se descarta.
 */
final class ConsultaVozJson {

    private ConsultaVozJson() {
    }

    /** Vacío si no hay una métrica válida del catálogo. */
    static Optional<ConsultaReporteVoz> convertir(JsonNode nodo) {
        if (nodo == null || !nodo.isObject()) {
            return Optional.empty();
        }
        MetricaVoz metrica = enumerado(MetricaVoz.class, texto(nodo.get("metrica")));
        if (metrica == null) {
            return Optional.empty();
        }
        Long limite = entero(nodo.get("limite"));
        return Optional.of(ConsultaReporteVoz.builder()
                .metrica(metrica)
                .agrupacion(enumerado(AgrupacionVoz.class, texto(nodo.get("agrupacion"))))
                .fechaInicio(fecha(texto(nodo.get("fechaInicio"))))
                .fechaFin(fecha(texto(nodo.get("fechaFin"))))
                .sucursalId(entero(nodo.get("sucursalId")))
                .productoId(entero(nodo.get("productoId")))
                .soloCriticos(nodo.path("soloCriticos").isBoolean() ? nodo.get("soloCriticos").asBoolean() : null)
                .limite(limite == null ? null : (int) Math.min(Math.max(limite, 1), 100))
                .build());
    }

    private static String texto(JsonNode valor) {
        return valor != null && valor.isTextual() ? valor.asText() : null;
    }

    private static <E extends Enum<E>> E enumerado(Class<E> tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDate fecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        try {
            return LocalDate.parse(limpio.length() > 10 ? limpio.substring(0, 10) : limpio);
        } catch (Exception e) {
            return null;
        }
    }

    /** Acepta 1 y 1.0 (la IA a veces envía enteros como decimales); rechaza texto. */
    private static Long entero(JsonNode valor) {
        if (valor == null || !valor.isNumber()) {
            return null;
        }
        double d = valor.asDouble();
        return d == Math.rint(d) ? (long) d : null;
    }
}
