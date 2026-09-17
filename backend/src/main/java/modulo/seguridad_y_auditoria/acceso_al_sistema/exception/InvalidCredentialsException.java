package modulo.seguridad_y_auditoria.acceso_al_sistema.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
