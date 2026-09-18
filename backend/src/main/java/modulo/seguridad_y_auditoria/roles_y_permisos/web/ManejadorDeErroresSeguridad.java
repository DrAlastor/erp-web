package modulo.seguridad_y_auditoria.roles_y_permisos.web;

import modulo.seguridad_y_auditoria.roles_y_permisos.error.RecursoNoEncontrado;
import modulo.seguridad_y_auditoria.roles_y_permisos.error.ReglaDeNegocio;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce los errores del módulo de seguridad a respuestas HTTP con una forma estable.
 *
 * <p>El acceso denegado que emite una anotación {@code @PreAuthorize} pasa por acá y
 * devuelve exactamente el mismo cuerpo que el que emite la cadena de filtros, así el
 * frontend muestra un solo mensaje de "acceso denegado" sin importar dónde se cortó.
 */
@org.springframework.core.annotation.Order(0)
@RestControllerAdvice(basePackages = "modulo.seguridad_y_auditoria.roles_y_permisos.web")
public class ManejadorDeErroresSeguridad {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accesoDenegado(AccessDeniedException excepcion) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(RespuestasDeSeguridad.accesoDenegado(null));
    }

    @ExceptionHandler(RecursoNoEncontrado.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontrado excepcion) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(cuerpo(excepcion.getCodigo(), excepcion.getMessage()));
    }

    @ExceptionHandler(ReglaDeNegocio.class)
    public ResponseEntity<Map<String, Object>> reglaDeNegocio(ReglaDeNegocio excepcion) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(cuerpo(excepcion.getCodigo(), excepcion.getMessage()));
    }

    private Map<String, Object> cuerpo(String codigo, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("error", codigo);
        cuerpo.put("mensaje", mensaje);
        return cuerpo;
    }
}
