package modulo.ventas_y_facturacion.cu13_gestionar_clientes.mapper;

import modulo.ventas_y_facturacion.cu13_gestionar_clientes.dto.ClienteResponse;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Traduce la entidad {@link Cliente} al DTO que expone la API.
 */
@Component
public class ClienteMapper {

    public ClienteResponse toResponse(Cliente cliente) {
        if (cliente == null) return null;
        Usuario usuario = cliente.getUsuario();
        return new ClienteResponse(
                cliente.getId(),
                cliente.getRazonSocial(),
                cliente.getNitCi(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getDireccion(),
                cliente.getCiudad(),
                cliente.getCondicionPago() != null ? cliente.getCondicionPago() : "CONTADO",
                cliente.getLimiteCredito() != null ? cliente.getLimiteCredito() : BigDecimal.ZERO,
                cliente.getDiasCredito() != null ? cliente.getDiasCredito() : 0,
                cliente.getActivo(),
                cliente.getFechaCreacion(),
                usuario == null ? null : usuario.getId(),
                usuario == null ? null : usuario.getUsername());
    }
}
