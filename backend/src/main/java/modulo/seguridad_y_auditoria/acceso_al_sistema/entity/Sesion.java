package modulo.seguridad_y_auditoria.acceso_al_sistema.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @PrePersist
    protected void onCreate() {
        this.fechaInicio = LocalDateTime.now();
    }
}
