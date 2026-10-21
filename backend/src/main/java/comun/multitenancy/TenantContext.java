package comun.multitenancy;

import java.util.Optional;
import java.util.function.Supplier;

public final class TenantContext {

    private static final ThreadLocal<TenantDescriptor> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static Optional<TenantDescriptor> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static TenantDescriptor require() {
        return current().orElseThrow(() -> new IllegalStateException("No existe un tenant activo"));
    }

    public static void set(TenantDescriptor tenant) {
        CURRENT.set(tenant);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static <T> T execute(TenantDescriptor tenant, Supplier<T> operation) {
        TenantDescriptor previous = CURRENT.get();
        CURRENT.set(tenant);
        try {
            return operation.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
