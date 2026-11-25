package ar.com.mercadodigital.backend.exception;

public class IntegracionPspException extends RuntimeException {

    public IntegracionPspException(String message) {
        super(message);
    }

    public IntegracionPspException(String message, Throwable cause) {
        super(message, cause);
    }
}
