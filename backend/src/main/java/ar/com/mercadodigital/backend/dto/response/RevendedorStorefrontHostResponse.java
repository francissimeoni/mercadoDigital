package ar.com.mercadodigital.backend.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RevendedorStorefrontHostResponse {
    String revendedorId;
    String storefrontHost;
}
