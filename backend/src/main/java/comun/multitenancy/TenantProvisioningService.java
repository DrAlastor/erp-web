package comun.multitenancy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TenantProvisioningService {

    private static final Pattern FIRST_DATA_INSERT = Pattern.compile("(?im)^\\s*INSERT\\s+INTO\\s+");

    private final JdbcTemplate provisioningJdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String baseUrl;
    private final String provisioningUsername;
    private final String provisioningPassword;
    private final String runtimeUsername;
    private final Resource tenantSchema;
    private final Resource tenantSeeds;

    public TenantProvisioningService(
            @Qualifier("provisioningJdbcTemplate") JdbcTemplate provisioningJdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${provisioning.datasource.base-url}") String baseUrl,
            @Value("${provisioning.datasource.username}") String provisioningUsername,
            @Value("${provisioning.datasource.password}") String provisioningPassword,
            @Value("${tenant.datasource.username}") String runtimeUsername,
            @Value("${provisioning.tenant-schema:classpath:db/tenant/tenant_schema.sql}") Resource tenantSchema,
            @Value("${provisioning.tenant-seeds:classpath:db/tenant/tenant_seed_maestros.sql}") Resource tenantSeeds) {
        this.provisioningJdbcTemplate = provisioningJdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.provisioningUsername = provisioningUsername;
        this.provisioningPassword = provisioningPassword;
        this.runtimeUsername = requireRole(runtimeUsername);
        this.tenantSchema = tenantSchema;
        this.tenantSeeds = tenantSeeds;
    }

    public void provision(
            String databaseName,
            String adminName,
            String adminEmail,
            String adminPassword,
            String companyName,
            String city,
            String country) {
        String safeDatabase = requireIdentifier(databaseName);
        if (databaseExists(safeDatabase)) {
            throw new IllegalStateException("La base de datos del tenant ya existe");
        }

        provisioningJdbcTemplate.execute("CREATE DATABASE " + quote(safeDatabase)
                + " ENCODING 'UTF8' TEMPLATE template0");

        try (HikariDataSource tenantAdmin = tenantAdminDataSource(safeDatabase)) {
            JdbcTemplate jdbc = new JdbcTemplate(tenantAdmin);
            createSchema(tenantAdmin);
            prepareRuntimeSchema(jdbc, safeDatabase);
            createInitialAdmin(jdbc, adminName, adminEmail, adminPassword, companyName, city, country);
        } catch (RuntimeException failure) {
            dropDatabase(safeDatabase);
            throw failure;
        }
    }

    public void dropDatabase(String databaseName) {
        String safeDatabase = requireIdentifier(databaseName);
        provisioningJdbcTemplate.execute("DROP DATABASE IF EXISTS " + quote(safeDatabase) + " WITH (FORCE)");
    }

    private boolean databaseExists(String databaseName) {
        Boolean exists = provisioningJdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_database WHERE datname = ?)",
                Boolean.class,
                databaseName);
        return Boolean.TRUE.equals(exists);
    }

    private void prepareRuntimeSchema(JdbcTemplate jdbc, String databaseName) {
        jdbc.execute("GRANT CONNECT ON DATABASE " + quote(databaseName) + " TO " + quote(runtimeUsername));
        jdbc.execute("GRANT USAGE ON SCHEMA public TO " + quote(runtimeUsername));
        jdbc.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO "
                + quote(runtimeUsername));
        jdbc.execute("GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO "
                + quote(runtimeUsername));
        jdbc.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "
                + quote(runtimeUsername));
        jdbc.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO "
                + quote(runtimeUsername));
    }

    private void createSchema(HikariDataSource tenantAdmin) {
        try {
            String schemaSql = tenantSchema.getContentAsString(StandardCharsets.UTF_8);
            try (Connection connection = tenantAdmin.getConnection()) {
                ScriptUtils.executeSqlScript(
                        connection,
                        new ByteArrayResource(schemaSql.getBytes(StandardCharsets.UTF_8)));

                if (tenantSeeds != null && tenantSeeds.exists()) {
                    String seedSql = tenantSeeds.getContentAsString(StandardCharsets.UTF_8);
                    ScriptUtils.executeSqlScript(
                            connection,
                            new ByteArrayResource(seedSql.getBytes(StandardCharsets.UTF_8)));
                }
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("No se pudo crear el esquema ERP del tenant", exception);
        }
    }

    private void createInitialAdmin(
            JdbcTemplate jdbc,
            String adminName,
            String adminEmail,
            String adminPassword,
            String companyName,
            String city,
            String country) {
        Integer roleId = jdbc.query(
                "SELECT id FROM rol WHERE UPPER(nombre) IN ('ADMINISTRADOR','ADMIN') ORDER BY id LIMIT 1",
                resultSet -> resultSet.next() ? resultSet.getInt(1) : null);
        if (roleId == null) {
            roleId = jdbc.queryForObject(
                    "INSERT INTO rol(nombre, permiso, activo) VALUES ('ADMINISTRADOR','ALL',TRUE) RETURNING id",
                    Integer.class);
        }

        if (jdbc.queryForObject("SELECT COUNT(*) FROM configuracion_empresa", Integer.class) == 0) {
            jdbc.update("""
                    INSERT INTO configuracion_empresa
                    (tema,color_primario,color_secundario,logo_personalizado_url,encabezado_factura,pie_factura,fecha_modificacion)
                    VALUES (NULL,NULL,NULL,NULL,?,?,?)
                    """, companyName + " - " + city + ", " + country, "Gracias por su compra.", LocalDateTime.now());
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM ciudad", Integer.class) == 0) {
            jdbc.update("INSERT INTO ciudad(nombre,pais) VALUES (?,?)", city, country);
        }

        jdbc.update("""
                INSERT INTO usuario
                (rol_id,empleado_id,email,password,nivel_usuario,fecha_creacion,activo)
                VALUES (?,NULL,?,?,?,?,TRUE)
                """,
                roleId,
                adminEmail.trim().toLowerCase(Locale.ROOT),
                passwordEncoder.encode(adminPassword),
                "ADMINISTRADOR",
                LocalDateTime.now());
    }

    private HikariDataSource tenantAdminDataSource(String databaseName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(baseUrl + "/" + databaseName + "?sslmode=require");
        config.setUsername(provisioningUsername);
        config.setPassword(provisioningPassword);
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);
        config.setPoolName("provision-" + databaseName);
        return new HikariDataSource(config);
    }

    private String requireIdentifier(String value) {
        if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_]{0,62}")) {
            throw new IllegalArgumentException("Identificador PostgreSQL inválido");
        }
        return value;
    }

    private String requireRole(String value) {
        return requireIdentifier(value);
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
