package modulo.seguridad_y_auditoria.roles_y_permisos.exception;

/**
 * La operación es válida como petición pero contradice una regla del negocio: por ejemplo
 * asignar dos veces el mismo rol, o dejar al rol Administrador sin los permisos de
 * seguridad.
 *
 * <p>Existe para que estas situaciones lleguen al cliente como un error nombrado y no como
 * un 500.
 */
public class ReglaDeNegocio extends RuntimeException {

    private final String codigo;

    public ReglaDeNegocio(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
