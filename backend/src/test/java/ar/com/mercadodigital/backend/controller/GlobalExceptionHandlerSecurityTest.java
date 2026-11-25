package ar.com.mercadodigital.backend.controller;

import ar.com.mercadodigital.backend.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerSecurityTest {
    @RestController static class RejectedOperations {
        @GetMapping("/denied") String denied() { throw new AccessDeniedException("private resource details"); }
        @GetMapping("/oversized") String oversized() { throw new MaxUploadSizeExceededException(5242880); }
        @GetMapping("/unexpected") String unexpected() { throw new IllegalStateException("SQL and private storage path"); }
    }
    @Test void ownershipRejectionIsForbiddenWithoutInternalDetails() throws Exception {
        MockMvcBuilders.standaloneSetup(new RejectedOperations()).setControllerAdvice(new GlobalExceptionHandler())
                .build().perform(get("/denied")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403)).andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.message").value("No tenés permiso para realizar esta operación."));
    }
    @Test void multipartLimitHasRecoverableClientError() throws Exception {
        MockMvcBuilders.standaloneSetup(new RejectedOperations()).setControllerAdvice(new GlobalExceptionHandler())
                .build().perform(get("/oversized")).andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }
    @Test void unexpectedFailureDoesNotExposeInfrastructureDetails() throws Exception {
        MockMvcBuilders.standaloneSetup(new RejectedOperations()).setControllerAdvice(new GlobalExceptionHandler())
                .build().perform(get("/unexpected")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("No se pudo completar la operación. Volvé a intentarlo."))
                .andExpect(jsonPath("$.details").isEmpty());
    }
}
