package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminRevendedorResponse {
    private UUID id;
    private String nombre;
    private String dominio;
    private boolean activo;
    private String usuarioEmail;
    private boolean usuarioConfigurado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AdminStorefrontResponse storefront;
    private AdminRevendedorMercadoPagoResponse mercadoPago;
}
