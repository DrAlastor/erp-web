package modulo.inteligencia_de_negocio.cu40_modelos_predictivos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import comun.multitenancy.TenantContext;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.dto.InteligenciaNegocioDtos.*;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.repository.ResultadosMlRepository;
import modulo.inteligencia_de_negocio.cu40_modelos_predictivos.repository.ResultadosMlRepository.EjecucionConMetricas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * CU40 - Inteligencia de Negocio: expone los resultados de los 6 modelos de Machine Learning.
 *
 * <p>Los modelos se entrenan en el servicio Python (ml-service) y dejan resultados precalculados
 * en las tablas {@code ml_*} de la base de la empresa; este servicio solo los lee. Si el ML se
 * cae, la pantalla sigue mostrando los ultimos resultados. Reentrenamiento: manual (boton) o
 * automatico al consultar el resumen cuando los resultados tienen mas de
 * {@code ml.reentrenar-cada-horas} horas.
 */
@Service
public class InteligenciaNegocioService {

    private static final Logger log = LoggerFactory.getLogger(InteligenciaNegocioService.class);

    static final String AVISO_SIN_ENTRENAR =
            "Los modelos todavia no se entrenaron para esta empresa. Usa \"Entrenar modelos\".";
    static final String AVISO_SIN_RESULTADOS = "No hay resultados para estos filtros.";
    static final Set<String> FRECUENCIAS = Set.of("MENSUAL", "SEMANAL");
    static final Set<String> NIVELES = Set.of("ALTO", "MEDIO", "BAJO");
    private static final int HORIZONTE_MENSUAL = 6;
    private static final int HORIZONTE_SEMANAL = 12;
    private static final int MAXIMO_HORIZONTE = 24;
    private static final Duration EN_CURSO_COLGADO = Duration.ofHours(2);

    /** clave, titulo, tipo, tecnica, pregunta de negocio. */
    static final List<String[]> CATALOGO = List.of(
            new String[]{"pronostico", "Pronostico de ventas", "Supervisado (series de tiempo)",
                    "Holt / Holt-Winters vs. modelo ingenuo", "¿Cuanto vamos a vender los proximos meses o semanas?"},
            new String[]{"reposicion", "Sugerencia de compra", "Prescriptivo",
                    "Pronostico por categoria + stock de seguridad (90 %)", "¿Que producto reponer, en que sucursal y cuantas unidades?"},
            new String[]{"abandono", "Riesgo de abandono", "Supervisado (clasificacion)",
                    "Random Forest vs. regresion logistica", "¿Que clientes probablemente no vuelvan a comprar en 90 dias?"},
            new String[]{"segmentacion", "Segmentacion de clientes", "No supervisado (clustering)",
                    "K-means sobre RFM", "¿Que grupos de clientes tenemos?"},
            new String[]{"canasta", "Canasta de mercado", "No supervisado (reglas de asociacion)",
                    "Apriori", "¿Que productos se compran juntos?"},
            new String[]{"anomalias", "Deteccion de anomalias", "No supervisado (deteccion de atipicos)",
                    "STL + Isolation Forest", "¿Que dias tuvo una sucursal una actividad rara?"});

    private final ResultadosMlRepository repo;
    private final MlClienteService ml;
    private final ObjectMapper json;
    private final Clock reloj;
    private final long reentrenarCadaHoras;

    @Autowired
    public InteligenciaNegocioService(ResultadosMlRepository repo, MlClienteService ml, ObjectMapper json,
                                      @Value("${ml.reentrenar-cada-horas:24}") long reentrenarCadaHoras) {
        this(repo, ml, json, Clock.system(ZoneId.of("America/La_Paz")), reentrenarCadaHoras);
    }

    InteligenciaNegocioService(ResultadosMlRepository repo, MlClienteService ml, ObjectMapper json, Clock reloj,
                               long reentrenarCadaHoras) {
        this.repo = repo;
        this.ml = ml;
        this.json = json;
        this.reloj = reloj;
        this.reentrenarCadaHoras = reentrenarCadaHoras;
    }

    // ------------------------------------------------------------------ resumen y entrenamiento

    public Resumen resumen() {
        boolean existen = repo.existenResultados();
        Optional<EjecucionConMetricas> ultima = existen ? repo.ultimaEjecucion() : Optional.empty();
        JsonNode metricas = existen ? leer(repo.metricasVigentes().orElse(null)) : null;

        boolean automatico = false;
        if (ml.configurado() && necesitaEntrenar(ultima.map(EjecucionConMetricas::ejecucion).orElse(null))) {
            try {
                automatico = ml.entrenar(TenantContext.require());
            } catch (Exception e) {
                log.warn("CU40: no se pudo encolar el reentrenamiento automatico: {}", e.getMessage());
            }
        }

        List<ModeloInfo> modelos = new ArrayList<>();
        for (String[] m : CATALOGO) {
            JsonNode met = metricas == null ? null : metricas.get(m[0]);
            modelos.add(new ModeloInfo(m[0], m[1], m[2], m[3], m[4], met));
        }
        return new Resumen(ml.configurado(), metricas != null, automatico,
                ultima.map(EjecucionConMetricas::ejecucion).orElse(null), modelos);
    }

