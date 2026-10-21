package comun.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import comun.multitenancy.TenantDataSourceProvider;
import comun.multitenancy.TenantRoutingDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class MultiTenantDataSourceConfig {

    @Bean(name = "masterDataSource", destroyMethod = "close")
    public HikariDataSource masterDataSource(
            @Value("${master.datasource.url}") String url,
            @Value("${master.datasource.username}") String username,
            @Value("${master.datasource.password}") String password,
            @Value("${master.datasource.maximum-pool-size:5}") int maximumPoolSize) {
        return hikari("master", url, username, password, maximumPoolSize);
    }

    @Bean(name = "masterJdbcTemplate")
    public JdbcTemplate masterJdbcTemplate(@Qualifier("masterDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "provisioningDataSource", destroyMethod = "close")
    public HikariDataSource provisioningDataSource(
            @Value("${provisioning.datasource.url}") String url,
            @Value("${provisioning.datasource.username}") String username,
            @Value("${provisioning.datasource.password}") String password) {
        return hikari("tenant-provisioning", url, username, password, 2);
    }

    @Bean(name = "provisioningJdbcTemplate")
    public JdbcTemplate provisioningJdbcTemplate(
            @Qualifier("provisioningDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "bootstrapTenantDataSource", destroyMethod = "close")
    public HikariDataSource bootstrapTenantDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.hikari.maximum-pool-size:10}") int maximumPoolSize) {
        return hikari("tenant-bootstrap", url, username, password, maximumPoolSize);
    }

    @Bean
    @Primary
    public DataSource dataSource(
            TenantDataSourceProvider provider,
            @Qualifier("bootstrapTenantDataSource") DataSource bootstrapDataSource) {
        return new TenantRoutingDataSource(provider, bootstrapDataSource);
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    private HikariDataSource hikari(
            String poolName,
            String url,
            String username,
            String password,
            int maximumPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setPoolName(poolName);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(20_000);
        return new HikariDataSource(config);
    }
}
