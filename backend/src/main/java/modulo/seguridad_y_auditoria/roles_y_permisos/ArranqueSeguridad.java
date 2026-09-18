package modulo.seguridad_y_auditoria.roles_y_permisos;

import modulo.seguridad_y_auditoria.roles_y_permisos.dominio.Empresa;
import modulo.seguridad_y_auditoria.roles_y_permisos.repositorio.EmpresaRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Aplica en cada arranque el catálogo de permisos y la matriz de roles de sistema.
 *
 * <p>Corre después de las migraciones de Flyway. Es idempotente: en un arranque normal no
 * cambia nada. Es lo que garantiza que una empresa creada por la CU-02 tenga sus siete
 * roles disponibles sin que nadie corra un script a mano.
 */
@org.springframework.core.annotation.Order(100)
@Component
public class ArranqueSeguridad implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ArranqueSeguridad.class);

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final ServicioAprovisionamiento aprovisionamiento;
    private final EmpresaRepositorio empresaRepositorio;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public ArranqueSeguridad(ServicioAprovisionamiento aprovisionamiento,
                             EmpresaRepositorio empresaRepositorio, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.aprovisionamiento = aprovisionamiento;
        this.empresaRepositorio = empresaRepositorio;
        this.jdbc = jdbc;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(ApplicationArguments args) {
        int permisosCreados = aprovisionamiento.sincronizarCatalogoDePermisos();
        log.info("Seguridad: catálogo de permisos sincronizado ({} permisos nuevos)", permisosCreados);


        for (Empresa empresa : empresaRepositorio.findAll()) {
            int rolesCreados = aprovisionamiento.aprovisionarEmpresa(empresa.getId());
            if (rolesCreados > 0) {
                log.info("Seguridad: se sembraron {} roles de sistema en la empresa '{}'",
                        rolesCreados, empresa.getNombre());
            }
        }
        entityManager.flush();
        // Import legacy assignments once. Removed CU03 roles stay removed on restart.
        jdbc.update("""
            INSERT INTO usuario_rol (usuario_id, rol_id)
            SELECT DISTINCT u.id, r.id FROM usuarios u
            JOIN usuario_roles ur ON ur.usuario_id = u.id
            JOIN roles legacy ON legacy.id = ur.rol_id
            JOIN rol r ON r.empresa_id = u.empresa_id AND r.codigo =
                CASE upper(legacy.nombre) WHEN 'ADMIN' THEN 'ADMINISTRADOR'
                    WHEN 'ALMACENERO' THEN 'ENCARGADO_ALMACEN' ELSE upper(legacy.nombre) END
            WHERE u.rbac_inicializado = false
            ON CONFLICT DO NOTHING
            """);
        jdbc.update("UPDATE usuarios SET rbac_inicializado = true WHERE rbac_inicializado = false");

    }
}
