package modulo.ventas_y_facturacion.cu13_gestionar_clientes.mapper;

import modulo.ventas_y_facturacion.cu13_gestionar_clientes.dto.ClienteResponse;
import modulo.ventas_y_facturacion.cu13_gestionar_clientes.entity.Cliente;

import modulo.seguridad_y_auditoria.compartido.entity.Usuario;

import org.springframework.stereotype.Component;

/**
 * Traduce la entidad {@link Cliente} al DTO que expone la API.
 *
 * <p>Del usuario vinculado solo salen identificador, nombre de cuenta y correo: la pantalla
 * del directorio no necesita nada más y así no se filtran datos de la cuenta.
 */
@Component
public class ClienteMapper {

    public ClienteResponse toResponse(Cliente cliente) {
        Usuario usuario = cliente.getUsuario();
        return new ClienteResponse(
                cliente.getId(),
                cliente.getRazonSocial(),
                cliente.getNitCi(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getActivo(),
                cliente.getFechaCreacion(),
                usuario == null ? null : usuario.getId(),
                usuario == null ? null : usuario.getUsername(),
                usuario == null ? null : usuario.getEmail());
    }
}
