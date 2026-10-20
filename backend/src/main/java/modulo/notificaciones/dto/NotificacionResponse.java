package modulo.notificaciones.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionResponse {
    private Long id;
    private Long usuarioId;
    private String titulo;
    private String mensaje;
    private String tipo;      // info, alerta, exito, urgente
    private String categoria; // ventas, inventario, seguridad, sistema
    private LocalDateTime fecha;
    private Boolean leida;
    private String moduloId;
    private String funcionId;
}
