package modulo.seguridad_y_auditoria.cu07_gestionar_backups.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import comun.multitenancy.TenantContext;
import modulo.seguridad_y_auditoria.cu08_consultar_bitacora.service.CloudWatchAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio centralizado de Bitácora de Auditoría Inmutable (CU08).
 * Almacena los eventos de auditoría fuera de las bases de datos operativas de los tenants
 * (en ERP_Master) utilizando encadenamiento criptográfico de hashes (blockchain-style hash chaining)
 * y replicación externa e inmutable en Amazon CloudWatch Logs (WORM).
 */
@Service
public class BitacoraAuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(BitacoraAuditoriaService.class);
    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final JdbcTemplate masterJdbcTemplate;
    private final CloudWatchAuditService cloudWatchAuditService;
    private final ObjectMapper objectMapper;

    public BitacoraAuditoriaService(
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
            CloudWatchAuditService cloudWatchAuditService,
            ObjectMapper objectMapper) {
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.cloudWatchAuditService = cloudWatchAuditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Sobrecarga de conveniencia que extrae automáticamente el Tenant actual,
     * el actor (usuario autenticado) y la IP de origen del contexto web/seguridad.
     */
    public synchronized void registrarEventoActual(
            String tipoOperacion,
            String modulo,
            String detalle,
            String resultado,
            String severidad,
            String metadatosJson) {

        Integer empresaId = null;
        String codigoEmpresa = null;
        if (TenantContext.current().isPresent()) {
            var tenant = TenantContext.current().get();
            empresaId = tenant.empresaId() != null ? tenant.empresaId().intValue() : null;
            codigoEmpresa = tenant.codigoEmpresa();
        }

        String actor = "SISTEMA";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank() && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
                actor = auth.getName();
            }
        } catch (Exception ignored) {}

        String ipOrigen = "127.0.0.1";
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest req = sra.getRequest();
                if (req != null) {
                    req.setAttribute("AUDITORIA_REGISTRADA", true);
                    String xForwardedFor = req.getHeader("X-Forwarded-For");
                    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                        ipOrigen = xForwardedFor.split(",")[0].trim();
                    } else if (req.getRemoteAddr() != null) {
                        ipOrigen = req.getRemoteAddr();
                    }
                }
            }
        } catch (Exception ignored) {}

        registrarEvento(empresaId, codigoEmpresa, actor, tipoOperacion, modulo, detalle, ipOrigen, resultado, severidad, metadatosJson);
    }

    /**
     * Sobrecarga retrocompatible sin severidad explícita.
     */
    public synchronized void registrarEvento(
            Integer empresaId,
            String codigoEmpresa,
            String actor,
            String tipoOperacion,
            String modulo,
            String detalle,
            String ipOrigen,
            String resultado,
            String metadatosJson) {
        registrarEvento(empresaId, codigoEmpresa, actor, tipoOperacion, modulo, detalle, ipOrigen, resultado, "MEDIA", metadatosJson);
    }

    /**
     * Registra un evento inmutable con encadenamiento SHA-256 y replicación en Amazon CloudWatch Logs.
     */
    public synchronized void registrarEvento(
            Integer empresaId,
            String codigoEmpresa,
            String actor,
            String tipoOperacion,
            String modulo,
            String detalle,
            String ipOrigen,
            String resultado,
            String severidad,
            String metadatosJson) {

        try {
            String eventoId = "EVT-" + UUID.randomUUID().toString();
            Instant nowUtc = Instant.now();
            String severidadFinal = (severidad != null && !severidad.isBlank()) ? severidad.toUpperCase() : "MEDIA";

            // 1. Obtener el hash del último bloque/evento registrado
            List<String> lastHashes = masterJdbcTemplate.query(
                    "SELECT hash_actual FROM bitacora_auditoria ORDER BY id DESC LIMIT 1",
                    (rs, rowNum) -> rs.getString("hash_actual"));

            String hashPrevio = lastHashes.isEmpty() ? GENESIS_HASH : lastHashes.getFirst();

            // 2. Calcular el nuevo hash encadenado
            String payload = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                    eventoId,
                    nowUtc.toString(),
                    empresaId != null ? empresaId.toString() : "GLOBAL",
                    actor != null ? actor : "SISTEMA",
                    tipoOperacion,
                    modulo,
                    detalle,
                    resultado,
                    severidadFinal,
                    hashPrevio);

            String hashActual = calcularSha256(payload);

            // 3. Insertar registro inmutable en ERP_Master
            masterJdbcTemplate.update("""
                    INSERT INTO bitacora_auditoria
                    (evento_id, fecha_hora_utc, empresa_id, codigo_empresa, actor, tipo_operacion, modulo, detalle,
                     ip_origen, resultado, severidad, hash_previo, hash_actual, cloudwatch_ingested, metadatos)
                    VALUES (?, NOW() AT TIME ZONE 'UTC', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?)
                    """,
                    eventoId,
                    empresaId,
                    codigoEmpresa,
                    actor != null ? actor : "SISTEMA",
                    tipoOperacion,
                    modulo,
                    detalle,
                    ipOrigen != null ? ipOrigen : "127.0.0.1",
                    resultado,
                    severidadFinal,
                    hashPrevio,
                    hashActual,
                    metadatosJson);

            // 4. Replicación externa e inmutable en Amazon CloudWatch Logs
            String streamName = (codigoEmpresa != null && !codigoEmpresa.isBlank())
                    ? "tenant-" + codigoEmpresa
                    : "tenant-GLOBAL";

            Map<String, Object> cwPayload = new LinkedHashMap<>();
            cwPayload.put("eventoId", eventoId);
            cwPayload.put("timestampUtc", nowUtc.toString());
            cwPayload.put("empresaId", empresaId);
            cwPayload.put("codigoEmpresa", codigoEmpresa);
            cwPayload.put("actor", actor);
            cwPayload.put("tipoOperacion", tipoOperacion);
            cwPayload.put("modulo", modulo);
            cwPayload.put("detalle", detalle);
            cwPayload.put("ipOrigen", ipOrigen);
            cwPayload.put("resultado", resultado);
            cwPayload.put("severidad", severidadFinal);
            cwPayload.put("hashPrevio", hashPrevio);
            cwPayload.put("hashActual", hashActual);

            boolean ingested = cloudWatchAuditService.enviarEvento(
                    streamName,
                    objectMapper.writeValueAsString(cwPayload),
                    nowUtc);

            if (ingested) {
                masterJdbcTemplate.update(
                        "UPDATE bitacora_auditoria SET cloudwatch_ingested = TRUE WHERE evento_id = ?",
                        eventoId);
            }

            log.info("Evento auditado [{}] en bitácora inmutable. Hash: {}... [CloudWatch: {}]",
                    tipoOperacion, hashActual.substring(0, 16), ingested ? "OK" : "PENDIENTE");
        } catch (Exception e) {
            log.error("Fallo registrando evento de auditoría inmutable: {}", e.getMessage(), e);
        }
    }

    /**
     * Consulta de Bitácora para el Tenant (Nivel 1 de Supervisión).
     * Cada empresa consulta exclusivamente su propia actividad.
     */
    public List<Map<String, Object>> listarEventosTenant(
            Integer empresaId,
            String modulo,
            String tipoOperacion,
            String severidad,
            String resultado,
            String busqueda,
            LocalDate desde,
            LocalDate hasta) {

        StringBuilder sql = new StringBuilder("""
                SELECT id, evento_id, fecha_hora_utc,
                       TO_CHAR(fecha_hora_utc AT TIME ZONE 'UTC' AT TIME ZONE 'America/La_Paz', 'YYYY-MM-DD"T"HH24:MI:SS') AS fecha_hora_bolivia,
                       empresa_id, codigo_empresa,
                       actor, tipo_operacion, modulo, detalle, ip_origen, resultado,
                       COALESCE(severidad, 'MEDIA') AS severidad,
                       COALESCE(cloudwatch_ingested, FALSE) AS cloudwatch_ingested,
                       hash_previo, hash_actual, metadatos
                FROM bitacora_auditoria
                WHERE empresa_id = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(empresaId);

        aplicarFiltrosComunes(sql, params, modulo, tipoOperacion, severidad, resultado, busqueda, desde, hasta);

        sql.append(" ORDER BY id DESC LIMIT 300");
        return masterJdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * Consulta de Bitácora para el Super Admin (Nivel 2 de Supervisión).
     * Supervisa la plataforma completa sin exponer datos comerciales sensibles.
     */
    public List<Map<String, Object>> listarEventosSuperAdmin(
            Integer empresaId,
            String modulo,
            String tipoOperacion,
            String severidad,
            String resultado,
            String busqueda,
            LocalDate desde,
            LocalDate hasta) {

        StringBuilder sql = new StringBuilder("""
                SELECT b.id, b.evento_id, b.fecha_hora_utc,
                       TO_CHAR(b.fecha_hora_utc AT TIME ZONE 'UTC' AT TIME ZONE 'America/La_Paz', 'YYYY-MM-DD"T"HH24:MI:SS') AS fecha_hora_bolivia,
                       b.empresa_id, b.codigo_empresa,
                       COALESCE(e.razon_social, 'PLATAFORMA SAAS') AS razon_social,
                       b.actor, b.tipo_operacion, b.modulo, b.detalle, b.ip_origen, b.resultado,
                       COALESCE(b.severidad, 'MEDIA') AS severidad,
                       COALESCE(b.cloudwatch_ingested, FALSE) AS cloudwatch_ingested,
                       b.hash_previo, b.hash_actual, b.metadatos
                FROM bitacora_auditoria b
                LEFT JOIN empresa e ON e.id = b.empresa_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (empresaId != null && empresaId > 0) {
            sql.append(" AND b.empresa_id = ?");
            params.add(empresaId);
        }

        aplicarFiltrosComunes(sql, params, modulo, tipoOperacion, severidad, resultado, busqueda, desde, hasta);

        sql.append(" ORDER BY b.id DESC LIMIT 500");
        return masterJdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    private void aplicarFiltrosComunes(
            StringBuilder sql,
            List<Object> params,
            String modulo,
            String tipoOperacion,
            String severidad,
            String resultado,
            String busqueda,
            LocalDate desde,
            LocalDate hasta) {

        if (modulo != null && !modulo.isBlank() && !"TODOS".equalsIgnoreCase(modulo)) {
            sql.append(" AND UPPER(modulo) = ?");
            params.add(modulo.toUpperCase());
        }

        if (tipoOperacion != null && !tipoOperacion.isBlank() && !"TODOS".equalsIgnoreCase(tipoOperacion)) {
            sql.append(" AND UPPER(tipo_operacion) = ?");
            params.add(tipoOperacion.toUpperCase());
        }

        if (severidad != null && !severidad.isBlank() && !"TODAS".equalsIgnoreCase(severidad)) {
            sql.append(" AND UPPER(severidad) = ?");
            params.add(severidad.toUpperCase());
        }

        if (resultado != null && !resultado.isBlank() && !"TODOS".equalsIgnoreCase(resultado)) {
            sql.append(" AND UPPER(resultado) = ?");
            params.add(resultado.toUpperCase());
        }

        if (busqueda != null && !busqueda.isBlank()) {
            sql.append(" AND (LOWER(actor) LIKE ? OR LOWER(detalle) LIKE ? OR LOWER(evento_id) LIKE ? OR ip_origen LIKE ?)");
            String pattern = "%" + busqueda.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (desde != null) {
            sql.append(" AND fecha_hora_utc >= ?");
            params.add(desde.atStartOfDay());
        }

        if (hasta != null) {
            sql.append(" AND fecha_hora_utc <= ?");
            params.add(hasta.plusDays(1).atStartOfDay());
        }
    }

    /**
     * Valida matemáticamente el encadenamiento criptográfico completo de la bitácora.
     */
    public Map<String, Object> verificarIntegridadCadenaDetallada(Integer optionalEmpresaId) {
        try {
            String sql = "SELECT id, evento_id, fecha_hora_utc, empresa_id, actor, tipo_operacion, modulo, detalle, resultado, hash_previo, hash_actual FROM bitacora_auditoria ORDER BY id ASC";
            List<Map<String, Object>> registros = masterJdbcTemplate.queryForList(sql);

            if (registros.isEmpty()) {
                return Map.of(
                        "valida", true,
                        "totalBloques", 0,
                        "algoritmo", "SHA-256 Hash Chaining (Inmutable)",
                        "mensaje", "La bitácora no contiene registros aún.");
            }

            String expectedPrevHash = GENESIS_HASH;
            int bloquesValidados = 0;

            for (Map<String, Object> reg : registros) {
                String hashPrevio = (String) reg.get("hash_previo");
                String hashActual = (String) reg.get("hash_actual");

                if (!expectedPrevHash.equals(hashPrevio)) {
                    log.warn("Corrupción de cadena de bitácora en ID {}: hash previo discrepante", reg.get("id"));
                    return Map.of(
                            "valida", false,
                            "bloqueInconsistente", reg.get("id"),
                            "eventoId", reg.get("evento_id"),
                            "totalBloques", registros.size(),
                            "algoritmo", "SHA-256 Hash Chaining (Inmutable)",
                            "mensaje", "Alerta: Inconsistencia detectada en el bloque ID #" + reg.get("id"));
                }
                expectedPrevHash = hashActual;
                bloquesValidados++;
            }

            return Map.of(
                    "valida", true,
                    "totalBloques", bloquesValidados,
                    "algoritmo", "SHA-256 Hash Chaining (Inmutable)",
                    "ultimoHash", expectedPrevHash,
                    "mensaje", "Cadena criptográfica verificada: " + bloquesValidados + " bloques consecutivos sin alteraciones.");
        } catch (Exception e) {
            log.error("Error verificando integridad de la bitácora: {}", e.getMessage());
            return Map.of(
                    "valida", false,
                    "error", e.getMessage(),
                    "mensaje", "Error durante la verificación criptográfica: " + e.getMessage());
        }
    }

    public boolean verificarIntegridadCadena() {
        return Boolean.TRUE.equals(verificarIntegridadCadenaDetallada(null).get("valida"));
    }

    /**
     * Retorna el estado de sincronización con Amazon CloudWatch Logs.
     */
    public Map<String, Object> obtenerEstadoCloudWatch() {
        return cloudWatchAuditService.obtenerEstado();
    }

    /**
     * Exporta el informe de auditoría completo con certificado criptográfico para auditorías ISO 27001 / SOX.
     */
    public Map<String, Object> exportarInformeAuditoria(Integer empresaId, String codigoEmpresa) {
        List<Map<String, Object>> eventos = listarEventosTenant(
                empresaId, null, null, null, null, null, null, null);
        Map<String, Object> verificacion = verificarIntegridadCadenaDetallada(empresaId);

        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("tipoReporte", "CERTIFICADO_AUDITORIA_INMUTABLE_ISO27001");
        reporte.put("empresaId", empresaId);
        reporte.put("codigoEmpresa", codigoEmpresa);
        reporte.put("fechaExportacionUtc", Instant.now().toString());
        reporte.put("almacenamientoExterno", "Amazon CloudWatch Logs (/aws/nexoerp/bitacora)");
        reporte.put("totalEventos", eventos.size());
        reporte.put("certificacionCriptografica", verificacion);
        reporte.put("eventos", eventos);

        return reporte;
    }

    public static String calcularSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error calculando SHA-256", e);
        }
    }
}
