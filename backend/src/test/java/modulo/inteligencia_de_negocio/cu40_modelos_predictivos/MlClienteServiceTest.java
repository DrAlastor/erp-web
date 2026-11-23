package modulo.inteligencia_de_negocio.cu40_modelos_predictivos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import comun.multitenancy.TenantDescriptor;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.service.MlClienteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba el cliente del servicio de ML contra un servidor HTTP que lo imita. */
class MlClienteServiceTest {

    private final ObjectMapper json = new ObjectMapper();
    private final List<JsonNode> cuerpos = new ArrayList<>();
    private final List<String> claves = new ArrayList<>();
    private HttpServer servidor;
    private static final TenantDescriptor EMPRESA = new TenantDescriptor(12L, "asuste-fcf5a4");

    private MlClienteService cliente(String clave, int estado, String respuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", ex -> {
            cuerpos.add(json.readTree(ex.getRequestBody()));
            claves.add(ex.getRequestHeaders().getFirst("X-Clave-Interna"));
            byte[] b = respuesta.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(estado, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        servidor.start();
        return new MlClienteService(RestClient.builder(),
                "http://127.0.0.1:" + servidor.getAddress().getPort() + "/", clave, 3000);
    }

    @AfterEach
    void detener() {
        if (servidor != null) servidor.stop(0);
    }

    @Test
    void entrenarEnviaLaEmpresaDelTokenYLaClave() throws Exception {
        MlClienteService c = cliente("secreta", 202, "{\"encolado\": true}");
        assertThat(c.entrenar(EMPRESA)).isTrue();
        assertThat(claves).containsExactly("secreta");
        assertThat(cuerpos.getFirst().path("idEmpresa").asLong()).isEqualTo(12);
        assertThat(cuerpos.getFirst().path("codigoEmpresa").asText()).isEqualTo("ASUSTE-FCF5A4");
    }

    @Test
    void entrenarYaEnColaDevuelveFalse() throws Exception {
        assertThat(cliente("secreta", 202, "{\"encolado\": false}").entrenar(EMPRESA)).isFalse();
    }

    @Test
    void sinClaveNoEstaConfiguradoYNoLlama() throws Exception {
        MlClienteService c = cliente("", 202, "{}");
        assertThat(c.configurado()).isFalse();
        assertThatThrownBy(() -> c.entrenar(EMPRESA)).isInstanceOf(MlClienteService.MlNoDisponibleException.class);
        assertThat(cuerpos).isEmpty();
    }

    @Test
    void claveRechazadaLanzaNoDisponible() throws Exception {
        MlClienteService c = cliente("mala", 401, "{\"detail\":\"Clave interna invalida\"}");
        assertThatThrownBy(() -> c.entrenar(EMPRESA))
                .isInstanceOf(MlClienteService.MlNoDisponibleException.class)
                .hasMessageContaining("clave interna");
    }

    @Test
    void pronosticoEnviaParametrosYDevuelvePuntos() throws Exception {
        MlClienteService c = cliente("secreta", 200,
                "{\"disponible\":true,\"modelo\":\"HOLT\",\"puntos\":[{\"periodo\":\"2026-12-01\",\"valor\":10,\"bajo\":5,\"alto\":15}]}");
        JsonNode r = c.pronostico(EMPRESA, "SUCURSAL", "2", "MENSUAL", 18);
        assertThat(r.path("disponible").asBoolean()).isTrue();
        JsonNode enviado = cuerpos.getFirst();
        assertThat(enviado.path("ambito").asText()).isEqualTo("SUCURSAL");
        assertThat(enviado.path("clave").asText()).isEqualTo("2");
        assertThat(enviado.path("horizonte").asInt()).isEqualTo(18);
    }

    @Test
    void pronosticoNuncaLanzaSiElServicioFalla() throws Exception {
        MlClienteService c = cliente("secreta", 500, "{}");
        JsonNode r = c.pronostico(EMPRESA, "TOTAL", "TOTAL", "MENSUAL", 18);
        assertThat(r.path("disponible").asBoolean()).isFalse();
        assertThat(r.path("motivo").asText()).contains("HTTP 500");
    }

    @Test
    void servicioCaidoNoRompe() {
        MlClienteService c = new MlClienteService(RestClient.builder(), "http://127.0.0.1:1", "secreta", 1000);
        assertThat(c.pronostico(EMPRESA, "TOTAL", "TOTAL", "MENSUAL", 18).path("disponible").asBoolean()).isFalse();
        assertThatThrownBy(() -> c.entrenar(EMPRESA)).isInstanceOf(MlClienteService.MlNoDisponibleException.class);
    }
}
