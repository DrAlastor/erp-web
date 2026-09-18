package modulo.seguridad_y_auditoria.acceso_al_sistema.seed;

import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Rol;
import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Permiso;
import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Usuario;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.PermisoRepository;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.RolRepository;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra el rol ADMIN y un usuario administrador de prueba para poder ejercitar
 * el flujo de login (HU-01) sin depender de HU-02 (todavía no implementada).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitializerSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.admin.email}")
    private String adminEmail;

    @Value("${seed.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Rol adminRol = rolRepository.findByNombre("ADMIN")
                .orElseGet(() -> rolRepository.save(crearRolAdmin()));

        Permiso clientesLectura = ensurePermission(
                "COMERCIAL", "CLIENTES", "LECTURA", "Ver lista de clientes");
        Permiso clientesEscritura = ensurePermission(
                "COMERCIAL", "CLIENTES", "ESCRITURA", "Crear y modificar clientes");
        Permiso perfilCliente = ensurePermission(
                "CLIENTE", "PERFIL", "LECTURA", "Consultar el perfil personal del cliente");
        adminRol.getPermisos().add(clientesLectura);
        adminRol.getPermisos().add(clientesEscritura);
        adminRol.getPermisos().add(perfilCliente);
        adminRol.getPermisos().addAll(permisoRepository.findAll());
        rolRepository.save(adminRol);

        if (usuarioRepository.findByUsernameOrEmail(adminEmail).isEmpty()) {
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullname("Administrador General");
            admin.setEnable(true);
            admin.setCreatedBy("system");
            admin.getRoles().add(adminRol);
            usuarioRepository.save(admin);
            log.info("Usuario administrador semilla creado: {}", adminEmail);
        } else {
            Usuario admin = usuarioRepository.findByUsernameOrEmail(adminEmail).orElseThrow();
            if (!admin.getRoles().contains(adminRol)) {
                admin.getRoles().add(adminRol);
                usuarioRepository.save(admin);
            }
        }
    }

    private Permiso ensurePermission(String modulo, String pantalla, String accion, String descripcion) {
        return permisoRepository.findByModuloAndPantallaAndAccion(modulo, pantalla, accion)
                .orElseGet(() -> {
                    Permiso permiso = new Permiso();
                    permiso.setModulo(modulo);
                    permiso.setPantalla(pantalla);
                    permiso.setAccion(accion);
                    permiso.setDescripcion(descripcion);
                    return permisoRepository.save(permiso);
                });
    }

    private Rol crearRolAdmin() {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        rol.setDescripcion("Administrador global del sistema con acceso completo");
        return rol;
    }
}
