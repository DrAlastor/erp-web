package modulo.Comercial_y_Preventa.Gestion_de_Clientes.service;

import comun.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.dto.ClienteRequest;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.dto.ClienteResponse;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.entity.Cliente;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.repository.ClienteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clientes;

    public Page<ClienteResponse> list(
            String search, Boolean activo, int page, int size) {
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
                        cb.like(cb.lower(root.get("nitCi")), term, '!')));
            }
            if (activo != null) conditions.add(cb.equal(root.get("activo"), activo));
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return clientes.findAll(filters, PageRequest.of(page, size, Sort.by("id")))
                .map(this::response);
    }

    public ClienteResponse detail(Long id) {
        return response(find(id));
    }

    public ClienteResponse current(String username) {
        return clientes.findByUsuarioUsername(username)
                .map(this::response)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de cliente inexistente"));
    }

    @Transactional
    public ClienteResponse create(ClienteRequest request) {
        String nitCi = cleanRequired(request.nitCi());
        if (clientes.existsByNitCi(nitCi)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El NIT/CI ya está registrado");
        }

        Cliente cliente = new Cliente();
        apply(cliente, request, nitCi);
        cliente.setActivo(true);
        return response(clientes.save(cliente));
    }

    @Transactional
    public ClienteResponse update(Long id, ClienteRequest request) {
        Cliente cliente = find(id);
        String nitCi = cleanRequired(request.nitCi());
        if (clientes.existsByNitCiAndIdNot(nitCi, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El NIT/CI ya está registrado");
        }

        apply(cliente, request, nitCi);
        return response(clientes.save(cliente));
    }

    @Transactional
    public ClienteResponse status(Long id, boolean activo) {
        Cliente cliente = find(id);
        cliente.setActivo(activo);
        if (cliente.getUsuario() != null) cliente.getUsuario().setEnable(activo);
        return response(clientes.save(cliente));
    }

    private Cliente find(Long id) {
        return clientes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente inexistente"));
    }

    private void apply(Cliente cliente, ClienteRequest request, String nitCi) {
        cliente.setRazonSocial(cleanRequired(request.razonSocial()));
        cliente.setNitCi(nitCi);
        cliente.setTelefono(cleanOptional(request.telefono()));
        cliente.setDireccion(cleanOptional(request.direccion()));
    }

    private String cleanRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanOptional(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private ClienteResponse response(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getRazonSocial(),
                cliente.getNitCi(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getActivo(),
                cliente.getFechaCreacion(),
                cliente.getUsuario() == null ? null : cliente.getUsuario().getId(),
                cliente.getUsuario() == null ? null : cliente.getUsuario().getUsername(),
                cliente.getUsuario() == null ? null : cliente.getUsuario().getEmail());
    }
}
