package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "CiudadCatalogo")
@Table(name = "ciudad")
@Getter
@NoArgsConstructor
public class Ciudad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String pais;
}
