package modulo.ventas_y_facturacion.cu13_gestionar_clientes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String razonSocial,
        String nitCi,
        String telefono,
        String email,
        String direccion,
        String ciudad,
        String condicionPago,
        BigDecimal limiteCredito,
        Integer diasCredito,
        Boolean activo,
        LocalDateTime fechaCreacion,
        Long usuarioId,
        String username) {}
