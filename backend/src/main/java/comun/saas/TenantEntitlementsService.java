package comun.saas;

import comun.multitenancy.TenantContext;
import comun.multitenancy.TenantDescriptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TenantEntitlementsService {

    private final JdbcTemplate master;

    public TenantEntitlementsService(@Qualifier("masterJdbcTemplate") JdbcTemplate master) {
        this.master = master;
    }

    public TenantEntitlementsResponse current() {
        TenantDescriptor tenant = TenantContext.require();
        TenantEntitlementsResponse base = master.query("""
                SELECT e.codigo_empresa,p.codigo,p.nombre,s.estado,s.fecha_fin,
                       p.limite_usuarios,p.limite_sucursales,p.limite_almacenes,p.limite_productos
                FROM empresa e
                JOIN suscripcion s ON s.empresa_id=e.id
                JOIN planes_suscripcion p ON p.id=s.plan_id
                WHERE e.id=? AND UPPER(e.estado)='ACTIVA' AND UPPER(s.estado)='ACTIVA'
                  AND s.fecha_inicio<=CURRENT_TIMESTAMP AND s.fecha_fin>CURRENT_TIMESTAMP
                ORDER BY s.fecha_inicio DESC LIMIT 1
                """, resultSet -> {
            if (!resultSet.next()) {
                throw new IllegalStateException("El tenant no tiene una suscripción activa");
            }
            return new TenantEntitlementsResponse(
                    resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
                    resultSet.getString(4), resultSet.getTimestamp(5).toLocalDateTime(),
                    resultSet.getInt(6), resultSet.getInt(7), resultSet.getInt(8), resultSet.getInt(9),
                    List.of());
        }, tenant.empresaId());

        List<TenantEntitlementsResponse.EnabledFunction> functions = master.query("""
                SELECT f.codigo,f.nombre,m.codigo,f.ruta_frontend
                FROM empresa e
                JOIN suscripcion s ON s.empresa_id=e.id
                JOIN plan_funciones pf ON pf.plan_id=s.plan_id AND pf.habilitada=TRUE
                JOIN funciones_saas f ON f.id=pf.funcion_id AND f.activo=TRUE
                JOIN modulos_saas m ON m.id=f.modulo_id AND m.activo=TRUE
                WHERE e.id=? AND UPPER(s.estado)='ACTIVA'
                  AND s.fecha_inicio<=CURRENT_TIMESTAMP AND s.fecha_fin>CURRENT_TIMESTAMP
                ORDER BY m.orden_visual,f.id
                """, (resultSet, rowNumber) -> new TenantEntitlementsResponse.EnabledFunction(
                resultSet.getString(1), resultSet.getString(2),
                resultSet.getString(3), resultSet.getString(4)), tenant.empresaId());

        return new TenantEntitlementsResponse(
                base.codigoEmpresa(), base.planCodigo(), base.planNombre(), base.estadoSuscripcion(), base.fechaFin(),
                base.limiteUsuarios(), base.limiteSucursales(), base.limiteAlmacenes(), base.limiteProductos(), functions);
    }

    public int limit(String resource) {
        TenantEntitlementsResponse entitlements = current();
        return switch (resource.toUpperCase()) {
            case "USUARIOS" -> entitlements.limiteUsuarios();
            case "SUCURSALES" -> entitlements.limiteSucursales();
            case "ALMACENES" -> entitlements.limiteAlmacenes();
            case "PRODUCTOS" -> entitlements.limiteProductos();
            default -> throw new IllegalArgumentException("Recurso SaaS desconocido");
        };
    }

    public void requireCapacity(String resource, long currentCount) {
        int configuredLimit = limit(resource);
        if (configuredLimit >= 0 && currentCount >= configuredLimit) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El plan alcanzó el límite de " + resource.toLowerCase());
        }
    }
}
