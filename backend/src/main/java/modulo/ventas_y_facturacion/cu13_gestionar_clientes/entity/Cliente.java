package modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import modulo.seguridad_y_auditoria.compartido.entity.Usuario;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "nit_ci", nullable = false, length = 30)
    private String nitCi;

    @Column(length = 30)
    private String telefono;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String direccion;

    @Column(length = 100)
    private String ciudad;

    @Column(name = "condicion_pago", length = 50)
    private String condicionPago = "CONTADO";

    @Column(name = "limite_credito", precision = 12, scale = 2)
    private BigDecimal limiteCredito = BigDecimal.ZERO;

    @Column(name = "dias_credito")
    private Integer diasCredito = 0;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Transient
    private Usuario usuario;

    public Cliente() {}

    public Cliente(Long id, String razonSocial, String nitCi, String telefono, String email,
                   String direccion, String ciudad, String condicionPago, BigDecimal limiteCredito,
                   Integer diasCredito, Boolean activo, LocalDateTime fechaCreacion) {
        this.id = id;
        this.razonSocial = razonSocial;
        this.nitCi = nitCi;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.ciudad = ciudad;
        this.condicionPago = condicionPago;
        this.limiteCredito = limiteCredito;
        this.diasCredito = diasCredito;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
    }

    @PrePersist
    protected void onCreate() {
        if (activo == null) activo = true;
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
        if (condicionPago == null || condicionPago.isBlank()) condicionPago = "CONTADO";
        if (limiteCredito == null) limiteCredito = BigDecimal.ZERO;
        if (diasCredito == null) diasCredito = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getNitCi() { return nitCi; }
    public void setNitCi(String nitCi) { this.nitCi = nitCi; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getCondicionPago() { return condicionPago; }
    public void setCondicionPago(String condicionPago) { this.condicionPago = condicionPago; }

    public BigDecimal getLimiteCredito() { return limiteCredito; }
    public void setLimiteCredito(BigDecimal limiteCredito) { this.limiteCredito = limiteCredito; }

    public Integer getDiasCredito() { return diasCredito; }
    public void setDiasCredito(Integer diasCredito) { this.diasCredito = diasCredito; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
