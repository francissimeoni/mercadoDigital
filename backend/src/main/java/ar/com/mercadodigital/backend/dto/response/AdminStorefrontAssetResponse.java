package ar.com.mercadodigital.backend.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStorefrontAssetResponse {
    private String url;
    private String filename;
    private String contentType;
    private long size;
}
