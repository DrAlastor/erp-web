package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listas_precios")
public class ListaPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "es_principal", nullable = false)
    private Boolean esPrincipal = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "listaPrecio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrecioProducto> precios = new ArrayList<>();

    public ListaPrecio() {}

    public ListaPrecio(Integer id, String codigo, String nombre, String descripcion, Boolean esPrincipal, Boolean activo) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.esPrincipal = esPrincipal != null ? esPrincipal : false;
        this.activo = activo != null ? activo : true;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getEsPrincipal() { return esPrincipal; }
    public void setEsPrincipal(Boolean esPrincipal) { this.esPrincipal = esPrincipal; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public List<PrecioProducto> getPrecios() { return precios; }
    public void setPrecios(List<PrecioProducto> precios) { this.precios = precios; }
}
