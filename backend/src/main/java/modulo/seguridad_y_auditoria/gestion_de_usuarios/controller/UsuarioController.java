package modulo.seguridad_y_auditoria.gestion_de_usuarios.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.*;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.service.UsuarioService;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Validated
@PreAuthorize("@usuariosAuthorization.canManage(authentication)")
public class UsuarioController {
    private final UsuarioService service;

    @GetMapping
    public Page<UsuarioResponse> list(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(required = false) java.util.UUID role,
            @RequestParam(required = false) Boolean enable,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.list(search, role, enable, page, size);
    }

    @GetMapping("/roles")
    public List<UsuarioResponse.RolResponse> roles() {
        return service.roles();
    }

    @GetMapping("/{id}")
    public UsuarioResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PutMapping("/{id}")
    public UsuarioResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequest body,
            Principal actor,
            HttpServletRequest request) {
        return service.update(id, body, actor.getName(), request.getRemoteAddr());
    }

    @PatchMapping("/{id}/status")
    public UsuarioResponse status(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioStatusRequest body,
            Principal actor,
            HttpServletRequest request) {
        return service.status(id, body.enable(), actor.getName(), request.getRemoteAddr());
    }
}
