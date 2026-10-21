package modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto.AparienciaDto;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto.LogoArchivo;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto.Peticiones;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.service.ServicioApariencia;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

/**
 * CU-06 Personalizar Apariencia.
 *
 * <p>Cualquier usuario autenticado lee la apariencia, porque todos la ven aplicada en el
 * panel. Cambiarla exige {@code SEGURIDAD_MODIFICAR}, que en la matriz de la HU-03 tiene
 * solo el Administrador.
 */
@RestController
@RequestMapping("/api/seguridad/apariencia")
public class ControladorApariencia {

    private final ServicioApariencia servicio;

    public ControladorApariencia(ServicioApariencia servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public AparienciaDto obtener() {
        return servicio.obtener();
    }

    @PutMapping
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
    public AparienciaDto actualizar(@Valid @RequestBody Peticiones.Actualizar cuerpo,
                                    Principal actor, HttpServletRequest peticion) {
        return servicio.actualizar(cuerpo, nombre(actor), peticion.getRemoteAddr());
    }

    /** El logo exige sesión; el frontend lo pide con su token y lo muestra como blob. */
    @GetMapping("/logo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> logo() {
        LogoArchivo logo = servicio.logo();
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.tipo()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .eTag("\"logo-" + logo.version() + "\"")
                .header("X-Content-Type-Options", "nosniff");
        if ("image/svg+xml".equals(logo.tipo())) {
            // Un SVG abierto directamente no puede ejecutar nada ni cargar recursos externos.
            respuesta.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox");
        }
        return respuesta.body(logo.contenido());
    }

    @PostMapping(path = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
    public AparienciaDto subirLogo(@RequestParam("archivo") MultipartFile archivo,
                                   Principal actor, HttpServletRequest peticion) {
        return servicio.subirLogo(archivo, nombre(actor), peticion.getRemoteAddr());
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
    public AparienciaDto eliminarLogo(Principal actor, HttpServletRequest peticion) {
        return servicio.eliminarLogo(nombre(actor), peticion.getRemoteAddr());
    }

    @PostMapping("/restablecer")
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
    public AparienciaDto restablecer(Principal actor, HttpServletRequest peticion) {
        return servicio.restablecer(nombre(actor), peticion.getRemoteAddr());
    }

    private static String nombre(Principal actor) {
        return actor != null ? actor.getName() : "desconocido";
    }
}
