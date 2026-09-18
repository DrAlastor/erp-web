package com.uagrm.erp.backend.seguridad.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cuerpos de respuesta del módulo de seguridad. Están en un solo lugar para que el cliente
 * Angular reciba siempre la misma forma, venga la negación de la cadena de filtros o de una
 * anotación {@code @PreAuthorize}.
 */
public final class RespuestasDeSeguridad {

    public static final String ACCESO_DENEGADO = "ACCESO_DENEGADO";
    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";

    public static final String MENSAJE_ACCESO_DENEGADO = "No tiene permiso para realizar esta operación";
    public static final String MENSAJE_NO_AUTENTICADO = "Necesita iniciar sesión para continuar";

    private static final ObjectMapper JSON = new ObjectMapper();

    private RespuestasDeSeguridad() {
    }

    /** Cuerpo de un acceso denegado, con el permiso que hacía falta si se lo conoce. */
    public static Map<String, Object> accesoDenegado(String permisoRequerido) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", ACCESO_DENEGADO);
        cuerpo.put("mensaje", MENSAJE_ACCESO_DENEGADO);
        if (permisoRequerido != null && !permisoRequerido.isBlank()) {
            cuerpo.put("permisoRequerido", permisoRequerido);
        }
        return cuerpo;
    }

    /** Cuerpo de una petición sin autenticar. */
    public static Map<String, Object> noAutenticado() {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", NO_AUTENTICADO);
        cuerpo.put("mensaje", MENSAJE_NO_AUTENTICADO);
        return cuerpo;
    }

    /** Escribe un cuerpo JSON con el estado indicado. */
    public static void escribir(HttpServletResponse respuesta, int estado, Map<String, Object> cuerpo)
            throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        JSON.writeValue(respuesta.getOutputStream(), cuerpo);
    }
}
