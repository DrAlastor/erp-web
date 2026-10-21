package modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Apariencia de la empresa (tabla 1.1 CONFIGURACION_EMPRESA del modelo).
 *
 * <p>Cada empresa tiene su propia base de datos, así que la tabla guarda una sola fila: la
 * apariencia que ven todos los usuarios de esa empresa. Las columnas pueden venir en
 * {@code null} desde el script de carga; el mapper completa los valores por defecto.
 */
@Entity(name = "ConfiguracionApariencia")
@Table(name = "configuracion_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionApariencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 30)
    private String tema;

    @Column(name = "color_primario", length = 20)
    private String colorPrimario;

    @Column(name = "color_secundario", length = 20)
    private String colorSecundario;

    @Column(name = "color_acento", length = 20)
    private String colorAcento;

    @Column(name = "tamano_fuente", length = 10)
    private String tamanoFuente;

    @Column(length = 15)
    private String tipografia;

    @Column(length = 15)
    private String bordes;

    /** Imagen del logo. En PostgreSQL es BYTEA; no lleva @Lob, que Hibernate mapearía a OID. */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo")
    private byte[] logo;

    @Column(name = "logo_tipo", length = 30)
    private String logoTipo;

    @Column(name = "logo_version", nullable = false)
    private int logoVersion;

    @Column(name = "encabezado_factura", columnDefinition = "TEXT")
    private String encabezadoFactura;

    @Column(name = "pie_factura", columnDefinition = "TEXT")
    private String pieFactura;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @Column(name = "modificado_por", length = 100)
    private String modificadoPor;

    /** Deja asentado quién y cuándo hizo el último cambio. */
    public void registrarCambio(String actor) {
        this.modificadoPor = actor;
        this.fechaModificacion = LocalDateTime.now();
    }

    /** Reemplaza el logo e invalida la copia en caché de los navegadores. */
    public void cambiarLogo(byte[] logo, String logoTipo) {
        this.logo = logo;
        this.logoTipo = logoTipo;
        this.logoVersion++;
    }

    public void quitarLogo() {
        cambiarLogo(null, null);
    }

    public boolean tieneLogo() {
        return logo != null && logo.length > 0;
    }
}
