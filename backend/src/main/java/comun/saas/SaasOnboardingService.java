package comun.saas;

import comun.catalogos.dto.MonedaCatalogoResponse;
import comun.catalogos.dto.PaisCatalogoResponse;
import comun.catalogos.service.CatalogosGeograficosService;
import comun.multitenancy.TenantProvisioningService;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.OnboardingEmpresaRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.OnboardingEmpresaResponse;
import modulo.ventas_y_facturacion.pagos.dto.StripeCheckoutRequestDTO;
import modulo.ventas_y_facturacion.pagos.dto.StripeCheckoutResponseDTO;
import modulo.ventas_y_facturacion.pagos.dto.StripeSesionDTO;
import modulo.ventas_y_facturacion.pagos.service.StripePaymentService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class SaasOnboardingService {

    private final JdbcTemplate master;
    private final TenantProvisioningService provisioning;
    private final CatalogosGeograficosService catalogos;
    private final StripePaymentService stripe;
    private final String rdsInstance;
    private final String rdsHost;
    private final String tenantSecretArn;
    private final String schemaVersion;
    private final String frontendUrl;

    public SaasOnboardingService(
            @Qualifier("masterJdbcTemplate") JdbcTemplate master,
            TenantProvisioningService provisioning,
            CatalogosGeograficosService catalogos,
            StripePaymentService stripe,
            @Value("${provisioning.rds-instance}") String rdsInstance,
            @Value("${provisioning.rds-host}") String rdsHost,
            @Value("${provisioning.tenant-secret-arn:}") String tenantSecretArn,
            @Value("${provisioning.schema-version}") String schemaVersion,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.master = master;
        this.provisioning = provisioning;
        this.catalogos = catalogos;
        this.stripe = stripe;
        this.rdsInstance = rdsInstance;
        this.rdsHost = rdsHost;
        this.tenantSecretArn = tenantSecretArn;
        this.schemaVersion = schemaVersion;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public OnboardingEmpresaResponse register(OnboardingEmpresaRequest request) {
        String planCode = normalizePlan(request.planCodigo());
        Plan plan = findPlan(planCode);
        PaisCatalogoResponse country = catalogos.buscarPais(request.paisCodigo());
        MonedaCatalogoResponse currency = findCurrency(request.monedaPrincipal(), country);
        String email = request.adminEmail().trim().toLowerCase(Locale.ROOT);

        ensureCompanyIsUnique(request.nit(), email);
        int countryId = upsertCountry(country);
        int currencyId = upsertCurrency(currency);
        LocalDateTime now = LocalDateTime.now();
        String companyCode = generateCompanyCode(request.razonSocial());

        Integer companyId = master.queryForObject("""
                INSERT INTO empresa
                (pais_id,moneda_principal_id,moneda_secundaria_id,codigo_empresa,razon_social,nombre_comercial,
                 nit,direccion,telefono,email,ciudad_nombre,estado,fecha_creacion,fecha_modificacion,fecha_suspension)
                VALUES (?,?,NULL,?,?,?,?,?,?,?,?,?,?,?,NULL) RETURNING id
                """, Integer.class,
                countryId, currencyId, companyCode, request.razonSocial().trim(),
                clean(request.nombreComercial(), request.razonSocial()), request.nit().trim(),
                clean(request.direccion(), null), clean(request.telefonoEmpresa(), null), email,
                clean(request.ciudad(), country.nombre()), "PROVISIONANDO", now, now);

        String databaseName = "ERP_TENANT_" + companyId;
        LocalDateTime endDate = plan.trialDays() > 0
                ? now.plusDays(plan.trialDays())
                : ("ANUAL".equalsIgnoreCase(request.cicloFacturacion()) ? now.plusYears(1) : now.plusMonths(1));
        Integer subscriptionId = master.queryForObject("""
                INSERT INTO suscripcion
                (empresa_id,plan_id,ciclo_facturacion,estado,es_prueba,auto_renovacion,fecha_inicio,fecha_fin,
                 fecha_cancelacion,motivo_cancelacion,fecha_creacion,fecha_modificacion)
                VALUES (?,?,?,'PROVISIONANDO',?,?,?, ?,NULL,NULL,?,?) RETURNING id
                """, Integer.class,
                companyId, plan.id(), request.cicloFacturacion().toUpperCase(Locale.ROOT),
                !plan.requiresPayment(), plan.requiresPayment(), now, endDate, now, now);

        master.update("""
                INSERT INTO tenant_database
                (empresa_id,nombre_database,rds_instance,host,port,secret_arn,estado,version_esquema,
                 ultima_migracion,fecha_creacion,fecha_modificacion)
                VALUES (?,?,?,?,5432,?,'PROVISIONANDO',?,NULL,?,?)
                """, companyId, databaseName, rdsInstance, rdsHost,
                clean(tenantSecretArn, null), schemaVersion, now, now);

        try {
            provisioning.provision(
                    databaseName,
                    request.adminNombre(),
                    email,
                    request.adminPassword(),
                    request.razonSocial(),
                    clean(request.ciudad(), country.nombre()),
                    country.nombre());

            if (!plan.requiresPayment()) {
                activate(companyId, subscriptionId, null, BigDecimal.ZERO);
                return response(request, companyCode, databaseName, planCode, "ACTIVA", null);
            }

            BigDecimal amount = "ANUAL".equalsIgnoreCase(request.cicloFacturacion())
                    ? plan.annualPrice() : plan.monthlyPrice();
            StripeCheckoutResponseDTO checkout = createCheckout(
                    request, companyId, subscriptionId, companyCode, plan, amount);
            master.update("""
                    INSERT INTO pago
                    (suscripcion_id,metodo_pago,importe,transaccion_externa,estado,fecha_pago)
                    VALUES (?,'STRIPE',?,?,'PENDIENTE',?)
                    """, subscriptionId, amount, checkout.getSessionId(), now);
            master.update("UPDATE empresa SET estado='PENDIENTE_PAGO',fecha_modificacion=? WHERE id=?", now, companyId);
            master.update("UPDATE suscripcion SET estado='PENDIENTE_PAGO',fecha_modificacion=? WHERE id=?", now, subscriptionId);
            master.update("UPDATE tenant_database SET estado='READY',ultima_migracion=?,fecha_modificacion=? WHERE empresa_id=?",
                    now, now, companyId);
            return response(request, companyCode, databaseName, planCode, "PENDIENTE_PAGO", checkout.getCheckoutUrl());
        } catch (RuntimeException failure) {
            markFailed(companyId, subscriptionId);
            throw failure;
        }
    }

    public String confirmStripeSession(String sessionId) {
        StripeSesionDTO session = stripe.consultarSesion(sessionId);
        if (!session.pagada() || !"SAAS_SUBSCRIPTION".equals(session.metadata().get("tipo"))) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "El pago todavía no fue confirmado");
        }
        int companyId = parseMetadataId(session, "empresaId");
        int subscriptionId = parseMetadataId(session, "suscripcionId");
        BigDecimal paidAmount = BigDecimal.valueOf(session.montoTotal(), 2).setScale(2, RoundingMode.HALF_UP);
        activate(companyId, subscriptionId, session.id(), paidAmount);
        return master.queryForObject("SELECT codigo_empresa FROM empresa WHERE id=?", String.class, companyId);
    }

    private StripeCheckoutResponseDTO createCheckout(
            OnboardingEmpresaRequest request,
            int companyId,
            int subscriptionId,
            String companyCode,
            Plan plan,
            BigDecimal amount) {
        if (!stripe.estaConfigurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe no está configurado");
        }
        return stripe.crearSesionCheckout(new StripeCheckoutRequestDTO(
                amount,
                "USD",
                plan.name(),
                "Suscripción NexoERP para " + request.razonSocial(),
                request.adminEmail(),
                request.adminNombre(),
                "SAAS-" + subscriptionId,
                frontendUrl + "/registro-suscripcion?pago=exito&session_id={CHECKOUT_SESSION_ID}",
                frontendUrl + "/registro-suscripcion?pago=cancelado",
                Map.of(
                        "tipo", "SAAS_SUBSCRIPTION",
                        "empresaId", String.valueOf(companyId),
                        "suscripcionId", String.valueOf(subscriptionId),
                        "codigoEmpresa", companyCode)));
    }

    private void activate(int companyId, int subscriptionId, String sessionId, BigDecimal paidAmount) {
        LocalDateTime now = LocalDateTime.now();
        master.update("UPDATE empresa SET estado='ACTIVA',fecha_suspension=NULL,fecha_modificacion=? WHERE id=?", now, companyId);
        master.update("UPDATE tenant_database SET estado='ACTIVA',ultima_migracion=?,fecha_modificacion=? WHERE empresa_id=?",
                now, now, companyId);
        master.update("UPDATE suscripcion SET estado='ACTIVA',fecha_modificacion=? WHERE id=?", now, subscriptionId);
        if (sessionId != null) {
            int updated = master.update("""
                    UPDATE pago SET estado='APROBADO',importe=?,fecha_pago=?
                    WHERE suscripcion_id=? AND transaccion_externa=? AND estado<>'APROBADO'
                    """, paidAmount, now, subscriptionId, sessionId);
            if (updated == 0) {
                Integer existing = master.queryForObject(
                        "SELECT COUNT(*) FROM pago WHERE suscripcion_id=? AND transaccion_externa=? AND estado='APROBADO'",
                        Integer.class, subscriptionId, sessionId);
                if (existing == null || existing == 0) {
                    throw new IllegalStateException("No existe el pago pendiente de la suscripción");
                }
            }
        }
    }

    private void markFailed(int companyId, int subscriptionId) {
        LocalDateTime now = LocalDateTime.now();
        master.update("UPDATE empresa SET estado='ERROR_APROVISIONAMIENTO',fecha_modificacion=? WHERE id=?", now, companyId);
        master.update("UPDATE tenant_database SET estado='ERROR',fecha_modificacion=? WHERE empresa_id=?", now, companyId);
        master.update("UPDATE suscripcion SET estado='ERROR',fecha_modificacion=? WHERE id=?", now, subscriptionId);
    }

    private Plan findPlan(String code) {
        return master.query("""
                SELECT id,nombre,precio_mensual,precio_anual,duracion_prueba_dias,requiere_pago
                FROM planes_suscripcion WHERE UPPER(codigo)=? AND activo=TRUE
                """, resultSet -> {
            if (!resultSet.next()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El plan seleccionado no existe");
            }
            return new Plan(
                    resultSet.getInt("id"),
                    resultSet.getString("nombre"),
                    resultSet.getBigDecimal("precio_mensual"),
                    resultSet.getBigDecimal("precio_anual"),
                    resultSet.getInt("duracion_prueba_dias"),
                    resultSet.getBoolean("requiere_pago"));
        }, code);
    }

    private int upsertCountry(PaisCatalogoResponse country) {
        return master.queryForObject("""
                INSERT INTO pais(codigo_iso2,codigo_iso3,nombre,activo)
                VALUES (?,?,?,TRUE)
                ON CONFLICT (codigo_iso2) DO UPDATE SET codigo_iso3=EXCLUDED.codigo_iso3,nombre=EXCLUDED.nombre,activo=TRUE
                RETURNING id
                """, Integer.class, country.codigoIso2(), country.codigoIso3(), country.nombre());
    }

    private int upsertCurrency(MonedaCatalogoResponse currency) {
        return master.queryForObject("""
                INSERT INTO moneda(codigo,nombre,simbolo,decimales,activo)
                VALUES (?,?,?,2,TRUE)
                ON CONFLICT (codigo) DO UPDATE SET nombre=EXCLUDED.nombre,simbolo=EXCLUDED.simbolo,activo=TRUE
                RETURNING id
                """, Integer.class, currency.codigo(), currency.nombre(), currency.simbolo());
    }

    private MonedaCatalogoResponse findCurrency(String code, PaisCatalogoResponse country) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return country.monedas().stream()
                .filter(currency -> currency.codigo().equalsIgnoreCase(normalized))
                .findFirst()
                .or(() -> catalogos.listarMonedas().stream()
                        .filter(currency -> currency.codigo().equalsIgnoreCase(normalized)).findFirst())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La moneda seleccionada no existe"));
    }

    private void ensureCompanyIsUnique(String nit, String email) {
        String safeNit = nit.trim();
        String safeEmail = email.trim().toLowerCase(Locale.ROOT);

        // 1. Limpiar intentos previos que quedaron en ERROR_APROVISIONAMIENTO
        List<Integer> failedCompanyIds = master.queryForList(
                "SELECT id FROM empresa WHERE (UPPER(COALESCE(nit,''))=UPPER(?) OR LOWER(email)=LOWER(?)) AND estado='ERROR_APROVISIONAMIENTO'",
                Integer.class, safeNit, safeEmail);
        for (Integer fId : failedCompanyIds) {
            master.update("DELETE FROM pago WHERE suscripcion_id IN (SELECT id FROM suscripcion WHERE empresa_id=?)", fId);
            master.update("DELETE FROM tenant_database WHERE empresa_id=?", fId);
            master.update("DELETE FROM suscripcion WHERE empresa_id=?", fId);
            master.update("DELETE FROM empresa WHERE id=?", fId);
        }

        // 2. Validar que no exista una empresa activa o pendiente
        Integer count = master.queryForObject(
                "SELECT COUNT(*) FROM empresa WHERE (UPPER(COALESCE(nit,''))=UPPER(?) OR LOWER(email)=LOWER(?)) AND estado<>'ERROR_APROVISIONAMIENTO'",
                Integer.class, safeNit, safeEmail);
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La empresa, NIT o correo ya está registrado con una cuenta activa");
        }
    }

    private String generateCompanyCode(String companyName) {
        String prefix = companyName.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        prefix = prefix.isBlank() ? "EMP" : prefix.substring(0, Math.min(prefix.length(), 6));
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String normalizePlan(String value) {
        String code = value.trim().toUpperCase(Locale.ROOT);
        return switch (code) {
            case "PLAN-BASICO" -> "BASICO";
            case "PLAN-PRO" -> "PREMIUM";
            case "PLAN-CORP" -> "CORPORATIVO";
            default -> code;
        };
    }

    private int parseMetadataId(StripeSesionDTO session, String key) {
        try {
            return Integer.parseInt(session.metadata().get(key));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sesión de Stripe no pertenece a una suscripción válida");
        }
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private OnboardingEmpresaResponse response(
            OnboardingEmpresaRequest request,
            String companyCode,
            String databaseName,
            String planCode,
            String status,
            String checkoutUrl) {
        return new OnboardingEmpresaResponse(
                "ACTIVA".equals(status) ? "Empresa aprovisionada correctamente" : "Empresa creada; pago pendiente",
                request.razonSocial().trim(),
                request.nit().trim(),
                companyCode,
                databaseName,
                planCode,
                request.adminEmail().trim().toLowerCase(Locale.ROOT),
                "ADMINISTRADOR",
                status,
                checkoutUrl,
                null);
    }

    private record Plan(
            int id,
            String name,
            BigDecimal monthlyPrice,
            BigDecimal annualPrice,
            int trialDays,
            boolean requiresPayment) {
    }
}
