package comun.exception;

import jakarta.servlet.http.HttpServletRequest;

import modulo.seguridad_y_auditoria.acceso_al_sistema.exception.AccountLockedException;
import modulo.seguridad_y_auditoria.acceso_al_sistema.exception.InvalidCredentialsException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return build(
                HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
            AccountLockedException ex, HttpServletRequest request) {
        return build(
                HttpStatus.LOCKED,
                "ACCOUNT_LOCKED",
                ex.getMessage(),
                request,
                ex.getBloqueadoHasta());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(FieldError::getDefaultMessage)
                        .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request, null);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> forbidden(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Operacion no autorizada", request, null);
    }

    @ExceptionHandler({
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        jakarta.validation.ConstraintViolationException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> badRequest(Exception ex, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Solicitud invalida o datos protegidos",
                request,
                null);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> conflict(Exception ex, HttpServletRequest request) {
        return build(
                HttpStatus.CONFLICT,
                "CONFLICT",
                "Los datos entran en conflicto con otra cuenta",
                request,
                null);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> business(
            org.springframework.web.server.ResponseStatusException ex, HttpServletRequest request) {
        return build(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                "OPERATION_REJECTED",
                ex.getReason(),
                request,
                null);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request,
            LocalDateTime bloqueadoHasta) {
        ErrorResponse body =
                new ErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        error,
                        message,
                        request.getRequestURI(),
                        bloqueadoHasta);
        return ResponseEntity.status(status).body(body);
    }
}
