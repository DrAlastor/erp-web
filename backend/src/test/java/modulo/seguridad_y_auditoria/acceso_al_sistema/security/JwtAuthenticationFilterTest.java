package modulo.seguridad_y_auditoria.acceso_al_sistema.security;

import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.*;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = new JwtService("test_only_secret_at_least_32_bytes_long", 900000);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, usuarios);
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private void request(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }
    @Test void usesCurrentPermissionsInsteadOfStaleTokenPermissions() throws Exception {
        Usuario usuario = new Usuario();
        Rol rol = new Rol();
        rol.getPermisos().add(new Permiso(1, "INVENTARIO", "PRODUCTOS", "LECTURA", null));
        usuario.getRoles().add(rol);
        when(usuarios.findByUsername("admin")).thenReturn(Optional.of(usuario));
        request(jwt.generateAccessToken("admin", List.of("OLD:PERMISSION:WRITE")));
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(List.of("INVENTARIO:PRODUCTOS:LECTURA"), auth.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }
    @Test void disabledUserIsNotAuthenticated() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEnable(false);
        when(usuarios.findByUsername("admin")).thenReturn(Optional.of(usuario));
        request(jwt.generateAccessToken("admin", List.of()));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
    @Test void invalidTokenIsNotAuthenticated() throws Exception {
        request("not-a-jwt");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(usuarios);
    }
    @Test void expiredTokenIsNotAuthenticated() throws Exception {
        JwtService expired = new JwtService("test_only_secret_at_least_32_bytes_long", -1000);
        request(expired.generateAccessToken("admin", List.of()));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(usuarios);
    }
}
