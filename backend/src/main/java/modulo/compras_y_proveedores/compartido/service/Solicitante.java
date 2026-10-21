package modulo.compras_y_proveedores.compartido.service;

/**
 * Quién pide la operación: el usuario (para reglas como "quien crea no aprueba"), el nombre
 * que queda en la bitácora y la IP de origen.
 */
public record Solicitante(Long usuarioId, String actor, String ip) {

    public Solicitante {
        if (actor == null || actor.isBlank()) actor = "sistema";
    }
}
