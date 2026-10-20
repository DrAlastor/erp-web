package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "parametro_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ParametroSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(nullable = false, length = 60)
    private String clave;

    @Column(nullable = false)
    private String valor;

    @Column
    private String descripcion;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    public ParametroSistema(UUID empresaId, String clave, String valor, String descripcion) {
        this.empresaId = empresaId;
        this.clave = clave;
        this.valor = valor;
        this.descripcion = descripcion;
    }
}
