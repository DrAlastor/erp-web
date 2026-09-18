package com.uagrm.erp.backend.seguridad;

import com.uagrm.erp.backend.seguridad.catalogo.CatalogoPermisos;
import com.uagrm.erp.backend.seguridad.catalogo.MatrizRolesDeSistema;
import com.uagrm.erp.backend.seguridad.catalogo.RolDeSistema;
import com.uagrm.erp.backend.seguridad.dominio.Permiso;
import com.uagrm.erp.backend.seguridad.dominio.Rol;
import com.uagrm.erp.backend.seguridad.repositorio.PermisoRepositorio;
import com.uagrm.erp.backend.seguridad.repositorio.RolRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lleva a la base de datos el catálogo de permisos y la matriz de roles que se definen en
 * {@link CatalogoPermisos} y {@link MatrizRolesDeSistema}.
 *
 * <p>Las dos operaciones son <strong>idempotentes</strong>: corren en cada arranque y no
 * pisan nada de lo que ya exista. En particular, si el administrador editó la matriz de un
 * rol, el aprovisionamiento no la revierte — solo crea lo que falta.
 */
@Service
public class ServicioAprovisionamiento {

    private final PermisoRepositorio permisoRepositorio;
    private final RolRepositorio rolRepositorio;

    public ServicioAprovisionamiento(PermisoRepositorio permisoRepositorio, RolRepositorio rolRepositorio) {
        this.permisoRepositorio = permisoRepositorio;
        this.rolRepositorio = rolRepositorio;
    }

    /**
     * Inserta en la tabla {@code permiso} los permisos del catálogo que falten.
     *
     * @return cuántos permisos se crearon
     */
    @Transactional
    public int sincronizarCatalogoDePermisos() {
        Set<String> yaEstan = new LinkedHashSet<>();
        for (Permiso permiso : permisoRepositorio.findAll()) {
            yaEstan.add(permiso.getCodigo());
        }

        List<Permiso> faltantes = CatalogoPermisos.TODOS.stream()
                .filter(definicion -> !yaEstan.contains(definicion.codigo()))
                .map(Permiso::new)
                .toList();

        if (faltantes.isEmpty()) {
            return 0;
        }

        permisoRepositorio.saveAll(faltantes);
        return faltantes.size();
    }

    /**
     * Siembra en una empresa los roles de sistema que le falten, con la matriz de permisos
     * del documento de la HU-03.
     *
     * @return cuántos roles se crearon
     * @throws IllegalStateException si el catálogo de permisos de la base está incompleto
     */
    @Transactional
    public int aprovisionarEmpresa(UUID empresaId) {
        Map<String, Permiso> porCodigo = new LinkedHashMap<>();
        for (Permiso permiso : permisoRepositorio.findAll()) {
            porCodigo.put(permiso.getCodigo(), permiso);
        }

        int creados = 0;
        for (RolDeSistema definicion : MatrizRolesDeSistema.ROLES) {
            if (rolRepositorio.findByEmpresaIdAndCodigo(empresaId, definicion.codigo()).isPresent()) {
                continue;
            }
            rolRepositorio.save(new Rol(empresaId, definicion, resolver(definicion, porCodigo)));
            creados++;
        }
        return creados;
    }

    private Set<Permiso> resolver(RolDeSistema definicion, Map<String, Permiso> porCodigo) {
        Set<Permiso> permisos = new LinkedHashSet<>();
        for (String codigo : definicion.permisos()) {
            Permiso permiso = porCodigo.get(codigo);
            if (permiso == null) {
                throw new IllegalStateException(
                        "El catálogo de permisos de la base de datos está incompleto: falta " + codigo
                                + ". Sincronice el catálogo antes de aprovisionar la empresa.");
            }
            permisos.add(permiso);
        }
        return permisos;
    }
}
