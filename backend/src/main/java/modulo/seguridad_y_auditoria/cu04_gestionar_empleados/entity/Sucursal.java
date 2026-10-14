package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sucursal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ciudad_id")
    private Integer ciudadId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Column(name = "hora_apertura")
    private java.time.LocalTime horaApertura;

    @Column(name = "hora_cierre")
    private java.time.LocalTime horaCierre;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
