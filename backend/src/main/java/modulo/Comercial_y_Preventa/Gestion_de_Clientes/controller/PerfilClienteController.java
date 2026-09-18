package modulo.Comercial_y_Preventa.Gestion_de_Clientes.controller;

import lombok.RequiredArgsConstructor;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.dto.ClienteResponse;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.service.ClienteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

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
