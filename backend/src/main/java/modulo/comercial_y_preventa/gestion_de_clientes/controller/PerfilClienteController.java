package modulo.comercial_y_preventa.gestion_de_clientes.controller;

import lombok.RequiredArgsConstructor;
import modulo.comercial_y_preventa.gestion_de_clientes.dto.ClienteResponse;
import modulo.comercial_y_preventa.gestion_de_clientes.service.ClienteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Portal del cliente: el cliente externo consulta su propio perfil.
 *
 * <p>Usa el permiso legacy CLIENTE:PERFIL:LECTURA porque el cliente no es un empleado de la
 * empresa y no forma parte de la matriz de 7 roles del catalogo de la CU03. Queda pendiente
 * decidir si el portal entra al catalogo como un modulo propio.
 */
@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilClienteController {

    private final ClienteService service;

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENTE:PERFIL:LECTURA')")
    public ClienteResponse current(Principal principal) {
        return service.current(principal.getName());
    }
}
