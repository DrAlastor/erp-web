package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "AlmacenMaestro")
@Table(name = "almacenes")
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

    @Column(length = 50)
    private String codigo;

    @Column(name = "sucursal_id")
    private Integer sucursalId;

    @Column(length = 255)
    private String direccion;

    @Column(name = "capacidad_estimada", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal capacidadEstimada = BigDecimal.ZERO;

    @Column(name = "es_principal", nullable = false)
    @Builder.Default
    private Boolean esPrincipal = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(name = "fecha_creacion")
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
