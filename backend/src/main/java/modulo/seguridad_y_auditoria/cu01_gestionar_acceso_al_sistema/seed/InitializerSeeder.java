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

/**
 * Siembra los usuarios de acceso inicial garantizando que tanto el superadministrador
 * central como los administradores de la empresa cuenten con credenciales válidas y roles activos.
 */
@org.springframework.core.annotation.Order(0)
@Component
@RequiredArgsConstructor
@Slf4j
public class InitializerSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Obtener rol administrador principal (ID 1 'ADMINISTRADOR' o 'ADMIN')
        Rol adminRol = rolRepository.findByNombre("ADMINISTRADOR")
                .or(() -> rolRepository.findByNombre("ADMIN"))
                .orElseGet(() -> rolRepository.save(crearRolAdmin()));

        // 1. Sembrar o actualizar Superadmin (superadmin@nexoerp.com / Admin2026!)
        asegurarUsuario("superadmin@nexoerp.com", "Admin2026!", "Superadmin Central", "SUPERADMIN", adminRol);

        // 2. Sembrar o verificar Administrador ASUS (admin@asustech.bo / Asus2026!)
        asegurarUsuario("admin@asustech.bo", "Asus2026!", "Administrador ASUS Tech", "GERENCIA", adminRol);

        // 3. Sembrar o verificar Administrador General ERP (admin@erp.com / Admin123!)
        asegurarUsuario("admin@erp.com", "Admin123!", "Administrador General", "GERENCIA", adminRol);
    }

    private void asegurarUsuario(String email, String plainPassword, String fullname, String nivel, Rol rol) {
        var opt = usuarioRepository.findByEmailIgnoreCase(email);
        if (opt.isEmpty()) {
            Usuario u = new Usuario();
            u.setEmail(email);
            u.setUsername(email.split("@")[0]);
            u.setPassword(passwordEncoder.encode(plainPassword));
            u.setFullname(fullname);
            u.setNivelUsuario(nivel);
            u.setEnable(true);
            u.setRol(rol);
            usuarioRepository.save(u);
            log.info("Usuario creado: {} (Rol: {})", email, rol.getNombre());
        } else {
            Usuario u = opt.get();
            boolean modificado = false;
            if (u.getRol() == null) {
                u.setRol(rol);
                modificado = true;
            }
            if (!Boolean.TRUE.equals(u.getActivo())) {
                u.setActivo(true);
                modificado = true;
            }
            // Asegurar que la contraseña coincida con la requerida si no valida
            if (!passwordEncoder.matches(plainPassword, u.getPassword())) {
                u.setPassword(passwordEncoder.encode(plainPassword));
                modificado = true;
                log.info("Contraseña de {} sincronizada correctamente.", email);
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
