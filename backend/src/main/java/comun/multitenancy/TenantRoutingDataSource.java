package comun.multitenancy;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final TenantDataSourceProvider provider;
    private final DataSource bootstrapDataSource;

    public TenantRoutingDataSource(TenantDataSourceProvider provider, DataSource bootstrapDataSource) {
        this.provider = provider;
        this.bootstrapDataSource = bootstrapDataSource;
        setTargetDataSources(Map.of());
        setDefaultTargetDataSource(bootstrapDataSource);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.current().map(TenantDescriptor::codigoEmpresa).orElse(null);
    }

    @Override
    protected DataSource determineTargetDataSource() {
        return TenantContext.current()
                .<DataSource>map(provider::get)
                .orElse(bootstrapDataSource);
    }
}