    boolean necesitaEntrenar(Ejecucion ultima) {
        if (ultima == null) return true;
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if ("EN_CURSO".equals(ultima.estado())) {
            return ultima.inicio() != null && ultima.inicio().plus(EN_CURSO_COLGADO).isBefore(ahora);
        }
        return ultima.inicio() == null || ultima.inicio().plusHours(reentrenarCadaHoras).isBefore(ahora);
    }

    public EntrenarResponse entrenar() {
        if (!ml.configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El servicio de Machine Learning no esta configurado (ML_URL / ML_CLAVE_INTERNA).");
        }
        try {
            boolean encolado = ml.entrenar(TenantContext.require());
            return new EntrenarResponse(encolado, encolado
                    ? "Entrenamiento encolado. Los resultados se actualizan en uno o dos minutos."
                    : "Ya hay un entrenamiento pendiente para esta empresa.");
        } catch (MlClienteService.MlNoDisponibleException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    public Opciones opciones() {
        return new Opciones(repo.sucursales(), repo.categorias());
    }

    // ------------------------------------------------------------------ pronostico

    public Pronostico pronostico(String frecuencia, Long sucursalId, String categoria, Integer horizonte) {
        String f = frecuencia == null ? "MENSUAL" : frecuencia.trim().toUpperCase();
        if (!FRECUENCIAS.contains(f)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Frecuencia invalida: use MENSUAL o SEMANAL");
        }
        if (sucursalId != null && categoria != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtre por sucursal o por categoria, no ambas");
        }
        int precalculado = "MENSUAL".equals(f) ? HORIZONTE_MENSUAL : HORIZONTE_SEMANAL;
        int h = horizonte == null ? precalculado : horizonte;
        if (h < 1 || h > MAXIMO_HORIZONTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El horizonte debe estar entre 1 y 24");
        }
        String cat = blancoANull(categoria);
        String ambito = sucursalId != null ? "SUCURSAL" : (cat != null ? "CATEGORIA" : "TOTAL");
        String clave = sucursalId != null ? String.valueOf(sucursalId) : (cat != null ? cat : "TOTAL");

        List<PuntoHistoria> historia = historia(f, sucursalId, cat);
        boolean existen = repo.existenResultados();
        JsonNode metricas = existen ? leer(repo.metricasVigentes().orElse(null)) : null;
        Double mape = null;
        Double mapeIngenuo = null;
        if ("TOTAL".equals(ambito) && "MENSUAL".equals(f) && metricas != null) {
            mape = numero(metricas.path("pronostico").path("mape_total_mensual"));
            mapeIngenuo = numero(metricas.path("pronostico").path("mape_ingenuo_total_mensual"));
        }

        if (h > precalculado) {
            JsonNode r = ml.pronostico(TenantContext.require(), ambito, clave, f, h);
            if (!r.path("disponible").asBoolean(false)) {
                return new Pronostico(metricas != null, false, r.path("motivo").asText("No disponible"), f, ambito,
                        clave, null, "AL_MOMENTO", null, null, historia, List.of());
            }
            List<PuntoPronostico> puntos = new ArrayList<>();
            for (JsonNode p : r.path("puntos")) {
                puntos.add(new PuntoPronostico(LocalDate.parse(p.path("periodo").asText()),
                        p.path("valor").decimalValue(), p.path("bajo").decimalValue(), p.path("alto").decimalValue()));
            }
            return new Pronostico(metricas != null, true, null, f, ambito, clave, r.path("modelo").asText(null),
                    "AL_MOMENTO", numero(r.path("mape")), numero(r.path("mapeIngenuo")), historia, puntos);
        }

        if (!existen || metricas == null) {
            return new Pronostico(false, false, AVISO_SIN_ENTRENAR, f, ambito, clave, null, "PRECALCULADO",
                    null, null, historia, List.of());
        }
        List<PuntoPronostico> puntos = repo.pronostico(ambito, clave, f, h);
        if (puntos.isEmpty()) {
            return new Pronostico(true, false,
                    "Esta serie no tiene suficientes periodos completos para pronosticar.", f, ambito, clave, null,
                    "PRECALCULADO", null, null, historia, List.of());
        }
        return new Pronostico(true, true, null, f, ambito, clave, repo.modeloDeSerie(ambito, clave, f).orElse(null),
                "PRECALCULADO", mape, mapeIngenuo, historia, puntos);
    }

    /** Ultimos 24 meses o 52 semanas COMPLETOS, con ceros en periodos sin ventas. */
    List<PuntoHistoria> historia(String frecuencia, Long sucursalId, String categoria) {
        LocalDate hoy = LocalDate.now(reloj);
        boolean semanal = "SEMANAL".equals(frecuencia);
        LocalDate actual = semanal ? hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : hoy.withDayOfMonth(1);
        LocalDate desde = semanal ? actual.minusWeeks(52) : actual.minusMonths(24);
        Map<LocalDate, BigDecimal> datos = repo.historiaVentas(frecuencia, desde, actual, sucursalId, categoria);
        List<PuntoHistoria> serie = new ArrayList<>();
        for (LocalDate p = desde; p.isBefore(actual); p = semanal ? p.plusWeeks(1) : p.plusMonths(1)) {
            serie.add(new PuntoHistoria(p, datos.getOrDefault(p, BigDecimal.ZERO)));
        }
        return serie;
    }

    // ------------------------------------------------------------------ clientes

    public ClientesRiesgo clientesEnRiesgo(String nivel, Integer limite) {
        String n = blancoANull(nivel);
        if (n != null) {
            n = n.toUpperCase();
            if (!NIVELES.contains(n)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel invalido: use ALTO, MEDIO o BAJO");
            }
        }
        int max = limite(limite, 100);
        if (!repo.existenResultados()) {
            return new ClientesRiesgo(false, AVISO_SIN_ENTRENAR, Map.of(), List.of());
        }
        List<ClienteRiesgo> clientes = repo.clientesEnRiesgo(n, max).stream()
                .map(f -> new ClienteRiesgo(f.clienteId(), f.cliente(), f.nitCi(), f.probabilidad(), f.nivel(),
                        leer(f.factoresJson())))
                .toList();
        Map<String, Long> porNivel = repo.conteoPorNivel();
        boolean entrenado = porNivel.values().stream().mapToLong(Long::longValue).sum() > 0 || entrenado();
        return new ClientesRiesgo(entrenado, clientes.isEmpty() ? avisoVacio(entrenado) : null, porNivel, clientes);
    }

    public Segmentos segmentos(String segmento, Integer limite) {
        int max = limite(limite, 100);
        if (!repo.existenResultados()) {
            return new Segmentos(false, AVISO_SIN_ENTRENAR, List.of(), List.of());
        }
        List<Segmento> segmentos = repo.segmentos();
        boolean entrenado = entrenado();
        return new Segmentos(entrenado, segmentos.isEmpty() ? avisoVacio(entrenado) : null, segmentos,
                repo.clientesDeSegmento(blancoANull(segmento), max));
    }

    // ------------------------------------------------------------------ productos y sucursales

    public Lista<ReglaAsociacion> canasta(String producto, Integer limite) {
        int max = limite(limite, 50);
        if (!repo.existenResultados()) return new Lista<>(false, AVISO_SIN_ENTRENAR, List.of());
        return lista(repo.reglas(blancoANull(producto), max));
    }

    public Lista<Anomalia> anomalias(LocalDate desde, LocalDate hasta, Long sucursalId, Integer limite) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a hasta");
        }
        int max = limite(limite, 100);
        if (!repo.existenResultados()) return new Lista<>(false, AVISO_SIN_ENTRENAR, List.of());
        return lista(repo.anomalias(desde, hasta, sucursalId, max));
    }

