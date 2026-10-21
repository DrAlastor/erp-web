package comun.multitenancy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantDataSourceProvider {

    private final JdbcTemplate masterJdbcTemplate;
    private final String username;
    private final String password;
    private final int maximumPoolSize;
    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    private final String defaultRdsHost;

    public TenantDataSourceProvider(
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
            @Value("${tenant.datasource.username}") String username,
            @Value("${tenant.datasource.password}") String password,
            @Value("${tenant.datasource.maximum-pool-size:5}") int maximumPoolSize,
            @Value("${provisioning.rds-host:erp-database.c49qmms6qw5e.us-east-1.rds.amazonaws.com}") String defaultRdsHost) {
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.username = username;
        this.password = password;
        this.maximumPoolSize = maximumPoolSize;
        this.defaultRdsHost = defaultRdsHost;
    }

    public DataSource get(TenantDescriptor tenant) {
        TenantDatabaseConfig config = loadConfig(tenant);
        String poolKey = "%s|%s|%s|%d".formatted(
                config.codigoEmpresa(), config.databaseName(), config.host(), config.port());
        return pools.computeIfAbsent(poolKey, ignored -> createDataSource(config));
    }

    private TenantDatabaseConfig loadConfig(TenantDescriptor tenant) {
        List<TenantDatabaseConfig> configs = masterJdbcTemplate.query(
                """
                SELECT e.id, e.codigo_empresa, td.nombre_database, td.host, td.port
                FROM empresa e
                JOIN tenant_database td ON td.empresa_id = e.id
                WHERE e.id = ?
                  AND UPPER(e.codigo_empresa) = ?
                  AND UPPER(e.estado) = 'ACTIVA'
                  AND UPPER(td.estado) IN ('ACTIVA','READY')
                  AND EXISTS (
                      SELECT 1 FROM suscripcion s
                      WHERE s.empresa_id = e.id
                        AND UPPER(s.estado) = 'ACTIVA'
                        AND (s.fecha_fin IS NULL OR s.fecha_fin > CURRENT_TIMESTAMP)
                  )
                """,
                (resultSet, rowNumber) -> new TenantDatabaseConfig(
                        resultSet.getLong("id"),
                        resultSet.getString("codigo_empresa"),
                        resultSet.getString("nombre_database"),
                        resultSet.getString("host"),
                        resultSet.getInt("port")),
                tenant.empresaId(),
                tenant.codigoEmpresa().toUpperCase(Locale.ROOT));

        if (configs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La base de datos del tenant no está disponible");
        }
        return configs.getFirst();
    }

    private HikariDataSource createDataSource(TenantDatabaseConfig config) {
        String effectiveHost = config.host();
        if (effectiveHost == null || effectiveHost.isBlank() || "erp-database".equalsIgnoreCase(effectiveHost)) {
            effectiveHost = defaultRdsHost;
        }
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:postgresql://%s:%d/%s?sslmode=require".formatted(
                effectiveHost, config.port(), config.databaseName()));
        hikari.setUsername(username);
        hikari.setPassword(password);
        hikari.setDriverClassName("org.postgresql.Driver");
        hikari.setPoolName("tenant-" + config.codigoEmpresa());
        hikari.setMaximumPoolSize(maximumPoolSize);
        hikari.setMinimumIdle(0);
        hikari.setConnectionTimeout(20_000);
        return new HikariDataSource(hikari);
    }

    @PreDestroy
    public void close() {
        pools.values().forEach(HikariDataSource::close);
        pools.clear();
    }
}
