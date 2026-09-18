package modulo.seguridad_y_auditoria.roles_y_permisos.exception;

/**
 * Lo que se pidió no existe, o no existe <em>para esta empresa</em>.
 *
 * <p>Pedir un rol de otra empresa devuelve esto y no un 403: así no se filtra si el
 * identificador existe en algún otro inquilino.
 */
public class RecursoNoEncontrado extends RuntimeException {

    private final String codigo;

    public RecursoNoEncontrado(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
