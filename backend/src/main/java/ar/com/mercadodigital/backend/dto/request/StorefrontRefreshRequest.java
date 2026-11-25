package ar.com.mercadodigital.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StorefrontRefreshRequest {

    @NotBlank
    @Size(max = 255)
    private String storefrontHost;

    @NotBlank
    @Size(max = 4096)
    private String refreshToken;
}
