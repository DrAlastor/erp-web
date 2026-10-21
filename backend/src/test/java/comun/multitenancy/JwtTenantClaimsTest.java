package comun.multitenancy;

import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTenantClaimsTest {

    private final JwtService jwtService = new JwtService(
            "test_only_secret_at_least_32_bytes_long",
            900_000);

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void includesAndReadsTenantClaims() {
        TenantDescriptor tenant = new TenantDescriptor(15L, "ASUS-7K4P2M");
        String token = TenantContext.execute(
                tenant,
                () -> jwtService.generateAccessToken("admin@asus.com", List.of("ROLE_ADMIN")));

        assertEquals(tenant, jwtService.extractTenant(token));
    }
}
