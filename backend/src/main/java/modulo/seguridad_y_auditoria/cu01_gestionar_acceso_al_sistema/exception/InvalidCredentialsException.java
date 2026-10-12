package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
