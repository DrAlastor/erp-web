package modulo.seguridad_y_auditoria.cu05_empresas_sucursales_configuracion.dto;

import java.util.UUID;

public record EmpresaDTO(
        UUID id,
        String nombre,
        String razonSocial,
        String nit,
        String direccion,
        String telefono,
        String email,
        String monedaPrincipal,
        String monedaSecundaria,
        String logoUrl,
        boolean activo) {
}
