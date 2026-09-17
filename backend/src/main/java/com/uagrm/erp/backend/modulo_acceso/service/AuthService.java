package com.uagrm.erp.backend.modulo_acceso.service;

import com.uagrm.erp.backend.exception.AccountLockedException;
import com.uagrm.erp.backend.exception.InvalidCredentialsException;
import com.uagrm.erp.backend.modulo_acceso.dto.auth.LoginRequest;
import com.uagrm.erp.backend.modulo_acceso.dto.auth.RefreshTokenRequest;
import com.uagrm.erp.backend.modulo_acceso.dto.auth.TokenResponse;
import com.uagrm.erp.backend.modulo_acceso.dto.auth.UsuarioPerfilResponse;
import com.uagrm.erp.backend.modulo_acceso.entity.Permiso;
import com.uagrm.erp.backend.modulo_acceso.entity.Sesion;
import com.uagrm.erp.backend.modulo_acceso.entity.Usuario;
import com.uagrm.erp.backend.modulo_acceso.repository.SesionRepository;
import com.uagrm.erp.backend.modulo_acceso.repository.UsuarioRepository;
import com.uagrm.erp.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_INTENTOS_FALLIDOS = 3;
    private static final long BLOQUEO_MINUTOS = 15;

    private final UsuarioRepository usuarioRepository;
    private final SesionRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public TokenResponse login(LoginRequest request, String ip, String userAgent) {
        Usuario usuario = usuarioRepository.findForLogin(request.getUsernameOrEmail().trim())
                .filter(Usuario::getEnable)
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o contraseña incorrectos"));

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            throw new AccountLockedException(
                    "Tu cuenta ha sido bloqueada temporalmente por acumular " + MAX_INTENTOS_FALLIDOS +
                            " intentos fallidos. Inténtalo de nuevo en " + BLOQUEO_MINUTOS + " minutos.",
                    usuario.getBloqueadoHasta()
            );
        }

        // Un bloqueo vencido inicia una nueva ventana de tres intentos.
        if (usuario.getBloqueadoHasta() != null) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            registrarIntentoFallido(usuario);
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        return emitirTokens(usuario, ip, userAgent);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        Sesion sesion = buscarSesionActiva(request.getRefreshToken());
        Usuario usuario = sesion.getUsuario();

        if (!Boolean.TRUE.equals(usuario.getEnable())) {
            throw new InvalidCredentialsException("Refresh token inválido o expirado");
        }

        String accessToken = jwtService.generateAccessToken(usuario.getUsername(), extraerAuthorities(usuario));

        return new TokenResponse(
                accessToken,
                request.getRefreshToken(),
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                toPerfil(usuario)
        );
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String hash = jwtService.hashToken(request.getRefreshToken());
        sesionRepository.findByRefreshTokenHash(hash)
                .filter(s -> s.getFechaCierre() == null)
                .ifPresent(s -> {
                    s.setFechaCierre(LocalDateTime.now());
                    sesionRepository.save(s);
                });
    }

    private Sesion buscarSesionActiva(String refreshTokenPlano) {
        String hash = jwtService.hashToken(refreshTokenPlano);
        return sesionRepository.findByRefreshTokenHash(hash)
                .filter(s -> s.getFechaCierre() == null)
                .filter(s -> s.getFechaExpiracion().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token inválido o expirado"));
    }

    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= MAX_INTENTOS_FALLIDOS) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(BLOQUEO_MINUTOS));
        }
        usuarioRepository.save(usuario);
    }

    private TokenResponse emitirTokens(Usuario usuario, String ip, String userAgent) {
        List<String> authorities = extraerAuthorities(usuario);
        String accessToken = jwtService.generateAccessToken(usuario.getUsername(), authorities);
        String refreshTokenPlain = jwtService.generateRefreshToken();

        Sesion sesion = new Sesion();
        sesion.setUsuario(usuario);
        sesion.setRefreshTokenHash(jwtService.hashToken(refreshTokenPlain));
        sesion.setIpOrigen(ip);
        sesion.setUserAgent(userAgent);
        sesion.setFechaExpiracion(LocalDateTime.now().plusDays(refreshExpirationDays));
        sesionRepository.save(sesion);

        return new TokenResponse(
                accessToken,
                refreshTokenPlain,
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                toPerfil(usuario)
        );
    }

    private List<String> extraerAuthorities(Usuario usuario) {
        return usuario.getRoles().stream()
                .flatMap(rol -> rol.getPermisos().stream())
                .map(Permiso::toAuthority)
                .distinct()
                .collect(Collectors.toList());
    }

    private UsuarioPerfilResponse toPerfil(Usuario usuario) {
        return new UsuarioPerfilResponse(usuario.getId(), usuario.getUsername(), usuario.getEmail(), usuario.getFullname());
    }
}
