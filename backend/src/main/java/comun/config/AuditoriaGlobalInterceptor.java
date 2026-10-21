package comun.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

/**
 * Interceptor global de auditoría (CU08).
 * Garantiza que cualquier operación de mutación (POST, PUT, DELETE, PATCH)
 * realizada por cualquier usuario en cualquier módulo del ERP quede
 * registrada de manera automática, inmutable y replicada en Amazon CloudWatch Logs.
 */
@Component
public class AuditoriaGlobalInterceptor implements HandlerInterceptor {

    public static final String ATTR_AUDITORIA_REGISTRADA = "AUDITORIA_REGISTRADA";

    private final BitacoraAuditoriaService bitacoraAuditoriaService;

    public AuditoriaGlobalInterceptor(BitacoraAuditoriaService bitacoraAuditoriaService) {
        this.bitacoraAuditoriaService = bitacoraAuditoriaService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // 1. Filtrar sólo métodos de mutación (CRUD) y excluir consultas repetitivas de bitácora/salud
        if (!isMetodoAuditado(method)) {
            return;
        }

        if (uri.startsWith("/api/bitacora") || uri.startsWith("/api/superadmin/bitacora") || uri.startsWith("/api/health")) {
            return;
        }

        // 2. Si el servicio de negocio ya registró un evento enriquecido (por ej. detalle de venta o stock con SKU), evitar duplicar
        if (Boolean.TRUE.equals(request.getAttribute(ATTR_AUDITORIA_REGISTRADA))) {
            return;
        }

        // 3. Inferir módulo del sistema a partir del endpoint
        String modulo = resolverModulo(uri);

        // 4. Inferir tipo de operación
        String tipoOperacion = resolverOperacion(method, modulo, uri);

        // 5. Determinar resultado y severidad
        int status = response.getStatus();
        String resultado = (status < 400 && ex == null) ? "EXITO" : "FALLO";
        String severidad = resolverSeveridad(method, status);

        String detalle = String.format("Operación %s en '%s' finalizada con HTTP %d.", method, uri, status);
        String metadatos = String.format("{\"uri\":\"%s\",\"method\":\"%s\",\"status\":%d}", uri, method, status);

        try {
            bitacoraAuditoriaService.registrarEventoActual(
                    tipoOperacion,
                    modulo,
                    detalle,
                    resultado,
                    severidad,
                    metadatos
            );
        } catch (Exception ignored) {
            // Evitar interrumpir la respuesta del cliente si la bitácora presentara algún fallo transitorio
        }
    }

    private boolean isMetodoAuditado(String method) {
        if (method == null) return false;
        String m = method.toUpperCase(Locale.ROOT);
        return m.equals("POST") || m.equals("PUT") || m.equals("DELETE") || m.equals("PATCH");
    }

    private String resolverModulo(String uri) {
        if (uri.contains("/auth") || uri.contains("/seguridad")) return "SEGURIDAD";
        if (uri.contains("/usuarios")) return "USUARIOS";
        if (uri.contains("/roles") || uri.contains("/permisos")) return "ROLES_PERMISOS";
        if (uri.contains("/empleados")) return "EMPLEADOS";
        if (uri.contains("/backups")) return "BACKUPS";
        if (uri.contains("/articulos")) return "CATALOGO";
        if (uri.contains("/categorias")) return "CATEGORIAS";
        if (uri.contains("/unidades-medida")) return "UNIDADES_MEDIDA";
        if (uri.contains("/almacenes")) return "ALMACENES";
        if (uri.contains("/movimientos") || uri.contains("/inventario")) return "INVENTARIO";
        if (uri.contains("/kardex")) return "KARDEX";
        if (uri.contains("/clientes") || uri.contains("/perfil-cliente")) return "CLIENTES";
        if (uri.contains("/cotizaciones")) return "COTIZACIONES";
        if (uri.contains("/pedidos")) return "PEDIDOS";
        if (uri.contains("/validacion-comercial")) return "VALIDACION_COMERCIAL";
        if (uri.contains("/precios") || uri.contains("/descuentos") || uri.contains("/listas-precio")) return "PRECIOS_DESCUENTOS";
        if (uri.contains("/ventas") || uri.contains("/facturacion") || uri.contains("/dosificacion")) return "VENTAS";
        if (uri.contains("/devoluciones") || uri.contains("/notas-credito")) return "DEVOLUCIONES";
        if (uri.contains("/despachos")) return "DESPACHOS";
        if (uri.contains("/pagos") || uri.contains("/stripe")) return "PAGOS";
        if (uri.contains("/notificaciones")) return "NOTIFICACIONES";
        if (uri.contains("/superadmin")) return "SUPERADMIN";
        return "SISTEMA";
    }

    private String resolverOperacion(String method, String modulo, String uri) {
        String m = method.toUpperCase(Locale.ROOT);
        return switch (m) {
            case "POST" -> "CREAR_" + modulo;
            case "PUT" -> "ACTUALIZAR_" + modulo;
            case "PATCH" -> "MODIFICAR_" + modulo;
            case "DELETE" -> "ELIMINAR_" + modulo;
            default -> "ACCION_" + modulo;
        };
    }

    private String resolverSeveridad(String method, int status) {
        if ("DELETE".equalsIgnoreCase(method) || status >= 500) {
            return "ALTA";
        }
        if (status >= 400) {
            return "MEDIA";
        }
        return "BAJA";
    }
}
