package modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos;

import comun.multitenancy.TenantContext;
import comun.multitenancy.TenantDescriptor;
import modulo.seguridad_y_auditoria.compartido.entity.Rol;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service.ServicioAutorizacion;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service.ServicioRoles;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** El mismo id de usuario en dos empresas no comparte permisos cacheados. */
class ServicioAutorizacionTenantTest {

    private static Usuario usuarioCon(String rolNombre, String permiso) {
        Rol rol = new Rol();
        rol.setNombre(rolNombre);
        rol.setPermiso(permiso);
        rol.setActivo(true);
        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        return usuario;
    }

    @Test
    void elCacheNoCruzaEmpresas() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ServicioRoles roles = mock(ServicioRoles.class);
        when(roles.obtenerPermisosEfectivos(org.mockito.ArgumentMatchers.any())).thenReturn(Set.of());
        when(usuarios.findById(2L))
                .thenReturn(Optional.of(usuarioCon("VENDEDOR", "VENTAS_ONLY")))
                .thenReturn(Optional.of(usuarioCon("CAJERO", "CAJA_ONLY")));
        ServicioAutorizacion servicio = new ServicioAutorizacion(usuarios, roles, mock(JdbcTemplate.class));

        boolean vendedorModifica = TenantContext.execute(new TenantDescriptor(1L, "EMP-A"),
                () -> servicio.tienePermiso(2L, "COMERCIAL_MODIFICAR"));
        boolean cajeroModifica = TenantContext.execute(new TenantDescriptor(2L, "EMP-B"),
                () -> servicio.tienePermiso(2L, "COMERCIAL_MODIFICAR"));

        assertTrue(vendedorModifica);
        assertFalse(cajeroModifica);
    }
}
