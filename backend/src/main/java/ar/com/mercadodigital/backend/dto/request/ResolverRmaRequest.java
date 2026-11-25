package ar.com.mercadodigital.backend.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolverRmaRequest {

    @NotBlank
    private String resolucionFinal;

    private String observacionesInspeccion;

    private String responsableProbable;

    private BigDecimal montoCompensacion;

    private String referenciaCompensacion;

    private List<org.springframework.web.multipart.MultipartFile> archivos;
}
