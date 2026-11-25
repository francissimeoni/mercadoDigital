package ar.com.mercadodigital.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import ar.com.mercadodigital.backend.exception.ApiErrorResponse;

/** Mantiene errores de permisos/concurrencia del nuevo módulo en sus estados HTTP correctos. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {CatalogoAdminController.class, CatalogoFichaController.class})
public class CatalogoAdminExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> forbidden(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "No tenés permiso para operar esta tienda.");
    }
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiErrorResponse> conflict(Exception ex) {
        return error(HttpStatus.CONFLICT, "La configuración cambió o el registro está en uso. Recargá y revisá los datos.");
    }
    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiErrorResponse.builder().timestamp(LocalDateTime.now())
                .status(status.value()).error(status.getReasonPhrase()).message(message).details(List.of()).build());
    }
}
