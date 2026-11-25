package ar.com.mercadodigital.backend.exception;

public class OperacionEnConflictoException extends RuntimeException {

    public OperacionEnConflictoException(String message) {
        super(message);
    }
}
