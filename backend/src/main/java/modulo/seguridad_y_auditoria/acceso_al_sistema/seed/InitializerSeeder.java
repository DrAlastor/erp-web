package modulo.seguridad_y_auditoria.acceso_al_sistema.seed;

import modulo.seguridad_y_auditoria.compartido.entity.Rol;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.RolRepository;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
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
@org.springframework.core.annotation.Order(0)
@Component
@RequiredArgsConstructor
@Slf4j
public class InitializerSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
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

        // Los permisos ya no se siembran acá: el catálogo y la matriz de roles son de la CU03
        // (CatalogoPermisos y ArranqueSeguridad). Este seeder solo deja el rol ADMIN legacy y
        // el usuario de prueba del login, que es lo que la HU-01 necesita para arrancar.
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

    private Rol crearRolAdmin() {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        rol.setDescripcion("Administrador global del sistema con acceso completo");
        return rol;
    }
}
