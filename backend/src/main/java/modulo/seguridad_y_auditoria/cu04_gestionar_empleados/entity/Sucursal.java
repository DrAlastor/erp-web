package modulo.seguridad_y_auditoria.cu04_gestionar_empleados.entity;

import jakarta.persistence.*;
import lombok.*;

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

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCiudadId() { return ciudadId; }
    public void setCiudadId(Integer ciudadId) { this.ciudadId = ciudadId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public java.time.LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(java.time.LocalTime horaApertura) { this.horaApertura = horaApertura; }

    public java.time.LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(java.time.LocalTime horaCierre) { this.horaCierre = horaCierre; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
