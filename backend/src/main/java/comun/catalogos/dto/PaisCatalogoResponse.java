package comun.catalogos.dto;

import java.util.List;

public record PaisCatalogoResponse(
        String codigoIso2,
        String codigoIso3,
        String nombre,
        List<MonedaCatalogoResponse> monedas) {
}
