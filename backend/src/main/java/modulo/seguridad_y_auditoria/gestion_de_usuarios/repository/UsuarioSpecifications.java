package modulo.seguridad_y_auditoria.gestion_de_usuarios.repository;

import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.roles_y_permisos.entity.UsuarioRol;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Filtros del listado paginado de cuentas: empresa, texto libre y rol asignado.
 *
 * <p>El texto se compara en minúsculas y con los comodines de SQL escapados, para que un
 * {@code %} escrito por el usuario busque ese carácter y no "cualquier cosa". El filtro por
 * rol se resuelve con una subconsulta sobre {@code usuario_rol}, así que un usuario con
 * varios roles aparece una sola vez.
 */
public final class UsuarioSpecifications {

    /** Carácter de escape de los comodines de {@code LIKE}. */
    private static final char ESCAPE = '!';

    private UsuarioSpecifications() {
    }

    public static Specification<Usuario> filtrar(String search, UUID role, Boolean enable, UUID empresaId) {
        return (raiz, consulta, constructor) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(constructor.equal(raiz.get("empresaId"), empresaId));

            if (search != null && !search.isBlank()) {
                String patron = "%" + search.trim().toLowerCase(Locale.ROOT)
                        .replace(String.valueOf(ESCAPE), ESCAPE + "" + ESCAPE)
                        .replace("%", ESCAPE + "%")
                        .replace("_", ESCAPE + "_") + "%";
                condiciones.add(constructor.or(
                        constructor.like(constructor.lower(raiz.get("username")), patron, ESCAPE),
                        constructor.like(constructor.lower(raiz.get("email")), patron, ESCAPE),
                        constructor.like(constructor.lower(raiz.get("fullname")), patron, ESCAPE)));
            }

            if (role != null) {
                var subconsulta = consulta.subquery(Long.class);
                var asignacion = subconsulta.from(UsuarioRol.class);
                subconsulta.select(asignacion.get("id").get("usuarioId"))
                        .where(constructor.equal(asignacion.get("id").get("rolId"), role),
                                constructor.equal(asignacion.get("rol").get("empresaId"), empresaId));
                condiciones.add(raiz.get("id").in(subconsulta));
            }

            if (enable != null) {
                condiciones.add(constructor.equal(raiz.get("enable"), enable));
            }

            return constructor.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
