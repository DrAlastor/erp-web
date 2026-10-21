package comun.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registro de interceptores web globales en Spring MVC.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditoriaGlobalInterceptor auditoriaGlobalInterceptor;

    public WebMvcConfig(AuditoriaGlobalInterceptor auditoriaGlobalInterceptor) {
        this.auditoriaGlobalInterceptor = auditoriaGlobalInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditoriaGlobalInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/bitacora/**",
                        "/api/superadmin/bitacora/**",
                        "/api/health/**"
                );
    }
}
