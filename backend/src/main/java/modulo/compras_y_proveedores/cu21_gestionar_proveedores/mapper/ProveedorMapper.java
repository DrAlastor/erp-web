package modulo.compras_y_proveedores.cu21_gestionar_proveedores.mapper;

import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.HistorialProveedorResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorOpcionResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.Proveedor;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

@Component
public class ProveedorMapper {

    public ProveedorResponse aRespuesta(Proveedor p) {
        return new ProveedorResponse(
                p.getId(),
                p.getRazonSocial(),
                p.getNitCi(),
                p.getContacto(),
                p.getTelefono(),
                p.getEmail(),
                p.getDireccion(),
                p.getCiudad(),
                p.getCondicionPago() == null ? null : p.getCondicionPago().name(),
                p.getDiasCredito(),
                Boolean.TRUE.equals(p.getActivo()),
                p.getFechaCreacion());
    }

    public ProveedorOpcionResponse aOpcion(Proveedor p) {
        return new ProveedorOpcionResponse(
                p.getId(),
                p.getRazonSocial(),
                p.getNitCi(),
                p.getCondicionPago() == null ? null : p.getCondicionPago().name(),
                p.getDiasCredito());
    }

    /** Convierte una fila nativa (id, número, fecha, total, estado) en un documento del historial. */
    public HistorialProveedorResponse.Documento aDocumento(Object[] fila) {
        return new HistorialProveedorResponse.Documento(
                ((Number) fila[0]).intValue(),
                (String) fila[1],
                aFecha(fila[2]),
                fila[3] == null ? BigDecimal.ZERO : new BigDecimal(fila[3].toString()),
                (String) fila[4]);
    }

    private static LocalDate aFecha(Object valor) {
        if (valor == null) return null;
        if (valor instanceof LocalDate fecha) return fecha;
        if (valor instanceof Date fecha) return fecha.toLocalDate();
        return LocalDate.parse(valor.toString().substring(0, 10));
    }
}
