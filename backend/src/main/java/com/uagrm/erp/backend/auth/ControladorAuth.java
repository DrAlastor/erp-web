package com.uagrm.erp.backend.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Inicio de sesión. Provisional: la CU-01 reemplaza este paquete.
 *
 * <p>Es el único endpoint abierto sin token, junto con el de salud.
 */
@RestController
@RequestMapping("/api/auth")
public class ControladorAuth {

    private final ServicioAutenticacion servicioAutenticacion;
    private final ServicioJwt servicioJwt;

    public ControladorAuth(ServicioAutenticacion servicioAutenticacion, ServicioJwt servicioJwt) {
        this.servicioAutenticacion = servicioAutenticacion;
        this.servicioJwt = servicioJwt;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody PeticionLogin peticion) {
        return servicioAutenticacion.autenticar(peticion.email(), peticion.password())
                .<ResponseEntity<Object>>map(principal -> ResponseEntity.ok(new RespuestaLogin(
                        servicioJwt.generar(principal),
                        principal.nombre(),
                        principal.empresaId(),
                        servicioJwt.minutosDeVigencia())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error", "CREDENCIALES_INVALIDAS",
                        "mensaje", "El email o la contraseña no son correctos")));
    }
}
