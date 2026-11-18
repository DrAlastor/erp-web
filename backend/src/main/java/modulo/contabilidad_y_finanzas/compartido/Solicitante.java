package modulo.contabilidad_y_finanzas.compartido;

/**
 * Quién pide una operación contable: el usuario, el nombre que queda en la bitácora y la IP de origen.
 */
public record Solicitante(Long usuarioId, String actor, String ip) {

    public Solicitante {
        if (actor == null || actor.isBlank()) actor = "sistema";
    }
}
