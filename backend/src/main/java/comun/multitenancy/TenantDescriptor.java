package comun.multitenancy;

public record TenantDescriptor(Long empresaId, String codigoEmpresa) {

    public TenantDescriptor {
        if (empresaId == null) {
            throw new IllegalArgumentException("El tenant necesita una empresa");
        }
        if (codigoEmpresa == null || codigoEmpresa.isBlank()) {
            throw new IllegalArgumentException("El tenant necesita un código de empresa");
        }
        codigoEmpresa = codigoEmpresa.trim().toUpperCase();
    }
}
