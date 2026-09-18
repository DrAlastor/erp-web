package com.uagrm.erp.backend.config;

import com.uagrm.erp.backend.auth.FiltroJwt;
import com.uagrm.erp.backend.seguridad.web.ManejadorAccesoDenegado;
import com.uagrm.erp.backend.seguridad.web.PuntoDeEntradaNoAutenticado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de seguridad de la API.
 *
 * <p>La regla de fondo es <strong>denegar por omisión</strong>: solo el login, la salud del
 * servicio y los endpoints públicos quedan abiertos; todo el resto exige un token válido.
 * Así, un endpoint nuevo que alguien agregue sin protegerlo queda cerrado en lugar de
 * abierto. Qué puede hacer cada usuario autenticado lo decide el RBAC por método, con
 * {@code @PreAuthorize("hasPermission('MODULO','ACCION')")}.
 *
 * <p>La API no usa sesión ni CSRF porque es stateless con JWT.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FiltroJwt filtroJwt;
    private final ManejadorAccesoDenegado manejadorAccesoDenegado;
    private final PuntoDeEntradaNoAutenticado puntoDeEntradaNoAutenticado;
    private final String origenesPermitidos;

    public SecurityConfig(FiltroJwt filtroJwt,
                          ManejadorAccesoDenegado manejadorAccesoDenegado,
                          PuntoDeEntradaNoAutenticado puntoDeEntradaNoAutenticado,
                          @Value("${erp.cors.origenes:http://localhost:4200,http://localhost:4000}")
                          String origenesPermitidos) {
        this.filtroJwt = filtroJwt;
        this.manejadorAccesoDenegado = manejadorAccesoDenegado;
        this.puntoDeEntradaNoAutenticado = puntoDeEntradaNoAutenticado;
        this.origenesPermitidos = origenesPermitidos;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(configuracionCors()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/health/**", "/api/public/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(manejo -> manejo
                .authenticationEntryPoint(puntoDeEntradaNoAutenticado)
                .accessDeniedHandler(manejadorAccesoDenegado)
            )
            .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Permite que el frontend Angular de desarrollo consuma la API desde otro puerto. */
    @Bean
    public CorsConfigurationSource configuracionCors() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}
