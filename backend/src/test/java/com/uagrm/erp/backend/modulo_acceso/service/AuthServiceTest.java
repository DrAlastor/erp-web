package com.uagrm.erp.backend.modulo_acceso.service;

import com.uagrm.erp.backend.exception.AccountLockedException;
import com.uagrm.erp.backend.exception.InvalidCredentialsException;
import com.uagrm.erp.backend.modulo_acceso.dto.auth.*;
import com.uagrm.erp.backend.modulo_acceso.entity.*;
import com.uagrm.erp.backend.modulo_acceso.repository.*;
import com.uagrm.erp.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock SesionRepository sesiones;
    @Mock PasswordEncoder encoder;
    private AuthService service;
    private JwtService jwt;
    private Usuario usuario;

    @BeforeEach void setup() {
        jwt = new JwtService("test_only_secret_at_least_32_bytes_long", 900000);
        service = new AuthService(usuarios, sesiones, encoder, jwt);
        ReflectionTestUtils.setField(service, "refreshExpirationDays", 7L);
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("admin");
        usuario.setEmail("admin@erp.com");
        usuario.setFullname("Administrador");
        usuario.setPassword("stored-hash");
    }
    private LoginRequest loginRequest(String identifier) {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail(identifier);
        request.setPassword("test-password");
        return request;
    }
    private RefreshTokenRequest refreshRequest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("test-refresh-token");
        return request;
    }
    private Sesion activeSession() {
        Sesion session = new Sesion();
        session.setUsuario(usuario);
        session.setFechaExpiracion(LocalDateTime.now().plusDays(1));
        when(sesiones.findByRefreshTokenHash(jwt.hashToken("test-refresh-token"))).thenReturn(Optional.of(session));
        return session;
    }
    @Test void loginIssuesTokensAndStoresOnlyRefreshHash() {
        when(usuarios.findForLogin("admin@erp.com")).thenReturn(Optional.of(usuario));
        when(encoder.matches("test-password", "stored-hash")).thenReturn(true);
        usuario.setIntentosFallidos(2);
        Rol rol = new Rol();
        rol.getPermisos().add(new Permiso(1, "ACCESO", "USUARIOS", "LECTURA", null));
        usuario.getRoles().add(rol);
        TokenResponse response = service.login(loginRequest(" admin@erp.com "), "127.0.0.1", "test-agent");
        assertTrue(jwt.isTokenValid(response.accessToken()));
        assertEquals("admin", jwt.extractUsername(response.accessToken()));
        assertEquals(java.util.List.of("ACCESO:USUARIOS:LECTURA"), jwt.extractAuthorities(response.accessToken()));
        assertEquals(0, usuario.getIntentosFallidos());
        assertNull(usuario.getBloqueadoHasta());
        var capture = org.mockito.ArgumentCaptor.forClass(Sesion.class);
        verify(sesiones).save(capture.capture());
        assertEquals(jwt.hashToken(response.refreshToken()), capture.getValue().getRefreshTokenHash());
        assertNotEquals(response.refreshToken(), capture.getValue().getRefreshTokenHash());
        assertEquals("127.0.0.1", capture.getValue().getIpOrigen());
    }
    @Test void disabledUserCannotLogin() {
        usuario.setEnable(false);
        when(usuarios.findForLogin("admin")).thenReturn(Optional.of(usuario));
        assertThrows(InvalidCredentialsException.class, () -> service.login(loginRequest("admin"), null, null));
        verifyNoInteractions(encoder, sesiones);
    }
    @Test void unknownUserCannotLogin() {
        when(usuarios.findForLogin("missing")).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> service.login(loginRequest("missing"), null, null));
        verifyNoInteractions(encoder, sesiones);
    }
    @Test void thirdFailureLocksAccountForFifteenMinutes() {
        when(usuarios.findForLogin("admin")).thenReturn(Optional.of(usuario));
        usuario.setIntentosFallidos(2);
        LocalDateTime before = LocalDateTime.now();
        assertThrows(InvalidCredentialsException.class, () -> service.login(loginRequest("admin"), null, null));
        assertEquals(3, usuario.getIntentosFallidos());
        assertTrue(usuario.getBloqueadoHasta().isAfter(before.plusMinutes(14)));
        assertTrue(usuario.getBloqueadoHasta().isBefore(LocalDateTime.now().plusMinutes(16)));
        verify(usuarios).save(usuario);
        assertThrows(AccountLockedException.class, () -> service.login(loginRequest("admin"), null, null));
        verify(encoder, times(1)).matches(anyString(), anyString());
    }
    @Test void expiredLockStartsANewAttemptWindow() {
        when(usuarios.findForLogin("admin")).thenReturn(Optional.of(usuario));
        usuario.setIntentosFallidos(3);
        usuario.setBloqueadoHasta(LocalDateTime.now().minusSeconds(1));
        assertThrows(InvalidCredentialsException.class, () -> service.login(loginRequest("admin"), null, null));
        assertEquals(1, usuario.getIntentosFallidos());
        assertNull(usuario.getBloqueadoHasta());
    }
    @Test void activeRefreshIssuesValidAccessToken() {
        activeSession();
        TokenResponse response = service.refresh(refreshRequest());
        assertTrue(jwt.isTokenValid(response.accessToken()));
        assertEquals("test-refresh-token", response.refreshToken());
    }
    @Test void disabledUserCannotRefresh() {
        activeSession();
        usuario.setEnable(false);
        assertThrows(InvalidCredentialsException.class, () -> service.refresh(refreshRequest()));
    }
    @Test void expiredSessionCannotRefresh() {
        activeSession().setFechaExpiracion(LocalDateTime.now().minusSeconds(1));
        assertThrows(InvalidCredentialsException.class, () -> service.refresh(refreshRequest()));
    }
    @Test void logoutClosesSessionAndPreventsRefresh() {
        Sesion session = activeSession();
        service.logout(refreshRequest());
        assertNotNull(session.getFechaCierre());
        verify(sesiones).save(session);
        assertThrows(InvalidCredentialsException.class, () -> service.refresh(refreshRequest()));
        service.logout(refreshRequest());
        verify(sesiones, times(1)).save(session);
    }
    @Test void unknownRefreshIsRejected() {
        when(sesiones.findByRefreshTokenHash(anyString())).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> service.refresh(refreshRequest()));
    }
}
