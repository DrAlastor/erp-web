package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Datos generales de la empresa. Mapea la misma tabla {@code empresa} que usa el RBAC (CU03),
 * pero expone las columnas que agrega la migracion de la CU05.
 */
@Entity(name = "EmpresaPerfil")
@Table(name = "empresa")
@Getter
@Setter
@NoArgsConstructor
public class EmpresaPerfil {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String nit;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(length = 255)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Column(length = 120)
    private String email;

    @Column(name = "moneda_principal", nullable = false, length = 10)
    private String monedaPrincipal = "BOB";

    @Column(name = "moneda_secundaria", length = 10)
    private String monedaSecundaria;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(nullable = false)
    private boolean activo = true;
}
