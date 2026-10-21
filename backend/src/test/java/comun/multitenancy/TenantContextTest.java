package comun.multitenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantContextTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void executesWithinTenantAndClearsContext() {
        TenantDescriptor tenant = new TenantDescriptor(10L, "asus-7k4p2m");

        String code = TenantContext.execute(tenant, () -> TenantContext.require().codigoEmpresa());

        assertEquals("ASUS-7K4P2M", code);
        assertTrue(TenantContext.current().isEmpty());
    }

    @Test
    void restoresPreviousTenantAfterNestedExecution() {
        TenantDescriptor outer = new TenantDescriptor(1L, "OUTER-1");
        TenantDescriptor inner = new TenantDescriptor(2L, "INNER-2");
        TenantContext.set(outer);

        TenantContext.execute(inner, () -> {
            assertEquals(inner, TenantContext.require());
            return null;
        });

        assertEquals(outer, TenantContext.require());
    }
}
