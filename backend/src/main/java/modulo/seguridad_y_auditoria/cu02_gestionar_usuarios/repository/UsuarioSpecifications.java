package modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository;

import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class UsuarioSpecifications {

    private static final char ESCAPE = '!';

    private UsuarioSpecifications() {
    }

    public static Specification<Usuario> filtrar(String search, Integer targetRolId, Boolean enable, UUID empresaId) {
        return (raiz, consulta, constructor) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String patron = "%" + search.trim().toLowerCase(Locale.ROOT)
                        .replace(String.valueOf(ESCAPE), ESCAPE + "" + ESCAPE)
                        .replace("%", ESCAPE + "%")
                        .replace("_", ESCAPE + "_") + "%";
                condiciones.add(constructor.or(
                        constructor.like(constructor.lower(raiz.get("email")), patron, ESCAPE),
                        constructor.like(constructor.lower(constructor.coalesce(raiz.get("fullname"), "")), patron, ESCAPE),
                        constructor.like(constructor.lower(constructor.coalesce(raiz.get("username"), "")), patron, ESCAPE),
                        constructor.like(constructor.lower(constructor.coalesce(raiz.get("nivelUsuario"), "")), patron, ESCAPE)));
            }

            if (targetRolId != null) {
                if (targetRolId == -1) {
                    condiciones.add(constructor.disjunction());
                } else {
                    condiciones.add(constructor.equal(raiz.get("rol").get("id"), targetRolId));
                }
            }

            if (enable != null) {
                condiciones.add(constructor.equal(raiz.get("activo"), enable));
            }

            return condiciones.isEmpty() ? constructor.conjunction() : constructor.and(condiciones.toArray(Predicate[]::new));
        };
    }

    public static Specification<Usuario> filtrar(String search, UUID role, Boolean enable, UUID empresaId) {
        return filtrar(search, (Integer) null, enable, empresaId);
    }
}
