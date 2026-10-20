package modulo.catalogo_e_inventario.cu09_datos_maestros_inventario.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlmacenDTO {
    private Integer id;
    private String nombre;
    private String codigo;
    private Integer sucursalId;
    private String sucursalNombre;
    private Integer ciudadId;
    private String ciudadNombre;
    private String direccion;
    private String telefono;
    private String horaApertura;
    private String horaCierre;
    private String horario;
    private BigDecimal capacidadEstimada;
    private Boolean esPrincipal;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}
