package modulo.notificaciones.controller;

import lombok.RequiredArgsConstructor;
import modulo.notificaciones.dto.NotificacionResponse;
import modulo.notificaciones.service.NotificacionService;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioActual;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar() {
        Long usuarioId = obtenerUsuarioIdActual();
        return ResponseEntity.ok(notificacionService.listar(usuarioId));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id) {
        notificacionService.marcarLeida(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/marcar-todas")
    public ResponseEntity<Void> marcarTodasLeidas() {
        Long usuarioId = obtenerUsuarioIdActual();
        notificacionService.marcarTodasLeidas(usuarioId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        notificacionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/evento-password")
    public ResponseEntity<Void> notificarCambioPassword(@RequestBody(required = false) Map<String, Object> body) {
        Long usuarioId = obtenerUsuarioIdActual();
        String detalle = "Se ha actualizado la contraseña de acceso al sistema exitosamente.";
        if (body != null && body.containsKey("email")) {
            detalle += " Cuenta: " + body.get("email");
        }
        notificacionService.notificar(usuarioId, "Cambio de Contraseña", detalle, "INFO");
        return ResponseEntity.ok().build();
    }

    private Long obtenerUsuarioIdActual() {
        try {
            var principal = UsuarioActual.obtener();
            return (principal != null && principal.usuarioId() != null) ? principal.usuarioId() : 1L;
        } catch (Exception e) {
            return 1L;
        }
    }
}
