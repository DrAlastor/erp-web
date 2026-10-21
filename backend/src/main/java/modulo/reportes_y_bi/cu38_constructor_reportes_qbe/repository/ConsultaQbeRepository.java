package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.repository;

import lombok.RequiredArgsConstructor;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service.ConstructorConsultaQbe.ConsultaSql;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ejecución de las consultas QBE ya construidas y lecturas de apoyo sobre {@code usuario}.
 * Solo recibe SQL generado por {@code ConstructorConsultaQbe}.
 */
@Repository
@RequiredArgsConstructor
public class ConsultaQbeRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public List<Map<String, Object>> ejecutar(ConsultaSql consulta, int limite, long desplazamiento) {
        MapSqlParameterSource params = copiar(consulta.parametros())
                .addValue("qbeLimite", limite)
                .addValue("qbeDesplazamiento", desplazamiento);
        return jdbc.queryForList(consulta.sqlOrdenado() + " LIMIT :qbeLimite OFFSET :qbeDesplazamiento", params)
                .stream()
                .map(ConsultaQbeRepository::normalizarFila)
                .toList();
    }

    public long contar(ConsultaSql consulta) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM (" + consulta.sql() + ") q", consulta.parametros(), Long.class);
        return total == null ? 0 : total;
    }

    public boolean existeUsuarioActivo(Long usuarioId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuario WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", usuarioId), Integer.class);
        return n != null && n > 0;
    }

    public Optional<Long> usuarioActivoPorEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return jdbc.queryForList(
                        "SELECT id FROM usuario WHERE LOWER(email) = LOWER(:email) AND activo = TRUE",
                        new MapSqlParameterSource("email", email.trim()), Long.class)
                .stream().findFirst();
    }

    /** Correo de cada usuario, para mostrar el autor de las plantillas. */
    public Map<Long, String> correos(Collection<Long> ids) {
        Map<Long, String> resultado = new HashMap<>();
        if (ids == null || ids.isEmpty()) return resultado;
        jdbc.query("SELECT id, email FROM usuario WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    resultado.put(rs.getLong("id"), rs.getString("email"));
                });
        return resultado;
    }

    private static MapSqlParameterSource copiar(MapSqlParameterSource origen) {
        return new MapSqlParameterSource(new HashMap<>(origen.getValues()));
    }

    /** Fechas a tipos de java.time y montos a dos decimales para un JSON uniforme. */
    private static Map<String, Object> normalizarFila(Map<String, Object> fila) {
        Map<String, Object> limpia = new LinkedHashMap<>();
        fila.forEach((clave, valor) -> {
            Object v = valor;
            if (valor instanceof Timestamp t) {
                v = t.toLocalDateTime();
            } else if (valor instanceof Date d) {
                v = d.toLocalDate();
            } else if (valor instanceof BigDecimal b) {
                v = b.setScale(2, RoundingMode.HALF_UP);
            }
            limpia.put(clave.toLowerCase(), v);
        });
        return limpia;
    }
}
