package com.uagrm.erp.backend.seguridad.web;

import com.uagrm.erp.backend.auth.FiltroJwt;
import com.uagrm.erp.backend.auth.ServicioJwt;
import com.uagrm.erp.backend.auth.UsuarioPrincipal;
import com.uagrm.erp.backend.config.SecurityConfig;
import com.uagrm.erp.backend.seguridad.ServicioAsignaciones;
import com.uagrm.erp.backend.seguridad.ServicioAutorizacion;
import com.uagrm.erp.backend.seguridad.ServicioRoles;
import com.uagrm.erp.backend.seguridad.config.ConfiguracionMetodosSeguros;
import com.uagrm.erp.backend.seguridad.config.EvaluadorDePermisos;
import com.uagrm.erp.backend.seguridad.error.RecursoNoEncontrado;
import com.uagrm.erp.backend.seguridad.error.ReglaDeNegocio;
import com.uagrm.erp.backend.seguridad.web.dto.RolDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de los endpoints con la cadena de seguridad real: la anotación
 * {@code @PreAuthorize}, el evaluador de permisos y los manejadores de 401 y 403.
 *
 * <p>Es la prueba que respalda los criterios de aprobación de la HU-03: el sistema verifica
 * los permisos antes de permitir el acceso a una funcionalidad protegida, y un usuario no
 * autorizado recibe una respuesta de acceso denegado.
 */
@WebMvcTest({ControladorRoles.class, ControladorAsignaciones.class, ControladorMisPermisos.class})
@Import({SecurityConfig.class, FiltroJwt.class, ManejadorAccesoDenegado.class,
        PuntoDeEntradaNoAutenticado.class, ConfiguracionMetodosSeguros.class, EvaluadorDePermisos.class})
class EndpointsDeSeguridadTest {

    private static final UUID EMPRESA = UUID.fromString("f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0");
    private static final UUID ROL = UUID.fromString("a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1");
    private static final UUID OTRO_USUARIO = UUID.fromString("b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2");

    private static final UsuarioPrincipal PRINCIPAL = new UsuarioPrincipal(
            UUID.fromString("c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3"), EMPRESA, "Administrador Demo", "admin@demo.bo");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServicioRoles servicioRoles;

    @MockBean
    private ServicioAsignaciones servicioAsignaciones;

    @MockBean
    private ServicioAutorizacion servicioAutorizacion;

    @MockBean
    private ServicioJwt servicioJwt;

    private RequestPostProcessor comoUsuario() {
        return authentication(new UsernamePasswordAuthenticationToken(PRINCIPAL, null, List.of()));
    }

    private void conPermiso(String codigo) {
        when(servicioAutorizacion.tienePermiso(PRINCIPAL.usuarioId(), codigo)).thenReturn(true);
    }

    private RolDto unRol() {
        return new RolDto(ROL, "CAJERO", "Cajero", "Registra ventas", true, true,
                Set.of("COMERCIAL_CREAR", "COMERCIAL_CONSULTAR"));
    }

