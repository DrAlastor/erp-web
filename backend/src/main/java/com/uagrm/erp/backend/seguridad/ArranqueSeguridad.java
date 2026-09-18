package com.uagrm.erp.backend.seguridad;

import com.uagrm.erp.backend.seguridad.dominio.Empresa;
import com.uagrm.erp.backend.seguridad.repositorio.EmpresaRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Aplica en cada arranque el catálogo de permisos y la matriz de roles de sistema.
 *
 * <p>Corre después de las migraciones de Flyway. Es idempotente: en un arranque normal no
 * hace nada y lo dice en el log. Es lo que garantiza que una empresa creada por la CU-02
 * tenga sus siete roles disponibles sin que nadie corra un script a mano.
 */
@Component
public class ArranqueSeguridad implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ArranqueSeguridad.class);

    private final ServicioAprovisionamiento aprovisionamiento;
    private final EmpresaRepositorio empresaRepositorio;

    public ArranqueSeguridad(ServicioAprovisionamiento aprovisionamiento, EmpresaRepositorio empresaRepositorio) {
        this.aprovisionamiento = aprovisionamiento;
        this.empresaRepositorio = empresaRepositorio;
    }

    @Override
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
    }
}
