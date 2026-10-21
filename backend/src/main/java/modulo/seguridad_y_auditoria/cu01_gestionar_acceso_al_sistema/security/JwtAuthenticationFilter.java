package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.security;

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
    private final UsuarioRepository usuarioRepository;
    private final modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.service.ServicioAutorizacion servicioAutorizacion;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtService.parseClaims(token).getSubject();
                usuarioRepository.findByUsername(username)
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
                                            usuario.getEmpresaId() != null ? usuario.getEmpresaId() : java.util.UUID.nameUUIDFromBytes("ERP_TENANT_DEFAULT".getBytes()),
                                            usuario.getFullname(),
                                            usuario.getEmail()));
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        });
            } catch (Exception ex) {
                // Un JWT invalido o vencido queda sin autenticar (401 en rutas protegidas).
            }
        }

        filterChain.doFilter(request, response);
    }
}
