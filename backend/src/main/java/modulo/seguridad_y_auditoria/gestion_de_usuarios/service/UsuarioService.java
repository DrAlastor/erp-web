package modulo.seguridad_y_auditoria.gestion_de_usuarios.service;

import comun.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import modulo.seguridad_y_auditoria.acceso_al_sistema.entity.Usuario;
import modulo.seguridad_y_auditoria.acceso_al_sistema.repository.*;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.*;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final JdbcTemplate jdbc;

    public Page<UsuarioResponse> list(
            String search, Integer role, Boolean enable, int page, int size) {
        Specification<Usuario> filters =
                (root, query, cb) -> {
                    query.distinct(true);
                    var conditions =
                            new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                    if (search != null && !search.isBlank()) {
                        String term =
                                "%"
                                        + search.trim()
                                                .toLowerCase(java.util.Locale.ROOT)
                                                .replace("!", "!!")
                                                .replace("%", "!%")
                                                .replace("_", "!_")
                                        + "%";
                        conditions.add(
                                cb.or(
                                        cb.like(cb.lower(root.get("username")), term, '!'),
                                        cb.like(cb.lower(root.get("email")), term, '!'),
                                        cb.like(cb.lower(root.get("fullname")), term, '!')));
                    }
                    if (role != null) conditions.add(cb.equal(root.join("roles").get("id"), role));
                    if (enable != null) conditions.add(cb.equal(root.get("enable"), enable));
                    return cb.and(
                            conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
                };
        return usuarios.findAll(filters, PageRequest.of(page, size, Sort.by("id")))
                .map(this::response);
    }

    public List<UsuarioResponse.RolResponse> roles() {
        return roles.findAll(Sort.by("nombre")).stream()
                .map(r -> new UsuarioResponse.RolResponse(r.getId(), r.getNombre()))
                .toList();
    }

    public UsuarioResponse detail(Long id) {
        return response(
                usuarios.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Usuario inexistente")));
    }

    @Transactional
    public UsuarioResponse update(Long id, UsuarioUpdateRequest request, String actor, String ip) {
        Usuario user = locked(id);
        String email = request.email().trim();
        String name = request.fullname().trim();
        if (name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        if (usuarios.existsByEmailIgnoreCaseAndIdNot(email, id))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El correo ya pertenece a otra cuenta");
        if (!name.equals(user.getFullname()) || !email.equals(user.getEmail())) {
            user.setFullname(name);
            user.setEmail(email);
            user.setUpdatedBy(actor);
            usuarios.saveAndFlush(user);
            audit("USUARIO_ACTUALIZADO", actor, id, ip);
        }
        return response(user);
    }

    @Transactional
    public UsuarioResponse status(Long id, boolean enable, String actor, String ip) {
        Usuario user = locked(id);
        if (!enable && user.getUsername().equals(actor))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "No puedes desactivar tu propia cuenta");
        if (!Boolean.valueOf(enable).equals(user.getEnable())) {
            user.setEnable(enable);
            user.setUpdatedBy(actor);
            usuarios.saveAndFlush(user);
            if (!enable)
                jdbc.update(
                        "UPDATE sesion SET fecha_cierre=? WHERE usuario_id=? AND fecha_cierre IS"
                            + " NULL",
                        LocalDateTime.now(),
                        id);
            audit(enable ? "USUARIO_ACTIVADO" : "USUARIO_DESACTIVADO", actor, id, ip);
        }
        return response(user);
    }

    private Usuario locked(Long id) {
        return usuarios.findForAdministration(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario inexistente"));
    }

    private void audit(String action, String actor, Long id, String ip) {
        jdbc.update(
                "INSERT INTO bitacora_logs (accion, fecha_hora, usuario_responsable, entidad,"
                    + " entidad_id, ip_origen) VALUES (?, ?, ?, 'usuarios', ?, ?)",
                action,
                LocalDateTime.now(),
                actor,
                id,
                ip);
    }

    private UsuarioResponse response(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFullname(),
                u.getEnable(),
                u.getRoles().stream()
                        .sorted(java.util.Comparator.comparing(r -> r.getNombre()))
                        .map(r -> new UsuarioResponse.RolResponse(r.getId(), r.getNombre()))
                        .toList(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}
