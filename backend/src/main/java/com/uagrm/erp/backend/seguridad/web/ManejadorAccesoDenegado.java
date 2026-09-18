package com.uagrm.erp.backend.seguridad.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 403 en JSON cuando la cadena de filtros niega el acceso.
 * Es el "acceso denegado" que muestra el frontend, con la misma forma que el que emite el
 * manejador de excepciones para las negaciones de {@code @PreAuthorize}.
 */
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
                       AccessDeniedException excepcion) throws IOException {
        RespuestasDeSeguridad.escribir(respuesta, HttpStatus.FORBIDDEN.value(),
                RespuestasDeSeguridad.accesoDenegado(null));
    }
}
