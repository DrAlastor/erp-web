package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.seed;

import modulo.seguridad_y_auditoria.compartido.entity.Rol;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.RolRepository;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Siembra los usuarios de acceso inicial garantizando que tanto el superadministrador
 * central como los administradores de la empresa cuenten con credenciales válidas y roles activos.
 */
@org.springframework.core.annotation.Order(0)
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.demo-initializers.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class InitializerSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 0. Asegurar columna username en tabla usuario de la base de datos
        try {
            jdbcTemplate.execute("ALTER TABLE usuario ADD COLUMN IF NOT EXISTS username VARCHAR(100)");
            jdbcTemplate.execute("UPDATE usuario SET username = split_part(email, '@', 1) WHERE username IS NULL OR username = ''");
        } catch (Exception ex) {
            log.warn("Nota sobre columna username en InitializerSeeder: {}", ex.getMessage());
        }

        // Obtener roles requeridos
        Rol adminRol = rolRepository.findByNombre("ADMINISTRADOR")
                .or(() -> rolRepository.findByNombre("ADMIN"))
                .orElseGet(() -> rolRepository.save(crearRolAdmin()));
        Rol ventasRol = rolRepository.findByNombre("VENDEDOR").orElse(adminRol);
        Rol cajeroRol = rolRepository.findByNombre("CAJERO").orElse(adminRol);
        Rol almacenRol = rolRepository.findByNombre("ALMACENERO").orElse(adminRol);

        // 1. Sembrar Superadmin solo si no existe
        asegurarUsuario("superadmin@nexoerp.com", "Admin2026!", "Superadmin Central", "SUPERADMIN", adminRol);

        // 2. Sembrar Administradores ASUS solo si no existen
        asegurarUsuario("admin@asustech.bo", "Asus2026!", "Administrador ASUS Tech", "GERENCIA", adminRol);
        asegurarUsuario("alessandro@asustech.bo", "Asus2026!", "Alessandro Yevara Ponce", "GERENCIA", adminRol);
        asegurarUsuario("admin@asus.com", "Asus2026!", "Administrador ASUS", "GERENCIA", adminRol);

        // 3. Sembrar Administrador General ERP solo si no existe
        asegurarUsuario("admin@erp.com", "Admin123!", "Administrador General", "GERENCIA", adminRol);

        // 4. Sembrar usuarios operativos ASUS Tech solo si no existen
        asegurarUsuario("ventas@asustech.bo", "Ventas2026!", "Alejandro Paz Vaca", "VENTAS", ventasRol);
        asegurarUsuario("caja@asustech.bo", "Caja2026!", "Laura Morales Justiniano", "CAJA", cajeroRol);
        asegurarUsuario("almacen@asustech.bo", "Almacen2026!", "Roberto Vaca Mendez", "ALMACEN", almacenRol);
    }

    private void asegurarUsuario(String email, String plainPassword, String fullname, String nivel, Rol rol) {
        var opt = usuarioRepository.findByEmailIgnoreCase(email);
        if (opt.isEmpty()) {
            Usuario u = new Usuario();
            u.setEmail(email.toLowerCase().trim());
            u.setUsername(email.split("@")[0]);
            u.setPassword(passwordEncoder.encode(plainPassword));
            u.setFullname(fullname);
            u.setNivelUsuario(nivel);
            u.setEnable(true);
            u.setRol(rol);
            usuarioRepository.save(u);
            log.info("Usuario inicial sembrado: {} (Rol: {})", email, rol.getNombre());
        } else {
            // El usuario ya existe en la base de datos: NO MODIFICAR SU CONTRASEÑA
            Usuario u = opt.get();
            boolean modificado = false;
            if (u.getUsername() == null || u.getUsername().isBlank()) {
                u.setUsername(email.split("@")[0]);
                modificado = true;
            }
            if (u.getRol() == null) {
                u.setRol(rol);
                modificado = true;
            }
            if (!Boolean.TRUE.equals(u.getActivo())) {
                u.setActivo(true);
                modificado = true;
            }
            if (modificado) {
                usuarioRepository.save(u);
            }
        }
    }

    private Rol crearRolAdmin() {
        Rol rol = new Rol();
        rol.setNombre("ADMINISTRADOR");
        rol.setPermiso("ALL");
        rol.setActivo(true);
        return rol;
    }
}
