package modulo.comercial_y_preventa.gestion_de_clientes.dto;

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
