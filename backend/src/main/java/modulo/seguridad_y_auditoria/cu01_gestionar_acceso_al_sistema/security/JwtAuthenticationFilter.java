package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.security;

import comun.multitenancy.TenantContext;
import comun.multitenancy.TenantDescriptor;
import comun.multitenancy.MasterTenantResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.compartido.entity.Permiso;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MasterTenantResolver tenantResolver;
    private final UsuarioRepository usuarioRepository;
    private final modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service.ServicioAutorizacion servicioAutorizacion;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        TenantContext.clear();
        try {
            authenticate(request);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String token = header.substring(7);
        try {
            if (jwtService.isSuperAdmin(token)) {
                String email = jwtService.parseClaims(token).getSubject();
                List<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_SUPERADMIN"),
                        new SimpleGrantedAuthority("ALL"));
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                return;
            }

            TenantDescriptor tenant = tenantResolver.requireActive(jwtService.extractTenant(token));
            TenantContext.set(tenant);
            String username = jwtService.parseClaims(token).getSubject();
            usuarioRepository.findByEmailIgnoreCase(username)
                        .filter(u -> Boolean.TRUE.equals(u.getEnable()))
                        .ifPresent(usuario -> {
                            List<GrantedAuthority> authorities = new java.util.ArrayList<>();
                            if (usuario.getRol() != null) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()));
                                if (usuario.getRol().getPermiso() != null && !usuario.getRol().getPermiso().isBlank()) {
                                    authorities.add(new SimpleGrantedAuthority(usuario.getRol().getPermiso()));
                                }
                            }
                            try {
                                java.util.Set<String> perms = servicioAutorizacion.permisosEfectivos(usuario.getId());
                                for (String p : perms) {
                                    authorities.add(new SimpleGrantedAuthority(p));
                                    authorities.add(new SimpleGrantedAuthority("ROLE_" + p));
                                }
                            } catch (Exception ignored) {}
                            try {
                                List<String> tokenAuths = jwtService.extractAuthorities(token);
                                if (tokenAuths != null) {
                                    for (String a : tokenAuths) {
                                        authorities.add(new SimpleGrantedAuthority(a));
                                    }
                                }
                            } catch (Exception ignored) {}

                            ApplicationUserPrincipal principal =
                                    new ApplicationUserPrincipal(username, authorities,
                                        new modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal(
                                            usuario.getId(),
                                            java.util.UUID.nameUUIDFromBytes(tenant.codigoEmpresa().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                                            usuario.getFullname(),
                                            usuario.getEmail()));
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        });
        } catch (Exception ex) {
            TenantContext.clear();
        }
    }
}