    public Lista<SugerenciaCompra> sugerenciaCompra(Long sucursalId, String categoria, Integer limite) {
        int max = limite(limite, 100);
        if (!repo.existenResultados()) return new Lista<>(false, AVISO_SIN_ENTRENAR, List.of());
        return lista(repo.sugerencias(sucursalId, blancoANull(categoria), max));
    }

    // ------------------------------------------------------------------ internos

    private <T> Lista<T> lista(List<T> items) {
        boolean entrenado = entrenado();
        return new Lista<>(entrenado, items.isEmpty() ? avisoVacio(entrenado) : null, items);
    }

    private boolean entrenado() {
        return repo.metricasVigentes().isPresent();
    }

    private static String avisoVacio(boolean entrenado) {
        return entrenado ? AVISO_SIN_RESULTADOS : AVISO_SIN_ENTRENAR;
    }

    static int limite(Integer pedido, int porDefecto) {
        if (pedido == null) return porDefecto;
        if (pedido < 1 || pedido > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El limite debe estar entre 1 y 500");
        }
        return pedido;
    }

    private static String blancoANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static Double numero(JsonNode n) {
        return n == null || n.isMissingNode() || n.isNull() ? null : n.asDouble();
    }

    private JsonNode leer(String texto) {
        if (texto == null) return null;
        try {
            return json.readTree(texto);
        } catch (Exception e) {
            return JsonNodeFactory.instance.nullNode();
        }
    }
}
