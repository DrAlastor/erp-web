package modulo.notificaciones.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.notificaciones.dto.NotificacionResponse;
import modulo.notificaciones.entity.Notificacion;
import modulo.notificaciones.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(Long usuarioId) {
        List<Notificacion> list;
        if (usuarioId != null && usuarioId > 0) {
            // Obtener notificaciones del usuario y del sistema (usuarioId = 1)
            list = notificacionRepository.findAllByOrderByFechaCreacionDesc();
        } else {
            list = notificacionRepository.findAllByOrderByFechaCreacionDesc();
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void marcarLeida(Long id) {
        notificacionRepository.marcarLeida(id);
    }

    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        if (usuarioId != null && usuarioId > 0) {
            notificacionRepository.marcarTodasLeidas();
        } else {
            notificacionRepository.marcarTodasLeidas();
        }
    }

    @Transactional
    public void eliminar(Long id) {
        if (notificacionRepository.existsById(id)) {
            notificacionRepository.deleteById(id);
        }
    }

    @Transactional
    public Notificacion notificar(Long usuarioId, String titulo, String mensaje, String tipo) {
        try {
            Long uid = (usuarioId != null && usuarioId > 0) ? usuarioId : 1L;
            Notificacion n = Notificacion.builder()
                    .usuarioId(uid)
                    .titulo(titulo != null ? titulo : "Notificación del Sistema")
                    .mensaje(mensaje != null ? mensaje : "")
                    .tipo(tipo != null ? tipo.toUpperCase() : "INFO")
                    .fechaCreacion(LocalDateTime.now())
                    .leido(false)
                    .build();
            return notificacionRepository.save(n);
        } catch (Exception e) {
            log.warn("No se pudo persistir la notificación '{}': {}", titulo, e.getMessage());
            return null;
        }
    }

    public NotificacionResponse toResponse(Notificacion entity) {
        String tipoNorm = entity.getTipo() != null ? entity.getTipo().toLowerCase() : "info";
        if (tipoNorm.contains("alert") || tipoNorm.contains("warning")) {
            tipoNorm = "alerta";
        } else if (tipoNorm.contains("exit") || tipoNorm.contains("success")) {
            tipoNorm = "exito";
        } else if (tipoNorm.contains("urg") || tipoNorm.contains("danger") || tipoNorm.contains("anul")) {
            tipoNorm = "urgente";
        } else {
            tipoNorm = "info";
        }

        String titulo = entity.getTitulo() != null ? entity.getTitulo() : "";
        String categoria = "sistema";
        String moduloId = "sistema";
        String funcionId = "";

        if (titulo.toLowerCase().contains("stock") || titulo.toLowerCase().contains("inventario") || titulo.toLowerCase().contains("kardex")) {
            categoria = "inventario";
            moduloId = "catalogo-e-inventario";
            funcionId = "cu11-movimientos-de-inventario";
        } else if (titulo.toLowerCase().contains("venta") || titulo.toLowerCase().contains("factura") || titulo.toLowerCase().contains("cotiz")) {
            categoria = "ventas";
            moduloId = "ventas-y-facturacion";
            funcionId = "cu19-registrar-venta-facturacion-y-anulacion";
        } else if (titulo.toLowerCase().contains("sesión") || titulo.toLowerCase().contains("sesion") || titulo.toLowerCase().contains("contraseña") || titulo.toLowerCase().contains("seguridad") || titulo.toLowerCase().contains("login")) {
            categoria = "seguridad";
            moduloId = "seguridad-y-auditoria";
            funcionId = "cu08-consultar-bitacora";
        }

        return NotificacionResponse.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .titulo(entity.getTitulo())
                .mensaje(entity.getMensaje())
                .tipo(tipoNorm)
                .categoria(categoria)
                .fecha(entity.getFechaCreacion())
                .leida(Boolean.TRUE.equals(entity.getLeido()))
                .moduloId(moduloId)
                .funcionId(funcionId)
                .build();
    }
}
