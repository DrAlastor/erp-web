package modulo.tesoreria_y_cajas.cu35_arqueos_y_conciliaciones.dto;

import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.MovimientoResponse;
import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.dto.TurnoResponse;

import java.util.List;

/** Detalle de un turno para la consulta: datos, arqueo y movimientos (más reciente primero). */
public record DetalleTurnoResponse(TurnoResponse turno, ArqueoResponse arqueo, List<MovimientoResponse> movimientos) {}
