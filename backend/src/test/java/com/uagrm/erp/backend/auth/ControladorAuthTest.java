package com.uagrm.erp.backend.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El endpoint de login se prueba sin la cadena de filtros de seguridad (addFilters=false)
 * porque acá lo que interesa es el contrato HTTP del controlador. El comportamiento de la
 * cadena completa —401 sin token y 403 sin permiso— se verifica en las pruebas de los
 * endpoints de seguridad.
 */
@WebMvcTest(ControladorAuth.class)
@AutoConfigureMockMvc(addFilters = false)
class ControladorAuthTest {

    private static final UsuarioPrincipal PRINCIPAL = new UsuarioPrincipal(
            UUID.fromString("88888888-8888-8888-8888-888888888888"),
            UUID.fromString("99999999-9999-9999-9999-999999999999"),
            "Administrador Demo",
            "admin@demo.bo");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServicioAutenticacion servicioAutenticacion;

    @MockBean
    private ServicioJwt servicioJwt;

    @Test
    @DisplayName("con credenciales correctas devuelve 200 y el token")
    void loginCorrecto() throws Exception {
        when(servicioAutenticacion.autenticar("admin@demo.bo", "Admin123*")).thenReturn(Optional.of(PRINCIPAL));
        when(servicioJwt.generar(PRINCIPAL)).thenReturn("un-token-firmado");
        when(servicioJwt.minutosDeVigencia()).thenReturn(480L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.bo\",\"password\":\"Admin123*\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("un-token-firmado"))
                .andExpect(jsonPath("$.nombre").value("Administrador Demo"))
                .andExpect(jsonPath("$.empresaId").value(PRINCIPAL.empresaId().toString()))
                .andExpect(jsonPath("$.minutosDeVigencia").value(480));
    }

    @Test
    @DisplayName("con credenciales equivocadas devuelve 401 y un error nombrado")
    void loginRechazado() throws Exception {
        when(servicioAutenticacion.autenticar(anyString(), anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.bo\",\"password\":\"equivocada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    @DisplayName("la respuesta del login no incluye permisos")
    void laRespuestaNoTraePermisos() throws Exception {
        when(servicioAutenticacion.autenticar("admin@demo.bo", "Admin123*")).thenReturn(Optional.of(PRINCIPAL));
        when(servicioJwt.generar(PRINCIPAL)).thenReturn("un-token-firmado");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.bo\",\"password\":\"Admin123*\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permisos").doesNotExist());
    }

    @Test
    @DisplayName("sin email la petición no es válida")
    void faltaElEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Admin123*\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("sin contraseña la petición no es válida")
    void faltaLaContrasena() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.bo\"}"))
                .andExpect(status().isBadRequest());
    }
}
