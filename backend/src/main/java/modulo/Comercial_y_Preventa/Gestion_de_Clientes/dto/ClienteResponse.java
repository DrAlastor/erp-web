package modulo.Comercial_y_Preventa.Gestion_de_Clientes.dto;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String razonSocial,
        String nitCi,
        String telefono,
        String direccion,
        Boolean activo,
        LocalDateTime fechaCreacion,
        Long usuarioId,
        String username,
        String email) {}
