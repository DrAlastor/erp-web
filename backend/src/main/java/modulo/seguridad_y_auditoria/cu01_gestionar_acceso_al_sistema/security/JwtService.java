package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.security;

import comun.multitenancy.TenantContext;
import comun.multitenancy.TenantDescriptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration-ms}") long accessExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
    }

    public String generateAccessToken(String username, Collection<String> authorities) {
        return generateAccessToken(username, authorities, List.of());
    }

    public String generateAccessToken(
            String username, Collection<String> authorities, Collection<String> roles) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(username)
                .claim("authorities", authorities)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMs)));

        TenantContext.current().ifPresent(tenant -> builder
                .claim("tenantId", tenant.empresaId())
                .claim("codigoEmpresa", tenant.codigoEmpresa()));

        return builder.signWith(key).compact();
    }

    public String generateSuperAdminToken(String email, String rolGlobal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("superAdmin", true)
                .claim("rolGlobal", rolGlobal)
                .claim("authorities", List.of("ROLE_SUPER_ADMIN", "ROLE_SUPERADMIN", "ALL"))
                .claim("roles", List.of("SUPER_ADMIN"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMs * 4)))
                .signWith(key)
                .compact();
    }

    public boolean isSuperAdmin(String token) {
        try {
            Claims claims = parseClaims(token);
            Boolean isSuper = claims.get("superAdmin", Boolean.class);
            return Boolean.TRUE.equals(isSuper);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        return parseClaims(token).get("authorities", List.class);
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    public String generateRefreshToken(String username, long expirationDays) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(username)
                .claim("tokenType", "REFRESH")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationDays * 86_400)));

        TenantContext.current().ifPresent(tenant -> builder
                .claim("tenantId", tenant.empresaId())
                .claim("codigoEmpresa", tenant.codigoEmpresa()));
        return builder.signWith(key).compact();
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        if (!"REFRESH".equals(claims.get("tokenType", String.class))) {
            throw new JwtException("El token no es un refresh token v\u00e1lido");
        }
        return claims;
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public TenantDescriptor extractTenant(String token) {
        Claims claims = parseClaims(token);
        Number tenantId = claims.get("tenantId", Number.class);
        String codigoEmpresa = claims.get("codigoEmpresa", String.class);
        if (tenantId == null || codigoEmpresa == null || codigoEmpresa.isBlank()) {
            throw new JwtException("El token no contiene un tenant válido");
        }
        return new TenantDescriptor(tenantId.longValue(), codigoEmpresa);
    }
}
