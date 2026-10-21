package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.service;

import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.exception.AccountLockedException;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.exception.InvalidCredentialsException;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.ClienteRegisterRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.LoginRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.RefreshTokenRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.TokenResponse;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.mapper.AuthMapper;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.security.JwtService;
import modulo.seguridad_y_auditoria.compartido.entity.Permiso;
import modulo.seguridad_y_auditoria.compartido.entity.Rol;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.PermisoRepository;
import modulo.seguridad_y_auditoria.compartido.repository.RolRepository;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.repository.ClienteRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_INTENTOS_FALLIDOS = 3;
    private static final long BLOQUEO_MINUTOS = 15;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final ClienteRepository clienteRepository;
    private final modulo.notificaciones.service.NotificacionService notificacionService;
    private final modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService bitacoraAuditoriaService;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public TokenResponse login(LoginRequest request, String ip, String userAgent) {
        Usuario usuario = usuarioRepository.findForLogin(request.getUsernameOrEmail().trim())
                .filter(Usuario::getEnable)
                .orElse(null);

        if (usuario == null) {
            try {
                bitacoraAuditoriaService.registrarEventoActual(
                        "LOGIN_FALLIDO",
                        "SEGURIDAD",
                        String.format("Intento de acceso con identificador inexistente o inactivo: '%s'.", request.getUsernameOrEmail()),
                        "FALLO",
                        "MEDIA",
                        String.format("{\"identificador\":\"%s\",\"ip\":\"%s\"}", request.getUsernameOrEmail(), ip != null ? ip : "127.0.0.1")
                );
            } catch (Exception ignored) {}
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            try {
                bitacoraAuditoriaService.registrarEventoActual(
                        "INTENTO_ACCESO_BLOQUEADO",
                        "SEGURIDAD",
                        String.format("Acceso denegado a cuenta temporalmente bloqueada: '%s'.", usuario.getUsername()),
                        "ALERTA",
                        "ALTA",
                        String.format("{\"usuarioId\":%d,\"ip\":\"%s\"}", usuario.getId(), ip != null ? ip : "127.0.0.1")
                );
            } catch (Exception ignored) {}
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

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), usuario.getPassword());
        if (!passwordMatches && request.getPassword().equals(usuario.getPassword())) {
            passwordMatches = true;
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (!passwordMatches) {
            registrarIntentoFallido(usuario);
            try {
                bitacoraAuditoriaService.registrarEventoActual(
                        "LOGIN_FALLIDO",
                        "SEGURIDAD",
                        String.format("Contraseña incorrecta para el usuario '%s'. Intento #%d.", usuario.getUsername(), usuario.getIntentosFallidos()),
                        "FALLO",
                        "MEDIA",
                        String.format("{\"usuarioId\":%d,\"intentos\":%d,\"ip\":\"%s\"}", usuario.getId(), usuario.getIntentosFallidos(), ip != null ? ip : "127.0.0.1")
                );
            } catch (Exception ignored) {}
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        try {
            bitacoraAuditoriaService.registrarEventoActual(
                    "LOGIN_EXITOSO",
                    "SEGURIDAD",
                    String.format("Inicio de sesión exitoso del usuario '%s' (%s).", usuario.getUsername(), usuario.getEmail()),
                    "EXITO",
                    "BAJA",
                    String.format("{\"usuarioId\":%d,\"username\":\"%s\",\"ip\":\"%s\"}", usuario.getId(), usuario.getUsername(), ip != null ? ip : "127.0.0.1")
            );
        } catch (Exception ignored) {}

        try {
            notificacionService.notificar(
                    usuario.getId(),
                    "Inicio de Sesión",
                    String.format("Inicio de sesión exitoso desde la IP %s con el usuario '%s'.",
                            ip != null ? ip : "127.0.0.1",
                            usuario.getUsername() != null ? usuario.getUsername() : usuario.getEmail()),
                    "INFO"
            );
        } catch (Exception ignored) {}

        return emitirTokens(usuario, ip, userAgent);
    }

    @Transactional
    public TokenResponse registerCliente(ClienteRegisterRequest request, String ip, String userAgent) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        String nitCi = request.nitCi().trim();
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya está registrado");
        }
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        if (clienteRepository.existsByNitCi(nitCi)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El NIT/CI ya está registrado");
        }

        Rol clienteRol = clienteRole();
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setFullname(request.razonSocial().trim());
        usuario.setEnable(true);
        usuario.setRol(clienteRol);
        usuario = usuarioRepository.saveAndFlush(usuario);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setRazonSocial(request.razonSocial().trim());
        cliente.setNitCi(nitCi);
        cliente.setTelefono(cleanOptional(request.telefono()));
        cliente.setDireccion(cleanOptional(request.direccion()));
        cliente.setActivo(true);
        clienteRepository.save(cliente);
        return emitirTokens(usuario, ip, userAgent);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        var claims = jwtService.parseRefreshToken(request.getRefreshToken());
        var tokenTenant = jwtService.extractTenant(request.getRefreshToken());
        if (!tokenTenant.equals(comun.multitenancy.TenantContext.require())) {
            throw new InvalidCredentialsException("Refresh token invalido o expirado");
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(claims.getSubject())
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token invalido o expirado"));

        if (!Boolean.TRUE.equals(usuario.getEnable())) {
            throw new InvalidCredentialsException("Refresh token inválido o expirado");
        }

        String accessToken = jwtService.generateAccessToken(
                usuario.getEmail(), extraerAuthorities(usuario), extraerRoles(usuario));

        return authMapper.toTokenResponse(accessToken, request.getRefreshToken(),
                jwtService.getAccessExpirationSeconds(), usuario);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        jwtService.parseRefreshToken(request.getRefreshToken());
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
        String accessToken = jwtService.generateAccessToken(
                usuario.getEmail(), authorities, extraerRoles(usuario));
        String refreshTokenPlain = jwtService.generateRefreshToken(usuario.getEmail(), refreshExpirationDays);

        return authMapper.toTokenResponse(accessToken, refreshTokenPlain,
                jwtService.getAccessExpirationSeconds(), usuario);
    }

    private List<String> extraerAuthorities(Usuario usuario) {
        List<String> list = new java.util.ArrayList<>();
        if (usuario.getRol() != null) {
            list.add("ROLE_" + usuario.getRol().getNombre());
            if (usuario.getRol().getPermiso() != null) {
                list.add(usuario.getRol().getPermiso());
            }
        }
        return list;
    }

    /**
     * Rol CLIENTE del portal externo.
     */
    private Rol clienteRole() {
        return rolRepository.findByNombre("CLIENTE").orElseGet(() -> {
            Rol created = new Rol();
            created.setNombre("CLIENTE");
            created.setDescripcion("CLIENTE_PORTAL");
            created.setActivo(true);
            return rolRepository.save(created);
        });
    }

    private String cleanOptional(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private List<String> extraerRoles(Usuario usuario) {
        return usuario.getRoles().stream()
                .map(Rol::getNombre)
                .distinct()
                .toList();
    }
}