    @Test
    @DisplayName("con el permiso de consulta devuelve los roles")
    void consultaRolesConPermiso() throws Exception {
        conPermiso("SEGURIDAD_CONSULTAR");
        when(servicioRoles.listar(EMPRESA)).thenReturn(List.of(unRol()));

        mockMvc.perform(get("/api/seguridad/roles").with(comoUsuario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("CAJERO"))
                .andExpect(jsonPath("$[0].permisos").isArray());
    }

    @Test
    @DisplayName("sin el permiso de consulta responde 403 con acceso denegado")
    void consultaRolesSinPermiso() throws Exception {
        mockMvc.perform(get("/api/seguridad/roles").with(comoUsuario()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"))
                .andExpect(jsonPath("$.mensaje").value("No tiene permiso para realizar esta operación"));
    }

    @Test
    @DisplayName("sin autenticación responde 401")
    void sinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/seguridad/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("editar la matriz exige el permiso de modificar, no alcanza con consultar")
    void editarLaMatrizExigeModificar() throws Exception {
        conPermiso("SEGURIDAD_CONSULTAR");

        mockMvc.perform(put("/api/seguridad/roles/" + ROL + "/permisos")
                        .with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permisos\":[\"COMERCIAL_CONSULTAR\"]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    @DisplayName("con el permiso de modificar se guarda la matriz")
    void editarLaMatrizConPermiso() throws Exception {
        conPermiso("SEGURIDAD_MODIFICAR");
        when(servicioRoles.reemplazarMatriz(eq(EMPRESA), eq(ROL), anyCollection(), any()))
                .thenReturn(unRol());

        mockMvc.perform(put("/api/seguridad/roles/" + ROL + "/permisos")
                        .with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permisos\":[\"COMERCIAL_CREAR\",\"COMERCIAL_CONSULTAR\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("CAJERO"));
    }

    @Test
    @DisplayName("una matriz sin la lista de permisos es una petición inválida")
    void matrizSinPermisos() throws Exception {
        conPermiso("SEGURIDAD_MODIFICAR");

        mockMvc.perform(put("/api/seguridad/roles/" + ROL + "/permisos")
                        .with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("activar o desactivar un rol exige el permiso de modificar")
    void cambiarEstadoConPermiso() throws Exception {
        conPermiso("SEGURIDAD_MODIFICAR");
        when(servicioRoles.cambiarEstado(eq(EMPRESA), eq(ROL), eq(false), any())).thenReturn(unRol());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/seguridad/roles/" + ROL + "/estado")
                        .with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("asignar un rol exige el permiso de crear y responde 201")
    void asignarRolConPermiso() throws Exception {
        conPermiso("SEGURIDAD_CREAR");

        mockMvc.perform(post("/api/seguridad/usuarios/" + OTRO_USUARIO + "/roles")
                        .with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rolId\":\"" + ROL + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("asignar un rol sin el permiso de crear responde 403")
    void asignarRolSinPermiso() throws Exception {
        conPermiso("SEGURIDAD_CONSULTAR");

        mockMvc.perform(post("/api/seguridad/usuarios/" + OTRO_USUARIO + "/roles")
                        .with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rolId\":\"" + ROL + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("asignar un rol que el usuario ya tiene responde 409 y no 500")
    void asignarRolRepetido() throws Exception {
        conPermiso("SEGURIDAD_CREAR");
        doThrow(new ReglaDeNegocio("ROL_YA_ASIGNADO", "El usuario ya tiene asignado el rol Cajero"))
                .when(servicioAsignaciones).asignar(eq(EMPRESA), eq(OTRO_USUARIO), eq(ROL), any());

        mockMvc.perform(post("/api/seguridad/usuarios/" + OTRO_USUARIO + "/roles")
                        .with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rolId\":\"" + ROL + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ROL_YA_ASIGNADO"));
    }

    @Test
    @DisplayName("un rol de otra empresa responde 404")
    void rolDeOtraEmpresa() throws Exception {
        conPermiso("SEGURIDAD_CREAR");
        doThrow(new RecursoNoEncontrado("ROL_NO_ENCONTRADO", "No existe ese rol en la empresa"))
                .when(servicioAsignaciones).asignar(eq(EMPRESA), eq(OTRO_USUARIO), eq(ROL), any());

        mockMvc.perform(post("/api/seguridad/usuarios/" + OTRO_USUARIO + "/roles")
                        .with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rolId\":\"" + ROL + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROL_NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("quitar un rol exige el permiso de anular y responde 204")
    void quitarRolConPermiso() throws Exception {
        conPermiso("SEGURIDAD_ANULAR");

        mockMvc.perform(delete("/api/seguridad/usuarios/" + OTRO_USUARIO + "/roles/" + ROL)
                        .with(comoUsuario()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("mis-permisos no exige ningún permiso: cualquiera consulta lo suyo")
    void misPermisosSinPermisosEspeciales() throws Exception {
        when(servicioAutorizacion.permisosEfectivos(PRINCIPAL.usuarioId()))
                .thenReturn(Set.of("COMERCIAL_CONSULTAR"));
        when(servicioAsignaciones.rolesDe(EMPRESA, PRINCIPAL.usuarioId())).thenReturn(List.of());

        mockMvc.perform(get("/api/seguridad/mis-permisos").with(comoUsuario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Administrador Demo"))
                .andExpect(jsonPath("$.permisos[0]").value("COMERCIAL_CONSULTAR"));
    }

    @Test
    @DisplayName("mis-permisos sin autenticación responde 401")
    void misPermisosSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/seguridad/mis-permisos"))
                .andExpect(status().isUnauthorized());
    }
}
