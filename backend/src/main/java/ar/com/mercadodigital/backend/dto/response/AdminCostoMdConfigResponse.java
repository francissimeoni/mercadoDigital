package ar.com.mercadodigital.backend.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminCostoMdConfigResponse {

    private List<AdminCostoMdCategoriaResponse> items;
}
