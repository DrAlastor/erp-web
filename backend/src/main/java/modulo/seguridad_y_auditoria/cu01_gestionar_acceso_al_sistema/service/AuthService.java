package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.service;

import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.exception.AccountLockedException;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.exception.InvalidCredentialsException;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.OnboardingEmpresaRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.OnboardingEmpresaResponse;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.ClienteRegisterRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.LoginRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.RefreshTokenRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.TokenResponse;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.entity.Sesion;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.mapper.AuthMapper;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.repository.SesionRepository;
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
    private final SesionRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthMapper authMapper;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final ClienteRepository clienteRepository;
    private final modulo.notificaciones.service.NotificacionService notificacionService;

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
    public OnboardingEmpresaResponse onboardingEmpresa(OnboardingEmpresaRequest request, String ip, String userAgent) {
        String adminEmail = request.adminEmail().trim().toLowerCase();
        String username = adminEmail.split("@")[0];

        if (usuarioRepository.existsByEmailIgnoreCase(adminEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo corporativo ya se encuentra registrado");
        }

        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            username = username + "_" + (System.currentTimeMillis() % 10000);
        }

        Rol adminRol = rolRepository.findByNombre("ADMINISTRADOR")
                .or(() -> rolRepository.findByNombre("ADMIN"))
                .orElseGet(() -> {
                    Rol r = new Rol();
                    r.setNombre("ADMINISTRADOR");
                    r.setDescripcion("ALL");
                    return rolRepository.save(r);
                });

        Usuario adminUser = new Usuario();
        adminUser.setUsername(username);
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(request.adminPassword()));
        adminUser.setFullname(request.adminNombre().trim());
        adminUser.setEnable(true);
        adminUser.setRol(adminRol);
        adminUser = usuarioRepository.saveAndFlush(adminUser);

        TokenResponse tokens = emitirTokens(adminUser, ip, userAgent);

        String cleanName = request.razonSocial().replaceAll("[^a-zA-Z0-9]", "");
        String dbName = "ERP_" + (cleanName.length() > 25 ? cleanName.substring(0, 25) : cleanName);

        return new OnboardingEmpresaResponse(
                "¡Empresa y Administrador aprovisionados con éxito!",
                request.razonSocial().trim(),
                request.nit().trim(),
                dbName,
                request.planCodigo(),
                adminEmail,
                "ADMINISTRADOR",
                tokens
        );
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        Sesion sesion = buscarSesionActiva(request.getRefreshToken());
        Usuario usuario = sesion.getUsuario();

        if (!Boolean.TRUE.equals(usuario.getEnable())) {
            throw new InvalidCredentialsException("Refresh token inválido o expirado");
        }

        String accessToken = jwtService.generateAccessToken(
                usuario.getUsername(), extraerAuthorities(usuario), extraerRoles(usuario));

        return authMapper.toTokenResponse(accessToken, request.getRefreshToken(),
                jwtService.getAccessExpirationSeconds(), usuario);
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
        String accessToken = jwtService.generateAccessToken(
                usuario.getUsername(), authorities, extraerRoles(usuario));
        String refreshTokenPlain = jwtService.generateRefreshToken();

        Sesion sesion = new Sesion();
        sesion.setUsuario(usuario);
        sesion.setRefreshTokenHash(jwtService.hashToken(refreshTokenPlain));
        sesion.setIpOrigen(ip);
        sesion.setUserAgent(userAgent);
        sesion.setFechaExpiracion(LocalDateTime.now().plusDays(refreshExpirationDays));
        sesionRepository.save(sesion);

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
