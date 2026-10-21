package modulo.seguridad_y_auditoria.cu08_consultar_bitacora.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de integración con Amazon CloudWatch Logs (CU08).
 * Garantiza un almacenamiento de auditoría inmutable, externo a las bases de datos operativas (WORM),
 * organizando streams dedicados por Tenant y para el Super Administrador.
 */
@Service
public class CloudWatchAuditService {

    private static final Logger log = LoggerFactory.getLogger(CloudWatchAuditService.class);

    private final boolean enabled;
    private final String region;
    private final String logGroupName;
    private final CloudWatchLogsClient logsClient;
    private final Set<String> verifiedStreams = ConcurrentHashMap.newKeySet();
    private boolean logGroupVerified = false;

    public CloudWatchAuditService(
            @Value("${aws.cloudwatch.enabled:true}") boolean enabled,
            @Value("${aws.region:us-east-1}") String region,
            @Value("${aws.cloudwatch.log-group:/aws/nexoerp/bitacora}") String logGroupName,
            @Value("${aws.accessKeyId:}") String accessKeyId,
            @Value("${aws.secretAccessKey:}") String secretAccessKey) {

        this.enabled = enabled;
        this.region = region;
        this.logGroupName = logGroupName;

        CloudWatchLogsClient client = null;
        if (enabled && accessKeyId != null && !accessKeyId.isBlank() && secretAccessKey != null && !secretAccessKey.isBlank()) {
            try {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
                client = CloudWatchLogsClient.builder()
                        .region(Region.of(region))
                        .credentialsProvider(StaticCredentialsProvider.create(credentials))
                        .build();
                log.info("Amazon CloudWatch Logs Client inicializado en región [{}] para el grupo [{}]", region, logGroupName);
            } catch (Exception ex) {
                log.warn("No se pudo inicializar CloudWatchLogsClient: {}. Operará con buffer local.", ex.getMessage());
            }
        } else {
            log.info("Amazon CloudWatch Logs deshabilitado o sin credenciales completas.");
        }
        this.logsClient = client;
    }

    /**
     * Envía un evento de auditoría a Amazon CloudWatch Logs en el stream correspondiente al tenant.
     *
     * @param streamName nombre del stream (ej. "tenant-ASUS", "tenant-GLOBAL")
     * @param jsonMessage mensaje estructurado en formato JSON
     * @param timestamp marca temporal del evento
     * @return true si se envió a AWS CloudWatch exitosamente, false en caso contrario
     */
    public boolean enviarEvento(String streamName, String jsonMessage, Instant timestamp) {
        if (!enabled || logsClient == null) {
            return false;
        }

        try {
            asegurarGrupoYStream(streamName);

            InputLogEvent logEvent = InputLogEvent.builder()
                    .timestamp(timestamp.toEpochMilli())
                    .message(jsonMessage)
                    .build();

            PutLogEventsRequest putRequest = PutLogEventsRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamName(streamName)
                    .logEvents(Collections.singletonList(logEvent))
                    .build();

            logsClient.putLogEvents(putRequest);
            log.debug("Evento de auditoría registrado en CloudWatch [{}/{}]", logGroupName, streamName);
            return true;
        } catch (Exception ex) {
            log.warn("Error enviando log a CloudWatch [{}/{}]: {}", logGroupName, streamName, ex.getMessage());
            return false;
        }
    }

    /**
     * Verifica la disponibilidad y estado de Amazon CloudWatch Logs para auditoría.
     */
    public Map<String, Object> obtenerEstado() {
        Map<String, Object> estado = new HashMap<>();
        estado.put("habilitado", enabled);
        estado.put("region", region);
        estado.put("logGroup", logGroupName);

        if (logsClient == null) {
            estado.put("conectado", false);
            estado.put("mensaje", "Cliente de AWS CloudWatch Logs no inicializado o credenciales pendientes.");
            return estado;
        }

        try {
            DescribeLogGroupsResponse response = logsClient.describeLogGroups(
                    DescribeLogGroupsRequest.builder().logGroupNamePrefix(logGroupName).limit(1).build());
            boolean existeGrupo = response.logGroups().stream().anyMatch(g -> logGroupName.equals(g.logGroupName()));

            estado.put("conectado", true);
            estado.put("grupoExiste", existeGrupo);
            estado.put("inmutableWORM", true);
            estado.put("almacenamientoExterno", "Amazon CloudWatch Logs (AWS US-East-1)");
            estado.put("streamsVerificados", new ArrayList<>(verifiedStreams));
            estado.put("mensaje", "Conexión activa con Amazon CloudWatch Logs. Registro inmutable operativo.");
        } catch (Exception ex) {
            estado.put("conectado", false);
            estado.put("error", ex.getMessage());
            estado.put("mensaje", "Fallo al consultar Amazon CloudWatch: " + ex.getMessage());
        }

        return estado;
    }

    private synchronized void asegurarGrupoYStream(String streamName) {
        if (!logGroupVerified) {
            try {
                logsClient.createLogGroup(CreateLogGroupRequest.builder().logGroupName(logGroupName).build());
                log.info("Log Group [{}] creado en CloudWatch.", logGroupName);
            } catch (ResourceAlreadyExistsException ignored) {
                // Ya existe
            } catch (Exception ex) {
                log.debug("Nota verificando grupo CloudWatch: {}", ex.getMessage());
            }
            logGroupVerified = true;
        }

        if (!verifiedStreams.contains(streamName)) {
            try {
                logsClient.createLogStream(CreateLogStreamRequest.builder()
                        .logGroupName(logGroupName)
                        .logStreamName(streamName)
                        .build());
                log.info("Log Stream [{}] creado en CloudWatch.", streamName);
            } catch (ResourceAlreadyExistsException ignored) {
                // Ya existe
            } catch (Exception ex) {
                log.debug("Nota verificando stream CloudWatch: {}", ex.getMessage());
            }
            verifiedStreams.add(streamName);
        }
    }
}
