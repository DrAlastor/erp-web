package modulo.seguridad_y_auditoria.gestion_de_usuarios.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioRequest;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioResponse;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioStatusRequest;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioUpdateRequest;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.service.UsuarioService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * CU-02: administración de las cuentas de usuario del ERP.
 *
 * <p>La protección es declarativa: cada operación declara el permiso que exige del módulo
 * SEGURIDAD y el 403 lo emite el framework. Los permisos no viajan en el token, se resuelven
 * contra la base en cada petición, así que un cambio de rol o de matriz hecho en la CU-03
 * surte efecto sin volver a iniciar sesión.
 *
 * <p>La empresa del actor no sale del cuerpo de la petición: la resuelve el servicio a
 * partir de la identidad autenticada, para que nadie pueda administrar cuentas de otro
 * inquilino.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Validated
public class UsuarioController {

    private final UsuarioService usuarioService;

    /** Crea una cuenta y devuelve 201 con la cabecera Location del recurso creado. */
    @PostMapping
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest cuerpo,
                                                 Principal actor,
                                                 HttpServletRequest peticion) {
        UsuarioResponse creado = usuarioService.create(cuerpo, actor.getName(), peticion.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/usuarios/" + creado.id())).body(creado);
    }

    /** Listado paginado, con búsqueda por nombre, usuario o correo y filtros por rol y estado. */
    @GetMapping
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR')")
    public Page<UsuarioResponse> listar(@RequestParam(defaultValue = "") @Size(max = 150) String search,
                                        @RequestParam(required = false) UUID role,
                                        @RequestParam(required = false) Boolean enable,
                                        @RequestParam(defaultValue = "0") @Min(0) int page,
                                        @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return usuarioService.list(search, role, enable, page, size);
    }

    /** Roles de la empresa, solo para poblar los filtros del listado. */
    @GetMapping("/roles")
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR')")
    public List<UsuarioResponse.RolResponse> roles() {
        return usuarioService.roles();
    }

    /** Detalle de una cuenta. */
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR')")
    public UsuarioResponse detalle(@PathVariable Long id) {
        return usuarioService.detail(id);
    }

    /** Edita el nombre completo y el correo. Los campos protegidos se rechazan con 400. */
    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
    public UsuarioResponse actualizar(@PathVariable Long id,
                                      @Valid @RequestBody UsuarioUpdateRequest cuerpo,
                                      Principal actor,
                                      HttpServletRequest peticion) {
        return usuarioService.update(id, cuerpo, actor.getName(), peticion.getRemoteAddr());
    }

    /** Activa o desactiva una cuenta. Desactivarla cierra sus sesiones de renovación. */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
    public UsuarioResponse cambiarEstado(@PathVariable Long id,
                                         @Valid @RequestBody UsuarioStatusRequest cuerpo,
                                         Principal actor,
                                         HttpServletRequest peticion) {
        return usuarioService.status(id, cuerpo.enable(), actor.getName(), peticion.getRemoteAddr());
    }
}
