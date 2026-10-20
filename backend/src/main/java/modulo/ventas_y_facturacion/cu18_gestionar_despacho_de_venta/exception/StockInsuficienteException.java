package modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.exception;

import modulo.ventas_y_facturacion.cu18_gestionar_despacho_de_venta.dto.FaltanteStock;

import java.util.List;

/**
 * No hay stock en la sucursal para todo lo que se quiere despachar. Lleva la lista de
 * faltantes para que el almacén sepa exactamente qué le falta, y no se despacha nada.
 */
public class StockInsuficienteException extends RuntimeException {

    private final transient List<FaltanteStock> faltantes;

    public StockInsuficienteException(List<FaltanteStock> faltantes) {
        super("Stock insuficiente en la sucursal para " + faltantes.size() + " producto(s).");
        this.faltantes = List.copyOf(faltantes);
    }

    public List<FaltanteStock> getFaltantes() {
        return faltantes;
    }
}
