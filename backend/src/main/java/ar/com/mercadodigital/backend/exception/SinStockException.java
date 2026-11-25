package ar.com.mercadodigital.backend.exception;

public class SinStockException extends RuntimeException {

    public SinStockException(String message) {
        super(message);
    }
}
