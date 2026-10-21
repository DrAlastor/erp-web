package comun.multitenancy;

public record TenantDatabaseConfig(
        Long empresaId,
        String codigoEmpresa,
        String databaseName,
        String host,
        int port) {
}
