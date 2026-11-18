package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto;

import java.util.List;

/** Una cuenta del árbol del plan de cuentas con sus subcuentas ordenadas por código. */
public record CuentaNodoResponse(CuentaResponse cuenta, List<CuentaNodoResponse> hijos) {
}
