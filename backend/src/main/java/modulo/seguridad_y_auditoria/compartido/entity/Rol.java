package modulo.seguridad_y_auditoria.compartido.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 100)
    private String permiso;

    @Column(nullable = false)
    private Boolean activo = true;

    @Transient
    private Set<Permiso> permisos = new HashSet<>();

    public String getDescripcion() {
        return this.permiso != null ? this.permiso : this.nombre;
    }

    public void setDescripcion(String descripcion) {
        this.permiso = descripcion;
    }
}
