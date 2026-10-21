package modulo.ventas_y_facturacion.cu13_gestionar_clientes.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.dto.ClienteRequest;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.dto.ClienteResponse;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.mapper.ClienteMapper;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.repository.ClienteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clientes;
    private final ClienteMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public Page<ClienteResponse> list(
            String search, String condicionPago, Boolean activo, int page, int size) {
        Specification<Cliente> filters = (root, query, cb) -> {
            var conditions = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (search != null && !search.isBlank()) {
                String term = "%"
                        + search.trim().toLowerCase(Locale.ROOT)
                                .replace("!", "!!")
                                .replace("%", "!%")
                                .replace("_", "!_")
                        + "%";
                conditions.add(cb.or(
                        cb.like(cb.lower(root.get("razonSocial")), term, '!'),
                        cb.like(cb.lower(root.get("nitCi")), term, '!'),
                        cb.like(cb.lower(root.get("ciudad")), term, '!'),
                        cb.like(cb.lower(root.get("email")), term, '!')));
            }
            if (condicionPago != null && !condicionPago.isBlank()) {
                conditions.add(cb.equal(cb.upper(root.get("condicionPago")), condicionPago.trim().toUpperCase(Locale.ROOT)));
            }
            if (activo != null) {
                conditions.add(cb.equal(root.get("activo"), activo));
            }
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return clientes.findAll(filters, PageRequest.of(page, size, Sort.by("id").descending()))
                .map(mapper::toResponse);
    }

    public ClienteResponse detail(Long id) {
        return mapper.toResponse(find(id));
    }

    public ClienteResponse current(String username) {
        return clientes.findByUsuarioUsername(username)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de cliente inexistente"));
    }

    @Transactional
    public ClienteResponse create(ClienteRequest request) {
        String nitCi = cleanRequired(request.nitCi());
        if (clientes.existsByNitCi(nitCi)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El NIT/CI '" + nitCi + "' ya está registrado.");
        }

        validarCondicionesCrediticias(request);

        Cliente cliente = new Cliente();
        apply(cliente, request, nitCi);
        cliente.setActivo(true);
        return mapper.toResponse(clientes.save(cliente));
    }

    @Transactional
    public ClienteResponse update(Long id, ClienteRequest request) {
        Cliente cliente = find(id);
        String nitCi = cleanRequired(request.nitCi());
        if (clientes.existsByNitCiAndIdNot(nitCi, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El NIT/CI '" + nitCi + "' ya pertenece a otro cliente.");
        }

        validarCondicionesCrediticias(request);

        apply(cliente, request, nitCi);
        return mapper.toResponse(clientes.save(cliente));
    }

    @Transactional
    public ClienteResponse status(Long id, boolean activo) {
        Cliente cliente = find(id);
        cliente.setActivo(activo);
        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setEnable(activo);
        }
        return mapper.toResponse(clientes.save(cliente));
    }

    @Transactional
    public void delete(Long id) {
        Cliente cliente = find(id);

        // Validar si tiene operaciones vinculadas (Cotizaciones, Pedidos, Ventas)
        long cotizacionesCount = contarOperaciones("COTIZACIONES", id);
        long pedidosCount = contarOperaciones("PEDIDOS", id);
        long ventasCount = contarOperaciones("VENTAS", id);

        if (cotizacionesCount > 0 || pedidosCount > 0 || ventasCount > 0) {
            // Aplicar baja lógica obligatoria según regla de CU13
            cliente.setActivo(false);
            if (cliente.getUsuario() != null) {
                cliente.getUsuario().setEnable(false);
            }
            clientes.save(cliente);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El cliente registra historial comercial (" + (cotizacionesCount + pedidosCount + ventasCount)
                    + " operaciones). Se aplicó baja lógica (inactivación) para preservar la integridad contable.");
        }

        try {
            clientes.delete(cliente);
            clientes.flush();
        } catch (DataIntegrityViolationException ex) {
            cliente.setActivo(false);
            clientes.save(cliente);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No es posible eliminar el cliente por restricciones de integridad. Se procedió a inactivarlo.");
        }
    }

    private long contarOperaciones(String tabla, Long clienteId) {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + tabla + " WHERE CLIENTE_ID = ?", Long.class, clienteId);
            return count != null ? count : 0L;
        } catch (Exception ex) {
            return 0L;
        }
    }

    private void validarCondicionesCrediticias(ClienteRequest request) {
        String condicion = request.condicionPago() != null ? request.condicionPago().trim().toUpperCase(Locale.ROOT) : "CONTADO";
        if ("CREDITO".equals(condicion)) {
            BigDecimal limite = request.limiteCredito();
            Integer dias = request.diasCredito();
            if (limite == null || limite.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para clientes con condición CREDITO, el límite de crédito debe ser mayor a 0.");
            }
            if (dias == null || dias <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para clientes con condición CREDITO, los días de plazo deben ser mayores a 0.");
            }
        }
    }

    private Cliente find(Long id) {
        return clientes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente inexistente con ID: " + id));
    }

    private void apply(Cliente cliente, ClienteRequest request, String nitCi) {
        cliente.setRazonSocial(cleanRequired(request.razonSocial()));
        cliente.setNitCi(nitCi);
        cliente.setTelefono(cleanOptional(request.telefono()));
        cliente.setEmail(cleanOptional(request.email()));
        cliente.setDireccion(cleanOptional(request.direccion()));
        cliente.setCiudad(cleanOptional(request.ciudad()));

        String condicion = request.condicionPago() != null ? request.condicionPago().trim().toUpperCase(Locale.ROOT) : "CONTADO";
        cliente.setCondicionPago(condicion);

        if ("CREDITO".equals(condicion)) {
            cliente.setLimiteCredito(request.limiteCredito() != null ? request.limiteCredito() : BigDecimal.ZERO);
            cliente.setDiasCredito(request.diasCredito() != null ? request.diasCredito() : 0);
        } else {
            cliente.setLimiteCredito(request.limiteCredito() != null ? request.limiteCredito() : BigDecimal.ZERO);
            cliente.setDiasCredito(request.diasCredito() != null ? request.diasCredito() : 0);
        }
    }

    private String cleanRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanOptional(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
