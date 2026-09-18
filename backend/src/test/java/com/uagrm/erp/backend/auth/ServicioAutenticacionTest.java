package com.uagrm.erp.backend.auth;

import com.uagrm.erp.backend.seguridad.dominio.Usuario;
import com.uagrm.erp.backend.seguridad.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAutenticacionTest {

    private static final UUID EMPRESA = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Mock
    private UsuarioRepositorio usuarioRepositorio;

    private final PasswordEncoder codificador = new BCryptPasswordEncoder();

    private ServicioAutenticacion servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new ServicioAutenticacion(usuarioRepositorio, codificador);
    }

    private Usuario usuarioDemo() {
        return new Usuario(EMPRESA, "admin@demo.bo", codificador.encode("Admin123*"), "Administrador Demo");
    }

    @Test
    @DisplayName("con la contraseña correcta devuelve la identidad del usuario")
    void credencialesCorrectas() {
        Usuario usuario = usuarioDemo();
        when(usuarioRepositorio.findByEmailAndActivoTrue("admin@demo.bo")).thenReturn(Optional.of(usuario));

        Optional<UsuarioPrincipal> principal = servicio.autenticar("admin@demo.bo", "Admin123*");

        assertThat(principal).isPresent();
        assertThat(principal.get().usuarioId()).isEqualTo(usuario.getId());
        assertThat(principal.get().empresaId()).isEqualTo(EMPRESA);
        assertThat(principal.get().nombre()).isEqualTo("Administrador Demo");
        assertThat(principal.get().email()).isEqualTo("admin@demo.bo");
    }

    @Test
    @DisplayName("con la contraseña equivocada no autentica")
    void contrasenaEquivocada() {
        when(usuarioRepositorio.findByEmailAndActivoTrue("admin@demo.bo")).thenReturn(Optional.of(usuarioDemo()));

        assertThat(servicio.autenticar("admin@demo.bo", "otra-cosa")).isEmpty();
    }

    @Test
    @DisplayName("un usuario que no existe o está inactivo no autentica")
    void usuarioInexistenteOInactivo() {
        when(usuarioRepositorio.findByEmailAndActivoTrue("nadie@demo.bo")).thenReturn(Optional.empty());

        assertThat(servicio.autenticar("nadie@demo.bo", "Admin123*")).isEmpty();
    }

    @Test
    @DisplayName("el email se normaliza: espacios y mayúsculas no impiden entrar")
    void elEmailSeNormaliza() {
        when(usuarioRepositorio.findByEmailAndActivoTrue("admin@demo.bo")).thenReturn(Optional.of(usuarioDemo()));

        assertThat(servicio.autenticar("  ADMIN@Demo.BO  ", "Admin123*")).isPresent();
    }

    @Test
    @DisplayName("credenciales vacías no autentican ni consultan la base")
    void credencialesVacias() {
        assertThat(servicio.autenticar(null, "Admin123*")).isEmpty();
        assertThat(servicio.autenticar("admin@demo.bo", null)).isEmpty();
        assertThat(servicio.autenticar("  ", "  ")).isEmpty();
    }
}
