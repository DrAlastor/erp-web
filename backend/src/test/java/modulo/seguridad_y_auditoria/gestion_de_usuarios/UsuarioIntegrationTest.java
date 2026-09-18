package modulo.seguridad_y_auditoria.gestion_de_usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import comun.BackendApplication;

import modulo.seguridad_y_auditoria.acceso_al_sistema.security.ApplicationUserPrincipal;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.roles_y_permisos.security.UsuarioPrincipal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pruebas de integración de la CU-02 contra PostgreSQL.
 *
 * <p>La autorización se ejerce con la misma identidad que deja el filtro JWT
 * ({@link ApplicationUserPrincipal} con su {@link UsuarioPrincipal}): el permiso no viaja en
 * las authorities, se resuelve contra la base con el RBAC de la CU-03. Por eso los casos
 * comparan un administrador real contra una cuenta con permiso de solo consulta.
 */
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@Transactional
class UsuarioIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    private Usuario objetivo;

    @BeforeEach
    void preparar() {
        objetivo = new Usuario();
        objetivo.setUsername("cu02_test");
        objetivo.setEmail("cu02_test@test.local");
        objetivo.setFullname("Prueba CU02");
        objetivo.setPassword("protected-hash");
        usuarios.saveAndFlush(objetivo);
    }

    /** El administrador de demostración, con el rol ADMINISTRADOR de la CU-03. */
    private RequestPostProcessor administrador() {
        return como(usuarios.findByUsername("admin").orElseThrow());
    }

    /** Una cuenta con rol Auditor Interno: consulta todo, no modifica nada. */
    private RequestPostProcessor soloConsulta() {
        jdbc.update("""
                INSERT INTO usuario_rol (usuario_id, rol_id)
                SELECT ?, r.id FROM rol r
                WHERE r.codigo = 'AUDITOR_INTERNO' AND r.empresa_id = ?
                ON CONFLICT DO NOTHING
                """, objetivo.getId(), objetivo.getEmpresaId());
        return como(objetivo);
    }

    private RequestPostProcessor como(Usuario usuario) {
        UsuarioPrincipal identidad = new UsuarioPrincipal(usuario.getId(), usuario.getEmpresaId(),
                usuario.getFullname(), usuario.getEmail());
        return user(new ApplicationUserPrincipal(usuario.getUsername(), List.of(), identidad));
    }

    private String cuerpoDeAlta(String username, String email, String rolId) throws Exception {
        return mapper.writeValueAsString(Map.of(
                "username", username,
                "email", email,
                "password", "Cuenta123!",
                "fullname", "Cuenta nueva",
                "rolesIds", rolId == null ? List.of() : List.of(rolId)));
    }

    @Test
    void exigeAutenticacion() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void elPermisoDeConsultaPermiteLeerPeroNoEscribir() throws Exception {
        mvc.perform(get("/api/usuarios").with(soloConsulta())).andExpect(status().isOk());
        mvc.perform(put("/api/usuarios/" + objetivo.getId()).with(soloConsulta())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullname\":\"Nuevo nombre\",\"email\":\"nuevo@test.local\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deniegaAQuienNoTieneElPermiso() throws Exception {
        mvc.perform(get("/api/usuarios").with(como(objetivo))).andExpect(status().isForbidden());
    }

    @Test
    void listaSinDatosSensiblesYAplicaFiltros() throws Exception {
        mvc.perform(get("/api/usuarios")
                        .param("search", "cu02_test")
                        .param("enable", "true")
                        .with(administrador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
        mvc.perform(get("/api/usuarios")
                        .param("search", "cu02_test")
                        .param("enable", "false")
                        .with(administrador()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rechazaCamposProtegidos() throws Exception {
        mvc.perform(put("/api/usuarios/" + objetivo.getId())
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullname\":\"Changed\",\"email\":\"valid@test.local\",\"password\":\"hack\"}"))
                .andExpect(status().isBadRequest());
        assertEquals("protected-hash", usuarios.findById(objetivo.getId()).orElseThrow().getPassword());
    }

    @Test
    void actualizaYAuditaDeFormaAtomica() throws Exception {
        mvc.perform(put("/api/usuarios/" + objetivo.getId())
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullname\":\"Updated Name\",\"email\":\"changed@test.local\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullname").value("Updated Name"))
                .andExpect(jsonPath("$.password").doesNotExist());
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM bitacora_logs WHERE entidad_id=? AND accion='USUARIO_ACTUALIZADO'",
                Integer.class, objetivo.getId()));
    }

    @Test
    void activaYDesactivaConAuditoria() throws Exception {
        mvc.perform(patch("/api/usuarios/" + objetivo.getId() + "/status")
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enable\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enable").value(false));
        mvc.perform(patch("/api/usuarios/" + objetivo.getId() + "/status")
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enable\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enable").value(true));
        assertEquals(2, jdbc.queryForObject(
                "SELECT count(*) FROM bitacora_logs WHERE entidad_id=?",
                Integer.class, objetivo.getId()));
    }

    @Test
    void noDejaQueElAdministradorSeDesactiveASiMismo() throws Exception {
        Long id = usuarios.findByUsername("admin").orElseThrow().getId();
        mvc.perform(patch("/api/usuarios/" + id + "/status")
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enable\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void respondeNoEncontradoParaUnaCuentaInexistente() throws Exception {
        mvc.perform(get("/api/usuarios/9223372036854775807").with(administrador()))
                .andExpect(status().isNotFound());
    }

    @Test
    void creaCuentaConContrasenaCifradaRolDelRbacYAuditoria() throws Exception {
        String rol = jdbc.queryForObject("SELECT id::text FROM rol WHERE codigo='CAJERO' AND empresa_id=?",
                String.class, objetivo.getEmpresaId());
        mvc.perform(post("/api/usuarios")
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAlta("cu02_nuevo", "nuevo@test.local", rol)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.roles[0].id").value(rol));

        Usuario creado = usuarios.findByUsername("cu02_nuevo").orElseThrow();
        assertTrue(encoder.matches("Cuenta123!", creado.getPassword()));
        assertTrue(creado.getEnable());
        assertTrue(creado.getRbacInicializado());
        assertEquals("admin", creado.getCreatedBy());
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM bitacora_logs WHERE accion='USUARIO_CREADO' AND entidad_id=?",
                Integer.class, creado.getId()));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"cu02_nuevo\",\"password\":\"Cuenta123!\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/usuarios").with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAlta("CU02_NUEVO", "otro@test.local", null)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/usuarios").with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAlta("otro_nuevo", "NUEVO@test.local", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void unRolInexistenteNoCreaLaCuenta() throws Exception {
        mvc.perform(post("/api/usuarios").with(administrador()).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAlta("cu02_invalido", "invalido@test.local", UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest());
        assertTrue(usuarios.findByUsername("cu02_invalido").isEmpty());
    }

    @Test
    void noPuedeCrearConPermisoDeSoloConsulta() throws Exception {
        mvc.perform(post("/api/usuarios")
                        .with(soloConsulta())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDeAlta("cu02_denegado", "denegado@test.local", null)))
                .andExpect(status().isForbidden());
        assertTrue(usuarios.findByUsername("cu02_denegado").isEmpty());
    }

    @Test
    void desactivarCierraLasSesionesDeRenovacion() throws Exception {
        jdbc.update("""
                INSERT INTO sesion(usuario_id, refresh_token_hash, fecha_expiracion)
                VALUES (?, 'cu02_session_hash', CURRENT_TIMESTAMP + INTERVAL '1 day')
                """, objetivo.getId());
        mvc.perform(patch("/api/usuarios/" + objetivo.getId() + "/status")
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enable\":false}"))
                .andExpect(status().isOk());
        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM sesion WHERE usuario_id=? AND fecha_cierre IS NULL",
                Integer.class, objetivo.getId()));
    }
}
