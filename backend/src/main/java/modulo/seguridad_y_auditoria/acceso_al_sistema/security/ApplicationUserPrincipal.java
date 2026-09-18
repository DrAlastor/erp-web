package modulo.seguridad_y_auditoria.acceso_al_sistema.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class ApplicationUserPrincipal implements UserDetails {

    private final String username;
    private modulo.seguridad_y_auditoria.roles_y_permisos.security.UsuarioPrincipal identidad;
    private final Collection<? extends GrantedAuthority> authorities;

    public ApplicationUserPrincipal(String username, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.authorities = authorities;
    }

    public ApplicationUserPrincipal(String username, Collection<? extends GrantedAuthority> authorities,
            modulo.seguridad_y_auditoria.roles_y_permisos.security.UsuarioPrincipal identidad) {
        this(username, authorities);
        this.identidad = identidad;
    }

    public modulo.seguridad_y_auditoria.roles_y_permisos.security.UsuarioPrincipal identidad() {
        return identidad;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
