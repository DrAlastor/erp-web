package com.uagrm.erp.backend.seguridad;

import com.uagrm.erp.backend.seguridad.dominio.Empresa;
import com.uagrm.erp.backend.seguridad.dominio.Rol;
import com.uagrm.erp.backend.seguridad.dominio.Usuario;
import com.uagrm.erp.backend.seguridad.dominio.UsuarioRol;
import com.uagrm.erp.backend.seguridad.repositorio.EmpresaRepositorio;
import com.uagrm.erp.backend.seguridad.repositorio.RolRepositorio;
import com.uagrm.erp.backend.seguridad.repositorio.UsuarioRepositorio;
import com.uagrm.erp.backend.seguridad.repositorio.UsuarioRolRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Siembra una empresa y tres usuarios de demostración para poder mostrar la HU-03 en vivo:
 * un administrador con acceso total, un cajero con el menú recortado y un auditor que solo
 * consulta.
 *
 * <p>Solo actúa sobre una base <strong>vacía de empresas</strong>: en cuanto el sistema
 * tiene datos reales no vuelve a tocar nada. Se desactiva con
 * {@code erp.seguridad.demo.habilitado=false}, que es lo que corresponde en producción.
 */
@Component
@ConditionalOnProperty(name = "erp.seguridad.demo.habilitado", havingValue = "true")
public class SembradoDemoSeguridad {

    private static final Logger log = LoggerFactory.getLogger(SembradoDemoSeguridad.class);

    private final EmpresaRepositorio empresaRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final RolRepositorio rolRepositorio;
    private final UsuarioRolRepositorio usuarioRolRepositorio;
    private final ServicioAprovisionamiento aprovisionamiento;
    private final PasswordEncoder codificador;

    public SembradoDemoSeguridad(EmpresaRepositorio empresaRepositorio,
                                 UsuarioRepositorio usuarioRepositorio,
                                 RolRepositorio rolRepositorio,
                                 UsuarioRolRepositorio usuarioRolRepositorio,
                                 ServicioAprovisionamiento aprovisionamiento,
                                 PasswordEncoder codificador) {
        this.empresaRepositorio = empresaRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.rolRepositorio = rolRepositorio;
        this.usuarioRolRepositorio = usuarioRolRepositorio;
        this.aprovisionamiento = aprovisionamiento;
        this.codificador = codificador;
    }

    /**
     * Crea los datos de demostración si la base no tiene ninguna empresa.
     *
     * @return true si sembró, false si no había nada que hacer
     */
    @Transactional
    public boolean sembrarSiLaBaseEstaVacia() {
        if (empresaRepositorio.count() > 0) {
            return false;
        }

        Empresa empresa = empresaRepositorio.save(new Empresa("Distribuidora Demo S.R.L.", "1029384019"));
        aprovisionamiento.aprovisionarEmpresa(empresa.getId());

        crearUsuarioConRol(empresa.getId(), "admin@demo.bo", "Admin123*", "Administrador Demo", "ADMINISTRADOR");
        crearUsuarioConRol(empresa.getId(), "cajero@demo.bo", "Cajero123*", "Cajero Demo", "CAJERO");
        crearUsuarioConRol(empresa.getId(), "auditor@demo.bo", "Auditor123*", "Auditor Demo", "AUDITOR_INTERNO");

        log.warn("Seguridad: se sembraron los datos de DEMOSTRACIÓN (empresa '{}' y 3 usuarios). "
                + "Desactivar con erp.seguridad.demo.habilitado=false", empresa.getNombre());
        return true;
    }

    private void crearUsuarioConRol(UUID empresaId, String email, String password, String nombre, String codigoRol) {
        Usuario usuario = usuarioRepositorio.save(
                new Usuario(empresaId, email, codificador.encode(password), nombre));

        Rol rol = rolRepositorio.findByEmpresaIdAndCodigo(empresaId, codigoRol)
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo sembrar el usuario demo: falta el rol " + codigoRol));

        usuarioRolRepositorio.save(new UsuarioRol(usuario.getId(), rol.getId(), null));
    }
}
