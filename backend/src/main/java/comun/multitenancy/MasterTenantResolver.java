package comun.multitenancy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;

@Component
public class MasterTenantResolver {

    private final JdbcTemplate masterJdbcTemplate;
    private final TenantDataSourceProvider tenantDataSourceProvider;

    public MasterTenantResolver(
            @Qualifier("masterJdbcTemplate") JdbcTemplate masterJdbcTemplate,
            @Lazy TenantDataSourceProvider tenantDataSourceProvider) {
        this.masterJdbcTemplate = masterJdbcTemplate;
        this.tenantDataSourceProvider = tenantDataSourceProvider;
    }

    public TenantDescriptor resolve(String codigoEmpresa) {
        String normalized = normalize(codigoEmpresa);
        List<TenantDescriptor> tenants = masterJdbcTemplate.query(
                """
                SELECT e.id, e.codigo_empresa
                FROM empresa e
                JOIN tenant_database td ON td.empresa_id = e.id
                WHERE UPPER(e.codigo_empresa) = ?
                  AND UPPER(e.estado) = 'ACTIVA'
                  AND UPPER(td.estado) IN ('ACTIVA','READY')
                  AND EXISTS (
                      SELECT 1 FROM suscripcion s
                      WHERE s.empresa_id = e.id
                        AND UPPER(s.estado) = 'ACTIVA'
                        AND (s.fecha_fin IS NULL OR s.fecha_fin > CURRENT_TIMESTAMP)
                  )
                """,
                (resultSet, rowNumber) -> new TenantDescriptor(
                        resultSet.getLong("id"),
                        resultSet.getString("codigo_empresa")),
                normalized);

        if (tenants.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La empresa no existe, está suspendida o todavía no fue aprovisionada");
        }
        return tenants.getFirst();
    }

    public TenantDescriptor resolveUserTenant(String usernameOrEmail, String optionalCompanyCode) {
        if (optionalCompanyCode != null && !optionalCompanyCode.isBlank()) {
            return resolve(optionalCompanyCode);
        }
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario o correo electrónico es obligatorio");
        }

        String input = usernameOrEmail.trim().toLowerCase(Locale.ROOT);

        // 1. Verificar si coincide con el correo registrado de la empresa en ERP_Master
        List<TenantDescriptor> byCompanyEmail = masterJdbcTemplate.query(
                """
                SELECT e.id, e.codigo_empresa
                FROM empresa e
                JOIN tenant_database td ON td.empresa_id = e.id
                WHERE LOWER(e.email) = ?
                  AND UPPER(e.estado) = 'ACTIVA'
                  AND UPPER(td.estado) IN ('ACTIVA','READY')
                """,
                (rs, rowNum) -> new TenantDescriptor(rs.getLong("id"), rs.getString("codigo_empresa")),
                input);

        if (!byCompanyEmail.isEmpty()) {
            return byCompanyEmail.getFirst();
        }

        // 2. Consultar tenants activos y verificar cuál tiene a este usuario en su base de datos
        List<TenantDescriptor> activeTenants = masterJdbcTemplate.query(
                """
                SELECT e.id, e.codigo_empresa
                FROM empresa e
                JOIN tenant_database td ON td.empresa_id = e.id
                WHERE UPPER(e.estado) = 'ACTIVA'
                  AND UPPER(td.estado) IN ('ACTIVA','READY')
                ORDER BY e.id ASC
                """,
                (rs, rowNum) -> new TenantDescriptor(rs.getLong("id"), rs.getString("codigo_empresa")));

        for (TenantDescriptor tenant : activeTenants) {
            try {
                DataSource ds = tenantDataSourceProvider.get(tenant);
                JdbcTemplate tenantJdbc = new JdbcTemplate(ds);
                Integer count = tenantJdbc.queryForObject(
                        "SELECT COUNT(1) FROM usuario WHERE (LOWER(email) = ? OR LOWER(email) LIKE ?) AND activo = TRUE",
                        Integer.class,
                        input,
                        input + "@%");
                if (count != null && count > 0) {
                    return tenant;
                }
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(MasterTenantResolver.class)
                        .warn("No se pudo verificar tenant {}: {}", tenant.codigoEmpresa(), ex.getMessage());
            }
        }

        // 3. Fallback: Si hay empresas activas (por ejemplo ASUS-7K4P2M), retornar la primera para intentar autenticar
        if (!activeTenants.isEmpty()) {
            return activeTenants.getFirst();
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay empresas activas disponibles");
    }

    public TenantDescriptor requireActive(TenantDescriptor claimedTenant) {
        TenantDescriptor activeTenant = resolve(claimedTenant.codigoEmpresa());
        if (!activeTenant.empresaId().equals(claimedTenant.empresaId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El tenant del token no es válido");
        }
        return activeTenant;
    }

    private String normalize(String codigoEmpresa) {
        if (codigoEmpresa == null || codigoEmpresa.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de empresa es obligatorio");
        }
        return codigoEmpresa.trim().toUpperCase(Locale.ROOT);
    }
}
