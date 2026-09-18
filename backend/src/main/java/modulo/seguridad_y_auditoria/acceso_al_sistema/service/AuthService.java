package modulo.seguridad_y_auditoria.acceso_al_sistema.service;

import modulo.seguridad_y_auditoria.acceso_al_sistema.exception.AccountLockedException;
import modulo.seguridad_y_auditoria.acceso_al_sistema.exception.InvalidCredentialsException;
import modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth.ClienteRegisterRequest;
import modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth.LoginRequest;
import modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth.RefreshTokenRequest;
import modulo.seguridad_y_auditoria.acceso_al_sistema.dto.auth.TokenResponse;
import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Sesion;
import modulo.seguridad_y_auditoria.acceso_al_sistema.mapper.AuthMapper;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.SesionRepository;
import modulo.seguridad_y_auditoria.acceso_al_sistema.security.JwtService;
import modulo.seguridad_y_auditoria.compartido.entity.Permiso;
import modulo.seguridad_y_auditoria.compartido.entity.Rol;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.PermisoRepository;
import modulo.seguridad_y_auditoria.compartido.repository.RolRepository;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.comercial_y_preventa.gestion_de_clientes.entity.Cliente;
import modulo.comercial_y_preventa.gestion_de_clientes.repository.ClienteRepository;

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
        usuario.getRoles().add(clienteRol);
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
        return usuario.getRoles().stream()
                .flatMap(rol -> rol.getPermisos().stream())
                .map(Permiso::toAuthority)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Rol CLIENTE del portal externo, con el permiso de consultar su propio perfil.
     *
     * <p>Es un rol de la tabla legacy de CU01 y no uno del catálogo de la CU03: el cliente
     * no es un empleado de la empresa y no forma parte de la matriz de 7 roles por empresa.
     */
    private Rol clienteRole() {
        Rol role = rolRepository.findByNombre("CLIENTE").orElseGet(() -> {
            Rol created = new Rol();
            created.setNombre("CLIENTE");
            created.setDescripcion("Cliente externo con acceso al portal");
            return rolRepository.save(created);
        });
        Permiso portal = permisoRepository
                .findByModuloAndPantallaAndAccion("CLIENTE", "PERFIL", "LECTURA")
                .orElseGet(() -> {
                    Permiso created = new Permiso();
                    created.setModulo("CLIENTE");
                    created.setPantalla("PERFIL");
                    created.setAccion("LECTURA");
                    created.setDescripcion("Consultar el perfil personal del cliente");
                    return permisoRepository.save(created);
                });
        role.getPermisos().add(portal);
        return rolRepository.save(role);
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
