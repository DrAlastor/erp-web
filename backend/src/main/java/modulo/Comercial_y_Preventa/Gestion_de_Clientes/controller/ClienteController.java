package modulo.Comercial_y_Preventa.Gestion_de_Clientes.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.dto.ClienteRequest;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.dto.ClienteResponse;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.dto.ClienteStatusRequest;
import modulo.Comercial_y_Preventa.Gestion_de_Clientes.service.ClienteService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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


@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Validated
public class ClienteController {

    private final ClienteService service;

    @GetMapping
    @PreAuthorize("hasAuthority('COMERCIAL:CLIENTES:LECTURA')")
    public Page<ClienteResponse> list(
            @RequestParam(defaultValue = "") @Size(max = 150) String search,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "15") @Min(1) @Max(100) int size) {
        return service.list(search, activo, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMERCIAL:CLIENTES:LECTURA')")
    public ClienteResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMERCIAL:CLIENTES:ESCRITURA')")
    public ResponseEntity<ClienteResponse> create(@Valid @RequestBody ClienteRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMERCIAL:CLIENTES:ESCRITURA')")
    public ClienteResponse update(
            @PathVariable Long id, @Valid @RequestBody ClienteRequest body) {
        return service.update(id, body);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('COMERCIAL:CLIENTES:ESCRITURA')")
    public ClienteResponse status(
            @PathVariable Long id, @Valid @RequestBody ClienteStatusRequest body) {
        return service.status(id, body.activo());
    }
}
