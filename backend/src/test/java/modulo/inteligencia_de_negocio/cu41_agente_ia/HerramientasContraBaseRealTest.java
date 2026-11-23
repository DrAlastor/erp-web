package modulo.inteligencia_de_negocio.cu41_agente_ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import comun.multitenancy.TenantDescriptor;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.service.MlClienteService;
import modulo.inteligencia_de_negocio.cu41_agente_ia.herramientas.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ejecuta las 24 herramientas contra una base de empresa REAL para validar su SQL.
 * Solo corre con AGENTE_DB_URL, AGENTE_DB_USER y AGENTE_DB_PASSWORD definidas (no en la suite normal).
 */
@EnabledIfEnvironmentVariable(named = "AGENTE_DB_URL", matches = ".+")
class HerramientasContraBaseRealTest {

    private static RegistroHerramientas registro;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TenantDescriptor EMPRESA = new TenantDescriptor(12L, "ASUSTE-FCF5A4");

    /** Argumentos por herramienta (las que no figuran se llaman sin argumentos). */
    private static final Map<String, String> ARGS = new LinkedHashMap<>();

    static {
        ARGS.put("ventas_por_periodo", "{\"agrupar\":\"mes\"}");
        ARGS.put("comparar_periodos", "{\"desde_a\":\"2026-09-01\",\"hasta_a\":\"2026-09-30\",\"desde_b\":\"2026-10-01\",\"hasta_b\":\"2026-10-31\"}");
        ARGS.put("top_productos", "{\"orden\":\"margen\",\"direccion\":\"menos\",\"limite\":5}");
        ARGS.put("ticket_promedio", "{\"agrupar\":\"sucursal\"}");
        ARGS.put("margen_por", "{\"agrupar\":\"categoria\"}");
        ARGS.put("descuentos_otorgados", "{\"agrupar\":\"mes\"}");
        ARGS.put("descomponer_variacion", "{\"desde_a\":\"2026-09-01\",\"hasta_a\":\"2026-09-30\",\"desde_b\":\"2026-10-01\",\"hasta_b\":\"2026-10-31\",\"dimension\":\"categoria\"}");
        ARGS.put("tasa_devolucion", "{\"agrupar\":\"producto\"}");
        ARGS.put("stock_actual", "{\"agrupar\":\"sucursal\"}");
        ARGS.put("rotacion_y_dias_inventario", "{\"agrupar\":\"categoria\"}");
        ARGS.put("candidatos_a_liquidar", "{\"meses_cobertura\":3}");
        ARGS.put("movimientos_de_inventario", "{\"agrupar\":\"tipo\"}");
        ARGS.put("pronosticar_ventas", "{\"categoria\":\"Laptops\"}");
        ARGS.put("sugerencia_de_compra", "{\"sucursal\":\"Equipetrol\",\"limite\":5}");
        ARGS.put("clientes_en_riesgo", "{\"nivel\":\"ALTO\",\"limite\":5}");
        ARGS.put("productos_que_se_compran_juntos", "{\"producto\":\"Router\"}");
        ARGS.put("anomalias_recientes", "{\"desde\":\"2025-01-01\"}");
        ARGS.put("clientes_nuevos_y_recurrencia", "{\"agrupar\":\"mes\"}");
        ARGS.put("cuentas_por_cobrar", "{}");
        ARGS.put("cuentas_por_pagar", "{\"dias\":60}");
        ARGS.put("compras_a_proveedores", "{\"agrupar\":\"mes\"}");
        ARGS.put("resultado_del_periodo", "{\"desde\":\"2026-01-01\",\"hasta\":\"2026-10-31\"}");
    }

    @BeforeAll
    static void preparar() {
        DriverManagerDataSource ds = new DriverManagerDataSource(System.getenv("AGENTE_DB_URL"),
                System.getenv("AGENTE_DB_USER"), System.getenv("AGENTE_DB_PASSWORD"));
        registro = new RegistroHerramientas(
                List.of(new VentasHerramientas(), new InventarioHerramientas(), new ModelosHerramientas(), new NegocioHerramientas()),
                new JdbcTemplate(ds), new DataSourceTransactionManager(ds),
                new MlClienteService(RestClient.builder(), "", "", 1000), JSON);
    }

    @Test
    void todasLasHerramientasCorrenContraLaBaseReal() throws Exception {
        LocalDate hoy = LocalDate.of(2026, 11, 23);
        StringBuilder informe = new StringBuilder();
        for (Herramienta h : registro.todas()) {
            String args = ARGS.getOrDefault(h.nombre(), "{}");
            RegistroHerramientas.Ejecucion e = registro.ejecutar(h.nombre(), JSON.readTree(args), EMPRESA, hoy);
            informe.append(String.format("%-34s ok=%-5s filas=%-4s grafico=%-7s %s%n", h.nombre(), e.ok(),
                    e.ok() ? e.resultado().filas().size() : "-", e.grafico() == null ? "-" : e.grafico().tipo(),
                    e.ok() ? e.paraModelo().toString().substring(0, Math.min(160, e.paraModelo().toString().length())) : e.error()));
            assertThat(e.ok()).as(h.nombre() + ": " + e.error()).isTrue();
        }
        System.out.println(informe);
    }

    @Test
    void laTransaccionEsDeSoloLectura() {
        ProveedorHerramientas maliciosa = () -> List.of(Herramienta.de("escribe", "intenta escribir")
                .ejecutar((a, c) -> ResultadoHerramienta.de(c.consultar(new Consulta().sql(
                        "INSERT INTO categorias (nombre, activo, fecha_creacion) VALUES ('X', TRUE, now()) RETURNING id")))));
        DriverManagerDataSource ds = new DriverManagerDataSource(System.getenv("AGENTE_DB_URL"),
                System.getenv("AGENTE_DB_USER"), System.getenv("AGENTE_DB_PASSWORD"));
        RegistroHerramientas r = new RegistroHerramientas(List.of(maliciosa), new JdbcTemplate(ds),
                new DataSourceTransactionManager(ds), new MlClienteService(RestClient.builder(), "", "", 1000), JSON);
        RegistroHerramientas.Ejecucion e = r.ejecutar("escribe", JSON.createObjectNode(), EMPRESA, LocalDate.now());
        assertThat(e.ok()).isFalse();
        assertThat(e.error()).contains("error interno");
    }
}
