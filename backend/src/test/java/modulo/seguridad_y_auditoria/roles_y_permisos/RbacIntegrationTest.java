package modulo.seguridad_y_auditoria.roles_y_permisos;

import comun.BackendApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Usuario;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.roles_y_permisos.repositorio.RolRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@Transactional
class RbacIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepositorio roles;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ServicioAutorizacion autorizacion;
    Usuario usuario;
    String token;
    UUID rolId;

    @BeforeEach void preparar() throws Exception {
        autorizacion.invalidarTodo();
        usuario = new Usuario();
        usuario.setUsername("integracion_rbac");
        usuario.setEmail("integracion_rbac@test.local");
        usuario.setFullname("Prueba integrada");
        usuario.setPassword(encoder.encode("Integracion123!"));
        usuarios.saveAndFlush(usuario);
        rolId = roles.findByEmpresaIdAndCodigo(usuario.getEmpresaId(), "AUDITOR_INTERNO").orElseThrow().getId();
        jdbc.update("INSERT INTO usuario_rol (usuario_id, rol_id) VALUES (?, ?)", usuario.getId(), rolId);
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"integracion_rbac\",\"password\":\"Integracion123!\"}"))
                .andExpect(status().isOk()).andReturn();
        token = mapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test void loginCu01UsaLaMismaCuentaEnCu03() throws Exception {
        mvc.perform(get("/api/seguridad/mis-permisos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuarioId").value(usuario.getId()))
                .andExpect(jsonPath("$.roles[0]").value("AUDITOR_INTERNO"));
        mvc.perform(get("/api/seguridad/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(7));
        mvc.perform(put("/api/seguridad/roles/" + rolId + "/permisos")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"permisos\":[]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/inventario/movimientos").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"productoId\":1,\"almacenId\":1,\"tipoMovimiento\":\"ENTRADA\",\"cantidad\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test void quitarRolRevocaPermisosSinCambiarToken() throws Exception {
        mvc.perform(get("/api/inventario/stock").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        jdbc.update("DELETE FROM usuario_rol WHERE usuario_id=?", usuario.getId());
        autorizacion.invalidarCache(usuario.getId());
        mvc.perform(get("/api/seguridad/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/inventario/stock").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test void cuentaDesactivadaEnCu02PierdeAccesoACu03() throws Exception {
        usuario.setEnable(false);
        usuarios.saveAndFlush(usuario);
        mvc.perform(get("/api/seguridad/mis-permisos").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test void cu02MuestraYFiltraLosRolesAsignadosEnCu03() throws Exception {
        var admin = usuarios.findByUsername("admin").orElseThrow();
        var identidad = new modulo.seguridad_y_auditoria.roles_y_permisos.auth.UsuarioPrincipal(
                admin.getId(), admin.getEmpresaId(), admin.getFullname(), admin.getEmail());
        var principal = new modulo.seguridad_y_auditoria.acceso_al_sistema.security.ApplicationUserPrincipal(
                "admin", java.util.List.of(), identidad);
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(principal);
        mvc.perform(get("/api/usuarios").param("role", rolId.toString()).with(auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(usuario.getId()))
                .andExpect(jsonPath("$.content[0].roles[0].id").value(rolId.toString()));
    }
}
