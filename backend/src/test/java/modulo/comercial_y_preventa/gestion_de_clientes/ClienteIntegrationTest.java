package modulo.comercial_y_preventa.gestion_de_clientes;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import comun.BackendApplication;

import modulo.comercial_y_preventa.gestion_de_clientes.entity.Cliente;
import modulo.comercial_y_preventa.gestion_de_clientes.repository.ClienteRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Pruebas de integración de la HU-04 contra PostgreSQL.
 *
 * <p>La autorización se ejerce con la identidad real del filtro JWT y con los permisos
 * efectivos del RBAC de la CU03: un Gerente General puede consultar el directorio pero no
 * modificarlo, y el administrador puede todo. Los permisos viven en la base, no en el token.
 */
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class ClienteIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ClienteRepository clientes;
    @Autowired UsuarioRepository usuarios;
    @Autowired JdbcTemplate jdbc;

    private Cliente objetivo;

    @BeforeEach
    void preparar() {
        objetivo = new Cliente();
        objetivo.setRazonSocial("Cliente HU-04");
        objetivo.setNitCi("999999999");
        objetivo.setTelefono("70000000");
        objetivo.setDireccion("Dirección de prueba");
        objetivo.setActivo(true);
        clientes.saveAndFlush(objetivo);
    }

    /** Administrador de demostración: COMERCIAL con crear, consultar, modificar y anular. */
    private RequestPostProcessor administrador() {
        return como(usuarios.findByUsername("admin").orElseThrow());
    }

    /** Gerente General: solo consulta, así que no puede crear ni modificar clientes. */
    private RequestPostProcessor soloConsultas() {
        Usuario gerente = usuarios.findByUsername("cliente_hu04").orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setUsername("cliente_hu04");
            nuevo.setEmail("cliente_hu04@test.local");
            nuevo.setFullname("Gerente de prueba");
            nuevo.setPassword("no-usada-en-la-prueba");
            return usuarios.saveAndFlush(nuevo);
        });

        jdbc.update("""
                INSERT INTO usuario_rol (usuario_id, rol_id)
                SELECT ?, r.id FROM rol r
                WHERE r.codigo = 'GERENTE_GENERAL' AND r.empresa_id = ?
                ON CONFLICT DO NOTHING
                """, gerente.getId(), gerente.getEmpresaId());

        return como(gerente);
    }

    private RequestPostProcessor como(Usuario usuario) {
        UsuarioPrincipal identidad = new UsuarioPrincipal(usuario.getId(), usuario.getEmpresaId(),
                usuario.getFullname(), usuario.getEmail());
        return user(new ApplicationUserPrincipal(usuario.getUsername(), List.of(), identidad));
    }

    private String cuerpo(String razonSocial, String nitCi) {
        return "{\"razonSocial\":\"" + razonSocial + "\",\"nitCi\":\"" + nitCi + "\"}";
    }

    @Test
    void exigeAutenticacion() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void dejaConsultarPeroNoEscribirSinElPermisoDeEscritura() throws Exception {
        mvc.perform(get("/api/clientes").param("search", "999999999").with(soloConsultas()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nitCi").value("999999999"));

        mvc.perform(post("/api/clientes")
                        .with(soloConsultas())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Nueva Empresa", "123456789")))
                .andExpect(status().isForbidden());
    }

    @Test
    void creaYRechazaElNitCiDuplicado() throws Exception {
        mvc.perform(post("/api/clientes")
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Nueva Empresa", "123456789")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true));

        mvc.perform(post("/api/clientes")
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Otra Empresa", "123456789")))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizaYCambiaElEstado() throws Exception {
        mvc.perform(put("/api/clientes/" + objetivo.getId())
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razonSocial\":\"Cliente Actualizado\",\"nitCi\":\"999999999\","
                                + "\"direccion\":\"Nueva dirección\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("Cliente Actualizado"));

        mvc.perform(patch("/api/clientes/" + objetivo.getId() + "/status")
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void rechazaCamposProtegidos() throws Exception {
        mvc.perform(put("/api/clientes/" + objetivo.getId())
                        .with(administrador())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razonSocial\":\"Modificado\",\"nitCi\":\"999999999\",\"activo\":false}"))
                .andExpect(status().isBadRequest());
        assertTrue(clientes.findById(objetivo.getId()).orElseThrow().getActivo());
    }
}
