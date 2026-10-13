package modulo.seguridad_y_auditoria.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permisos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(nullable = false, length = 50)
    private String pantalla;

    @Column(nullable = false, length = 30)
    private String accion;

    @Column(length = 255)
    private String descripcion;

    public String toAuthority() {
        return modulo + ":" + pantalla + ":" + accion;
    }
}
