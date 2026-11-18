package modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto;

/** Cuenta de movimiento elegible en una línea del asiento. */
public record CuentaOpcionResponse(Long id, String codigo, String nombre, String tipo, String naturaleza) {}
