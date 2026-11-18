package modulo.tesoreria_y_cajas.cu32_apertura_de_caja.mapper;

import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.CierreCajaResponse;
import modulo.tesoreria_y_cajas.cu32_apertura_de_caja.dto.EstadoCajaResponse;
import modulo.ventas_y_facturacion.compartido.entity.FormaPago;
import modulo.ventas_y_facturacion.compartido.entity.TurnoCaja;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import org.springframework.stereotype.Component;

/** Traduce turnos, sucursales y formas de pago a los DTOs de la API de caja. */
@Component("aperturaCajaMapper")
public class CajaMapper {

    public EstadoCajaResponse.TurnoActivo toTurno(TurnoCaja t, String sucursalNombre) {
        return new EstadoCajaResponse.TurnoActivo(t.getId(), t.getSucursalId(), sucursalNombre, t.getCajeroId(),
                t.getMontoApertura(), t.getFechaApertura(), t.getEstado(), t.getObservaciones());
    }

    public EstadoCajaResponse.Sucursal toSucursal(Almacen s) {
        return new EstadoCajaResponse.Sucursal(s.getId(), s.getNombre());
    }

    public EstadoCajaResponse.FormaPago toFormaPago(FormaPago f) {
        return new EstadoCajaResponse.FormaPago(f.getId(), f.getCodigo(), f.getNombre());
    }

    public CierreCajaResponse toCierre(TurnoCaja t, EstadoCajaResponse.ResumenTurno resumen) {
        return new CierreCajaResponse(t.getId(), t.getEstado(), t.getMontoApertura(),
                resumen.montoEsperado(), t.getMontoCierre(), t.getDiferencia(), t.getFechaCierre());
    }
}
