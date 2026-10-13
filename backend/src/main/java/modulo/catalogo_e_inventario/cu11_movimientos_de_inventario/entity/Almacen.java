package modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad Almacén/Sucursal mapeada a la tabla sucursal de ERP_Asus.
 */
@Entity
@Table(name = "sucursal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Almacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    public Boolean getEsPrincipal() {
        return id != null && id == 1;
    }
}
