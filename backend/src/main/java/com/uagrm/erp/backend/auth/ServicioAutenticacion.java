package com.uagrm.erp.backend.auth;

import com.uagrm.erp.backend.seguridad.dominio.Usuario;
import com.uagrm.erp.backend.seguridad.repositorio.UsuarioRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Verifica credenciales contra la tabla {@code usuario}. Provisional: la CU-01 reemplaza
 * este paquete con la autenticación definitiva.
 *
 * <p>No distingue entre "el usuario no existe", "está inactivo" y "la contraseña está mal":
 * las tres respuestas son la misma, para no filtrar qué correos están registrados.
 */
@Service
public class ServicioAutenticacion {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificador;

    public ServicioAutenticacion(UsuarioRepositorio usuarioRepositorio, PasswordEncoder codificador) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.codificador = codificador;
    }

    /** Devuelve la identidad del usuario si las credenciales son válidas. */
    @Transactional(readOnly = true)
    public Optional<UsuarioPrincipal> autenticar(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        String normalizado = email.trim().toLowerCase(Locale.ROOT);

        return usuarioRepositorio.findByEmailAndActivoTrue(normalizado)
                .filter(usuario -> codificador.matches(password, usuario.getPasswordHash()))
                .map(this::aPrincipal);
    }

    private UsuarioPrincipal aPrincipal(Usuario usuario) {
        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getEmpresaId(),
                usuario.getNombre(),
                usuario.getEmail());
    }
}
