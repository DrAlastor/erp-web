package modulo.seguridad_y_auditoria.gestion_de_usuarios;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import comun.BackendApplication;

import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Usuario;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.UsuarioRepository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class UsuarioIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository users;
    @Autowired JdbcTemplate jdbc;
    Usuario target;

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return user("admin")
                .authorities(new SimpleGrantedAuthority("SEGURIDAD:USUARIOS:ESCRITURA"));
    }

    @BeforeEach
    void setup() {
        target = new Usuario();
        target.setUsername("cu02_test");
        target.setEmail("cu02_test@test.local");
        target.setFullname("Prueba CU02");
        target.setPassword("protected-hash");
        users.saveAndFlush(target);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test
    void deniesReadOnlyPermission() throws Exception {
        mvc.perform(
                        get("/api/usuarios")
                                .with(
                                        user("admin")
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "SEGURIDAD:USUARIOS:LECTURA"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deniesNonAdministratorWithPermission() throws Exception {
        mvc.perform(
                        get("/api/usuarios")
                                .with(
                                        user("cu02_test")
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "SEGURIDAD:USUARIOS:ESCRITURA"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsWithoutSensitiveDataAndFilters() throws Exception {
        mvc.perform(
                        get("/api/usuarios")
                                .param("search", "cu02_test")
                                .param("enable", "true")
                                .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());
        mvc.perform(
                        get("/api/usuarios")
                                .param("search", "cu02_test")
                                .param("enable", "false")
                                .with(admin()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsProtectedFields() throws Exception {
        mvc.perform(
                        put("/api/usuarios/" + target.getId())
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"fullname\":\"Changed\",\"email\":\"valid@test.local\",\"password\":\"hack\"}"))
                .andExpect(status().isBadRequest());
        assertEquals("protected-hash", users.findById(target.getId()).orElseThrow().getPassword());
    }

    @Test
    void updatesAndAuditsAtomically() throws Exception {
        mvc.perform(
                        put("/api/usuarios/" + target.getId())
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"fullname\":\"Updated"
                                            + " Name\",\"email\":\"changed@test.local\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullname").value("Updated Name"))
                .andExpect(jsonPath("$.password").doesNotExist());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT count(*) FROM bitacora_logs WHERE entidad_id=? AND"
                            + " accion='USUARIO_ACTUALIZADO'",
                        Integer.class,
                        target.getId()));
    }

    @Test
    void activatesAndDeactivatesWithAudit() throws Exception {
        mvc.perform(
                        patch("/api/usuarios/" + target.getId() + "/status")
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"enable\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enable").value(false));
        mvc.perform(
                        patch("/api/usuarios/" + target.getId() + "/status")
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"enable\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enable").value(true));
        assertEquals(
                2,
                jdbc.queryForObject(
                        "SELECT count(*) FROM bitacora_logs WHERE entidad_id=?",
                        Integer.class,
                        target.getId()));
    }

    @Test
    void rejectsSelfDeactivationAndMissingUser() throws Exception {
        Long id = users.findByUsername("admin").orElseThrow().getId();
        mvc.perform(
                        patch("/api/usuarios/" + id + "/status")
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"enable\":false}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/usuarios/9223372036854775807").with(admin()))
                .andExpect(status().isNotFound());
    }
}
