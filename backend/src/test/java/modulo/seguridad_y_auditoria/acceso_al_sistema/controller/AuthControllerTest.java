package modulo.seguridad_y_auditoria.acceso_al_sistema.controller;

import comun.config.*;
import comun.BackendApplication;
import comun.exception.GlobalExceptionHandler;
import modulo.seguridad_y_auditoria.acceso_al_sistema.exception.*;
import modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth.*;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.acceso_al_sistema.service.AuthService;
import modulo.seguridad_y_auditoria.acceso_al_sistema.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = BackendApplication.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockBean AuthService service;
    @MockBean JwtService jwt;
    @MockBean UsuarioRepository usuarios;
    private static final String LOGIN = "{\"usernameOrEmail\":\"admin\",\"password\":\"test-password\"}";
    @Test void loginReturnsTokensWithoutExposingPassword() throws Exception {
        when(service.login(any(), anyString(), nullable(String.class))).thenReturn(new TokenResponse(
                "access", "refresh", "Bearer", 900,
                new UsuarioPerfilResponse(1L, "admin", "admin@erp.com", "Administrador")));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist());
    }
    @Test void wrongCredentialsReturn401() throws Exception {
        when(service.login(any(), anyString(), nullable(String.class)))
                .thenThrow(new InvalidCredentialsException("Usuario o contraseña incorrectos"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }
    @Test void lockedAccountReturns423() throws Exception {
        when(service.login(any(), anyString(), nullable(String.class)))
                .thenThrow(new AccountLockedException("Cuenta bloqueada", LocalDateTime.now().plusMinutes(15)));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isLocked()).andExpect(jsonPath("$.error").value("ACCOUNT_LOCKED"));
    }
    @Test void blankCredentialsAreRejectedBeforeService() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void refreshRejectionReturns401() throws Exception {
        when(service.refresh(any())).thenThrow(new InvalidCredentialsException("Refresh inválido"));
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"expired\"}"))
                .andExpect(status().isUnauthorized());
    }
    @Test void logoutDoesNotRequireAccessToken() throws Exception {
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isNoContent());
        verify(service).logout(any());
    }
    @Test void protectedApiReturns401InsteadOf403WhenNotAuthenticated() throws Exception {
        mvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
