package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.AbrirTurnoRequest;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.MovimientoResponse;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.OpcionesCajaResponse;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.RegistrarMovimientoRequest;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.TurnoResponse;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.MovimientoCajaService;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.TurnoCajaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU-34 / CU34 (+ apertura mínima CU32): turno de caja y sus movimientos.
 *
 * <p>Las funciones de caja se mapean a FACTURACION en el RBAC (ServicioAutorizacion):
 * CONSULTAR para lecturas, CREAR para abrir turnos y registrar movimientos. El arqueo, el
 * cierre y el historial viven en la HU-35 (cu35_arqueos_y_conciliaciones), misma base /api/caja.
 */
@RestController("movimientosCajaController")
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {

    private final TurnoCajaService turnoService;
    private final MovimientoCajaService movimientoService;

    @GetMapping("/opciones")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public OpcionesCajaResponse opciones() {
        return turnoService.opciones();
    }

    @GetMapping("/turno-actual")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public ResponseEntity<TurnoResponse> turnoActual(Authentication authentication) {
        return turnoService.turnoActual(authentication)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/turnos")
    @PreAuthorize("hasPermission('FACTURACION','CREAR')")
    public ResponseEntity<TurnoResponse> abrir(
            @Valid @RequestBody AbrirTurnoRequest body, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turnoService.abrir(body, authentication));
    }

    @GetMapping("/turnos/{id}/movimientos")
    @PreAuthorize("hasPermission('FACTURACION','CONSULTAR')")
    public List<MovimientoResponse> movimientos(@PathVariable Long id, Authentication authentication) {
        return movimientoService.listar(id, authentication);
    }

    @PostMapping("/turnos/{id}/movimientos")
    @PreAuthorize("hasPermission('FACTURACION','CREAR')")
    public ResponseEntity<MovimientoResponse> registrarMovimiento(
            @PathVariable Long id, @Valid @RequestBody RegistrarMovimientoRequest body,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimientoService.registrar(id, body, authentication));
    }
}
