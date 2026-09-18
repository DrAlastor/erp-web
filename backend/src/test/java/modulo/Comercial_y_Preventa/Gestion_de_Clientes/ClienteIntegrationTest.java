package modulo.Comercial_y_Preventa.Gestion_de_Clientes;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import comun.BackendApplication;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.entity.Cliente;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class ClienteIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ClienteRepository clientes;

    private Cliente target;

    @BeforeEach
    void setup() {
        target = new Cliente();
        target.setRazonSocial("Cliente HU-04");
        target.setNitCi("999999999");
        target.setTelefono("70000000");
        target.setDireccion("Dirección de prueba");
        target.setActivo(true);
        clientes.saveAndFlush(target);
    }

    private RequestPostProcessor reader() {
        return user("vendedor")
                .authorities(new SimpleGrantedAuthority("COMERCIAL:CLIENTES:LECTURA"));
    }

    private RequestPostProcessor writer() {
        return user("admin")
                .authorities(new SimpleGrantedAuthority("COMERCIAL:CLIENTES:ESCRITURA"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized());
    }

    @Test
    void allowsReadAndRejectsWriteWithoutWritePermission() throws Exception {
        mvc.perform(get("/api/clientes").param("search", "999999999").with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nitCi").value("999999999"));

        mvc.perform(post("/api/clientes")
                        .with(reader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razonSocial\":\"Nueva Empresa\",\"nitCi\":\"123456789\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsAndRejectsDuplicateNitCi() throws Exception {
        mvc.perform(post("/api/clientes")
                        .with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razonSocial\":\"Nueva Empresa\",\"nitCi\":\"123456789\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true));

        mvc.perform(post("/api/clientes")
                        .with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razonSocial\":\"Otra Empresa\",\"nitCi\":\"123456789\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updatesAndChangesStatus() throws Exception {
        mvc.perform(put("/api/clientes/" + target.getId())
                        .with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razonSocial\":\"Cliente Actualizado\",\"nitCi\":\"999999999\",\"direccion\":\"Nueva dirección\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("Cliente Actualizado"));

        mvc.perform(patch("/api/clientes/" + target.getId() + "/status")
                        .with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void rejectsProtectedFields() throws Exception {
        mvc.perform(put("/api/clientes/" + target.getId())
                        .with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razonSocial\":\"Modificado\",\"nitCi\":\"999999999\",\"activo\":false}"))
                .andExpect(status().isBadRequest());
    }
}
