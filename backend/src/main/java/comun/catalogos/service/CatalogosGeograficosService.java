package comun.catalogos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import comun.catalogos.dto.MonedaCatalogoResponse;
import comun.catalogos.dto.PaisCatalogoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class CatalogosGeograficosService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String countriesApiUrl;
    private final String countriesApiToken;
    private final Duration cacheDuration;
    private final ReentrantLock cacheLock = new ReentrantLock();

    private volatile List<PaisCatalogoResponse> paisesCache;
    private volatile Instant cacheExpiresAt = Instant.MIN;

    public CatalogosGeograficosService(
            RestTemplate catalogosRestTemplate,
            ObjectMapper objectMapper,
            @Value("${catalogos.paises.url:https://api.restcountries.com/countries/v5}") String countriesApiUrl,
            @Value("${catalogos.paises.token:}") String countriesApiToken,
            @Value("${catalogos.api.cache-minutes:360}") long cacheMinutes) {
        this.restTemplate = catalogosRestTemplate;
        this.objectMapper = objectMapper;
        this.countriesApiUrl = countriesApiUrl;
        this.countriesApiToken = countriesApiToken;
        this.cacheDuration = Duration.ofMinutes(cacheMinutes);
    }

    public List<PaisCatalogoResponse> listarPaises() {
        cargarCacheSiEsNecesario();
        return paisesCache;
    }

    public List<MonedaCatalogoResponse> listarMonedas() {
        Map<String, MonedaCatalogoResponse> monedas = new LinkedHashMap<>();
        listarPaises().forEach(pais -> pais.monedas().forEach(moneda -> monedas.putIfAbsent(moneda.codigo(), moneda)));
        return monedas.values().stream()
                .sorted(Comparator.comparing(MonedaCatalogoResponse::codigo))
                .toList();
    }

    public PaisCatalogoResponse buscarPais(String codigoIso2) {
        return listarPaises().stream()
                .filter(pais -> pais.codigoIso2().equalsIgnoreCase(codigoIso2))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(SERVICE_UNAVAILABLE, "País no encontrado"));
    }

    private void cargarCacheSiEsNecesario() {
        if (paisesCache != null && Instant.now().isBefore(cacheExpiresAt)) {
            return;
        }

        cacheLock.lock();
        try {
            if (paisesCache != null && Instant.now().isBefore(cacheExpiresAt)) {
                return;
            }
            paisesCache = consultarPaises();
            cacheExpiresAt = Instant.now().plus(cacheDuration);
        } finally {
            cacheLock.unlock();
        }
    }

    private List<PaisCatalogoResponse> consultarPaises() {
        HttpHeaders headers = new HttpHeaders();
        if (!countriesApiToken.isBlank()) {
            headers.setBearerAuth(countriesApiToken);
        }

        try {
            List<PaisCatalogoResponse> result = new ArrayList<>();
            int offset = 0;
            boolean more;
            do {
                JsonNode root = requestPage(headers, offset);
                JsonNode countries = root.isArray() ? root : root.path("data").path("objects");
                if (!countries.isArray()) {
                    throw new IllegalStateException("Respuesta inv\u00e1lida del cat\u00e1logo de pa\u00edses");
                }
                countries.forEach(country -> addCountry(result, country));
                JsonNode meta = root.path("data").path("meta");
                more = !root.isArray() && meta.path("more").asBoolean(false);
                offset += meta.path("count").asInt(countries.size());
            } while (more);

            if (result.isEmpty()) {
                throw new IllegalStateException("El cat\u00e1logo externo no devolvi\u00f3 pa\u00edses");
            }

            return result.stream().sorted(Comparator.comparing(PaisCatalogoResponse::nombre)).toList();
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "El catálogo externo de países no está disponible",
                    exception);
        }
    }

    private JsonNode requestPage(HttpHeaders headers, int offset) throws Exception {
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(countriesApiUrl);
        if (countriesApiUrl.contains("/v5")) {
            if (countriesApiToken.isBlank()) {
                throw new IllegalStateException("REST_COUNTRIES_API_TOKEN es obligatorio para REST Countries v5");
            }
            uri.replaceQueryParam("limit", 100)
                    .replaceQueryParam("offset", offset)
                    .replaceQueryParam(
                            "response_fields",
                            "names.common,codes.alpha_2,codes.alpha_3,currencies");
        }
        String response = restTemplate.exchange(
                uri.build(true).toUri(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class).getBody();
        return objectMapper.readTree(response);
    }

    private void addCountry(List<PaisCatalogoResponse> result, JsonNode country) {
        boolean v5 = country.has("codes");
        String codigoIso2 = v5
                ? country.path("codes").path("alpha_2").asText("")
                : country.path("cca2").asText("");
        String codigoIso3 = v5
                ? country.path("codes").path("alpha_3").asText("")
                : country.path("cca3").asText("");
        String nombre = v5
                ? country.path("names").path("common").asText("")
                : country.path("name").path("common").asText("");
        Map<String, MonedaCatalogoResponse> currencies = new LinkedHashMap<>();
        JsonNode currencyNode = country.path("currencies");

        if (currencyNode.isObject()) {
            currencyNode.fields().forEachRemaining(entry -> addCurrency(currencies, entry.getKey(), entry.getValue()));
        } else if (currencyNode.isArray()) {
            currencyNode.forEach(currency -> addCurrency(
                    currencies,
                    currency.path("code").asText(""),
                    currency));
        }

        if (!codigoIso2.isBlank() && !nombre.isBlank()) {
            result.add(new PaisCatalogoResponse(
                    codigoIso2,
                    codigoIso3,
                    nombre,
                    currencies.values().stream()
                            .sorted(Comparator.comparing(MonedaCatalogoResponse::codigo))
                            .toList()));
        }
    }

    private void addCurrency(
            Map<String, MonedaCatalogoResponse> currencies,
            String code,
            JsonNode currency) {
        if (code == null || code.isBlank()) {
            return;
        }
        String normalizedCode = code.toUpperCase();
        currencies.putIfAbsent(normalizedCode, new MonedaCatalogoResponse(
                normalizedCode,
                currency.path("name").asText(normalizedCode),
                currency.path("symbol").asText("")));
    }
}
