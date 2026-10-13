package comun.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

/**
 * Habilita la seguridad a nivel de método y enchufa el {@link EvaluadorDePermisos} para que
 * {@code hasPermission('MODULO','ACCION')} funcione dentro de {@code @PreAuthorize}.
 *
 * <p>El evaluador se resuelve de forma diferida a través de un {@code ObjectProvider}: la
 * infraestructura de seguridad de métodos se construye muy temprano en el arranque y pedir
 * el evaluador de entrada forzaría a inicializar JPA antes de tiempo.
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracionMetodosSeguros {

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            ObjectProvider<EvaluadorDePermisos> proveedorDelEvaluador) {

        DefaultMethodSecurityExpressionHandler manejador = new DefaultMethodSecurityExpressionHandler();
        manejador.setPermissionEvaluator(new PermissionEvaluator() {

            @Override
            public boolean hasPermission(Authentication autenticacion, Object modulo, Object accion) {
                return proveedorDelEvaluador.getObject().hasPermission(autenticacion, modulo, accion);
            }

            @Override
            public boolean hasPermission(Authentication autenticacion, Serializable idDelObjeto,
                                         String tipoDeObjeto, Object permiso) {
                return proveedorDelEvaluador.getObject()
                        .hasPermission(autenticacion, idDelObjeto, tipoDeObjeto, permiso);
            }
        });
        return manejador;
    }
}
