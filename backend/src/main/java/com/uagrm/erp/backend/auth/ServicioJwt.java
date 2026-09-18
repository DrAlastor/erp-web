package com.uagrm.erp.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Emite y valida los tokens de acceso. Provisional: la CU-01 reemplaza este paquete.
 *
 * <p>El token lleva el usuario, la empresa, el nombre y el email. <strong>No lleva
 * permisos</strong>, a propósito: si los llevara, quitarle un rol a alguien no tendría
 * efecto hasta que el token venciera.
 */
@Component
public class ServicioJwt {

    private static final Logger log = LoggerFactory.getLogger(ServicioJwt.class);

    /** HS256 exige una clave de al menos 256 bits. */
    private static final int LARGO_MINIMO_DEL_SECRETO = 32;

    private static final String CLAIM_EMPRESA = "empresaId";
    private static final String CLAIM_NOMBRE = "nombre";
    private static final String CLAIM_EMAIL = "email";

    private final SecretKey clave;
    private final long minutosDeVigencia;

    public ServicioJwt(@Value("${erp.auth.jwt.secreto}") String secreto,
                       @Value("${erp.auth.jwt.minutos-de-vigencia:480}") long minutosDeVigencia) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LARGO_MINIMO_DEL_SECRETO) {
            throw new IllegalArgumentException(
                    "El secreto del JWT debe tener al menos " + LARGO_MINIMO_DEL_SECRETO
                            + " bytes para firmar con HS256");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.minutosDeVigencia = minutosDeVigencia;
    }

    /** Emite un token para el usuario indicado. */
    public String generar(UsuarioPrincipal usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.usuarioId().toString())
                .claim(CLAIM_EMPRESA, usuario.empresaId().toString())
                .claim(CLAIM_NOMBRE, usuario.nombre())
                .claim(CLAIM_EMAIL, usuario.email())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(minutosDeVigencia * 60)))
                .signWith(clave)
                .compact();
    }

    /**
     * Valida el token y devuelve la identidad que transporta.
     *
     * @return vacío si el token falta, está vencido, viene mal firmado o es ilegible
     */
    public Optional<UsuarioPrincipal> leer(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new UsuarioPrincipal(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.get(CLAIM_EMPRESA, String.class)),
                    claims.get(CLAIM_NOMBRE, String.class),
                    claims.get(CLAIM_EMAIL, String.class)));

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token rechazado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Minutos de vigencia del token emitido, para informarlo al cliente. */
    public long minutosDeVigencia() {
        return minutosDeVigencia;
    }
}
