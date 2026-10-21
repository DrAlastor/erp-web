package comun.saas;

import java.time.LocalDateTime;
import java.util.List;

public record TenantEntitlementsResponse(
        String codigoEmpresa,
        String planCodigo,
        String planNombre,
        String estadoSuscripcion,
        LocalDateTime fechaFin,
        int limiteUsuarios,
        int limiteSucursales,
        int limiteAlmacenes,
        int limiteProductos,
        List<EnabledFunction> funciones) {

    public record EnabledFunction(String codigo, String nombre, String modulo, String rutaFrontend) {
    }
}
