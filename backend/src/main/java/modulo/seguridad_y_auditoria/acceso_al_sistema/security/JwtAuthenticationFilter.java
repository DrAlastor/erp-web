package modulo.seguridad_y_auditoria.acceso_al_sistema.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Permiso;
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
                            List<GrantedAuthority> authorities = usuario.getRoles().stream()
                                    .flatMap(rol -> rol.getPermisos().stream())
                                    .map(Permiso::toAuthority)
                                    .distinct()
                                    .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p))
                                    .collect(Collectors.toList());
                            ApplicationUserPrincipal principal =
                                    new ApplicationUserPrincipal(username, authorities,
                                        new modulo.seguridad_y_auditoria.roles_y_permisos.auth.UsuarioPrincipal(
                                            usuario.getId(), usuario.getEmpresaId(), usuario.getFullname(), usuario.getEmail()));
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        });
            } catch (JwtException | IllegalArgumentException ex) {
                // Un JWT invalido o vencido queda sin autenticar (401 en rutas protegidas).
            }
        }

        filterChain.doFilter(request, response);
    }
}
