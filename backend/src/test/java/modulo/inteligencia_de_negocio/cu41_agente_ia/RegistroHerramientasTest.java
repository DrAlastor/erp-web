package modulo.inteligencia_de_negocio.cu41_agente_ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import comun.multitenancy.TenantDescriptor;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.service.MlClienteService;
import modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RegistroHerramientasTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TenantDescriptor EMPRESA = new TenantDescriptor(12L, "ASUSTE-FCF5A4");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 23);

    private JdbcTemplate jdbc;
    private final List<Argumentos> recibidos = new ArrayList<>();

    /** Herramienta de prueba que devuelve 250 filas con una columna privada. */
    private final ProveedorHerramientas prueba = () -> List.of(
            Herramienta.de("prueba", "Herramienta de prueba")
                    .parametro("desde", Parametro.DESDE)
                    .parametro("nivel", Parametro.opciones("Nivel", "ALTO", "BAJO"))
                    .parametro("limite", Parametro.LIMITE)
                    .parametro("nombre", Parametro.texto("Texto"))
                    .parametro("activo", Parametro.logico("Bandera"))
                    .requeridos("nivel")
                    .visual(a -> Visual.barras("Prueba", "cliente", "monto"))
                    .ejecutar((a, c) -> {
                        recibidos.add(a);
                        List<Map<String, Object>> filas = new ArrayList<>();
                        for (int i = 0; i < 250; i++) {
                            Map<String, Object> f = new LinkedHashMap<>();
                            f.put("cliente", "Ana Perez " + i);
                            f.put("monto", i);
                            filas.add(f);
                        }
                        return ResultadoHerramienta.de(filas).conNota("nota").privadas("cliente");
                    }),
            Herramienta.de("rota", "Falla siempre").ejecutar((a, c) -> {
                throw new IllegalStateException("detalle interno secreto");
            }));

    private RegistroHerramientas registro;

    @BeforeEach
    void preparar() {
        jdbc = mock(JdbcTemplate.class);
        registro = new RegistroHerramientas(List.of(prueba), jdbc, mock(PlatformTransactionManager.class),
                mock(MlClienteService.class), JSON);
    }

    private RegistroHerramientas.Ejecucion ejecutar(String nombre, String args) throws Exception {
        return registro.ejecutar(nombre, JSON.readTree(args), EMPRESA, HOY);
    }

    @Test
    void ejecutaEnSoloLecturaConTiempoMaximo() throws Exception {
        RegistroHerramientas.Ejecucion e = ejecutar("prueba", "{\"nivel\":\"ALTO\",\"limite\":5,\"activo\":true}");
        assertThat(e.ok()).isTrue();
        verify(jdbc).execute("SET TRANSACTION READ ONLY");
        verify(jdbc).execute("SET LOCAL statement_timeout = 10000");
        assertThat(recibidos.getFirst().entero("limite", 0)).isEqualTo(5);
        assertThat(recibidos.getFirst().logico("activo", false)).isTrue();
    }

    @Test
    void lasColumnasPrivadasNoSeEnvianAGoogleYSeCortaEn200Filas() throws Exception {
        RegistroHerramientas.Ejecucion e = ejecutar("prueba", "{\"nivel\":\"ALTO\"}");
        String enviado = e.paraModelo().toString();
        assertThat(enviado).doesNotContain("Ana Perez");
        assertThat(e.paraModelo().path("filas")).hasSize(200);
        assertThat(e.paraModelo().path("total_filas").asInt()).isEqualTo(250);
        assertThat(e.paraModelo().path("aviso").asText()).contains("200 de 250");
        assertThat(e.paraModelo().path("nota").asText()).isEqualTo("nota");
        // la pantalla si recibe el nombre
        assertThat(e.resultado().filas().getFirst().get("cliente")).isEqualTo("Ana Perez 0");
        assertThat(e.grafico().tipo()).isEqualTo("barras");
    }

    @Test
    void rechazaParametrosNoDeclaradosComoIdDeEmpresa() throws Exception {
        RegistroHerramientas.Ejecucion e = ejecutar("prueba", "{\"nivel\":\"ALTO\",\"empresa_id\":99}");
        assertThat(e.ok()).isFalse();
        assertThat(e.error()).contains("'empresa_id' no existe");
        assertThat(recibidos).isEmpty();
    }

    @Test
    void validaRequeridosEnumsFechasRangosYTipos() throws Exception {
        assertThat(ejecutar("prueba", "{}").error()).contains("obligatorio 'nivel'");
        assertThat(ejecutar("prueba", "{\"nivel\":\"CRITICO\"}").error()).contains("uno de [ALTO, BAJO]");
        assertThat(ejecutar("prueba", "{\"nivel\":\"ALTO\",\"desde\":\"2026-02-30\"}").error()).contains("fecha real");
        assertThat(ejecutar("prueba", "{\"nivel\":\"ALTO\",\"limite\":500}").error()).contains("entre 1 y 100");
        assertThat(ejecutar("prueba", "{\"nivel\":\"ALTO\",\"limite\":2.5}").error()).contains("entero");
        assertThat(ejecutar("prueba", "{\"nivel\":\"ALTO\",\"limite\":\"5\"}").error()).contains("entero");
        assertThat(ejecutar("prueba", "{\"nivel\":\"ALTO\",\"nombre\":\"" + "x".repeat(101) + "\"}").error()).contains("100 caracteres");
        assertThat(ejecutar("prueba", "{\"nivel\":\"ALTO\",\"activo\":\"si\"}").error()).contains("true o false");
        assertThat(recibidos).isEmpty();
    }

    @Test
    void nuncaLanzaYNoFiltraDetallesInternos() throws Exception {
        RegistroHerramientas.Ejecucion e = ejecutar("rota", "{}");
        assertThat(e.ok()).isFalse();
        assertThat(e.paraModelo().toString()).doesNotContain("secreto").contains("error interno");
        assertThat(ejecutar("no_existe", "{}").error()).contains("no existe");
    }

    @Test
    void declaracionesTienenElFormatoDeGemini() {
        JsonNode d = registro.declaraciones();
        JsonNode prueba = d.get(0).get("name").asText().equals("prueba") ? d.get(0) : d.get(1);
        assertThat(prueba.path("parameters").path("type").asText()).isEqualTo("OBJECT");
        assertThat(prueba.path("parameters").path("properties").path("nivel").path("enum")).hasSize(2);
        assertThat(prueba.path("parameters").path("properties").path("limite").path("maximum").asInt()).isEqualTo(100);
        assertThat(prueba.path("parameters").path("required").get(0).asText()).isEqualTo("nivel");
    }

    @Test
    void consultaEscapaComodinesYNuncaConcatenaTexto() {
        Consulta q = new Consulta();
        q.sql("SELECT 1 FROM t WHERE 1 = 1").filtroContiene("nombre", "50%_off'; DROP TABLE ventas;--");
        assertThat(q.texto()).isEqualTo("SELECT 1 FROM t WHERE 1 = 1 AND nombre ILIKE ?");
        assertThat(q.parametros()[0]).isEqualTo("%50\\%\\_off'; DROP TABLE ventas;--%");
    }

    @Test
    void elCatalogoRealTiene24HerramientasConNombresUnicosYDescripciones() {
        List<ProveedorHerramientas> reales = List.of(new VentasHerramientas(), new InventarioHerramientas(),
                new ModelosHerramientas(), new NegocioHerramientas());
        RegistroHerramientas r = new RegistroHerramientas(reales, jdbc, mock(PlatformTransactionManager.class),
                mock(MlClienteService.class), JSON);
        Set<String> nombres = r.todas().stream().map(Herramienta::nombre).collect(Collectors.toSet());
        assertThat(nombres).hasSize(24).contains("ventas_por_periodo", "descomponer_variacion", "pronosticar_ventas",
                "clientes_en_riesgo", "resultado_del_periodo", "catalogos", "stock_actual");
        assertThat(r.todas()).allSatisfy(h -> {
            assertThat(h.nombre()).matches("[a-z_]+");
            assertThat(h.descripcion().length()).isGreaterThan(40);
            assertThat(h.parametros().keySet()).noneMatch(p -> p.contains("empresa"));
        });
        assertThat(r.declaraciones()).hasSize(24);
    }
}
