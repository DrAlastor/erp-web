package modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto;

import java.time.LocalDateTime;

/** Ficha completa del proveedor. */
public record ProveedorResponse(
        Integer id,
        String razonSocial,
        String nitCi,
        String contacto,
        String telefono,
        String email,
        String direccion,
        String ciudad,
        String condicionPago,
        Integer diasCredito,
        boolean activo,
        LocalDateTime fechaCreacion) {
}
