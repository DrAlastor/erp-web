package modulo.inventario_y_almacenes.catalogo_de_articulos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import comun.BackendApplication;

import modulo.inventario_y_almacenes.catalogo_de_articulos.repository.ArticuloRepository;
import modulo.seguridad_y_auditoria.acceso_al_sistema.security.ApplicationUserPrincipal;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.roles_y_permisos.security.UsuarioPrincipal;

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
 * Pruebas de integración de la HU-05 contra PostgreSQL.
 *
 * <p>Comprueba el contrato HTTP del catálogo con permisos efectivos del RBAC: el Encargado de
 * Almacén consulta y crea, y el Auditor Interno —que solo consulta— recibe 403 al intentar
 * dar de baja. Las respuestas nunca son entidades JPA.
 */
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class ArticuloIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ArticuloRepository articulos;
    @Autowired UsuarioRepository usuarios;
    @Autowired JdbcTemplate jdbc;

    @Test
    void exigeAutenticacion() throws Exception {
        mvc.perform(get("/api/articulos")).andExpect(status().isUnauthorized());
    }

    @Test
    void listaElCatalogoConSuCategoria() throws Exception {
        mvc.perform(get("/api/articulos").with(como("inv_almacenero", "ENCARGADO_ALMACEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sku").exists())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void creaUnArticuloYRechazaElSkuDuplicado() throws Exception {
        Integer categoriaId = jdbc.queryForObject("SELECT id FROM categorias ORDER BY id LIMIT 1",
                Integer.class);
        String cuerpo = "{\"sku\":\"SKU-TEST-HU05\",\"nombre\":\"Artículo de prueba\","
                + "\"descripcion\":\"Prueba\",\"precio\":10.50,\"stock\":3,"
                + "\"categoriaId\":" + categoriaId + "}";

        mvc.perform(post("/api/articulos")
                        .with(como("inv_almacenero", "ENCARGADO_ALMACEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-TEST-HU05"))
                .andExpect(jsonPath("$.categoria.id").value(categoriaId));

        mvc.perform(post("/api/articulos")
                        .with(como("inv_almacenero", "ENCARGADO_ALMACEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isConflict());
    }

    @Test
    void elPerfilDeSoloConsultaNoPuedeDarDeBaja() throws Exception {
        Long id = articulos.findAll().stream().findFirst().orElseThrow().getId();
        mvc.perform(delete("/api/articulos/" + id).with(como("inv_auditor", "AUDITOR_INTERNO")))
                .andExpect(status().isForbidden());
        assertFalse(Boolean.FALSE.equals(articulos.findById(id).orElseThrow().getActivo()));
    }

    @Test
    void rechazaUnPrecioInvalido() throws Exception {
        mvc.perform(post("/api/articulos")
                        .with(como("inv_almacenero", "ENCARGADO_ALMACEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-MALO\",\"nombre\":\"Malo\",\"precio\":0}"))
                .andExpect(status().isBadRequest());
    }

    /** Cuenta de prueba con un rol del catálogo de la CU03 (los permisos viven en la base). */
    private RequestPostProcessor como(String username, String codigoRol) {
        Usuario cuenta = usuarios.findByUsername(username).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setUsername(username);
            nuevo.setEmail(username + "@test.local");
            nuevo.setFullname("Usuario de prueba " + codigoRol);
            nuevo.setPassword("no-usada-en-la-prueba");
            return usuarios.saveAndFlush(nuevo);
        });

        jdbc.update("""
                INSERT INTO usuario_rol (usuario_id, rol_id)
                SELECT ?, r.id FROM rol r
                WHERE r.codigo = ? AND r.empresa_id = ?
                ON CONFLICT DO NOTHING
                """, cuenta.getId(), codigoRol, cuenta.getEmpresaId());

        UsuarioPrincipal identidad = new UsuarioPrincipal(cuenta.getId(), cuenta.getEmpresaId(),
                cuenta.getFullname(), cuenta.getEmail());
        return user(new ApplicationUserPrincipal(cuenta.getUsername(), List.of(), identidad));
    }
}
