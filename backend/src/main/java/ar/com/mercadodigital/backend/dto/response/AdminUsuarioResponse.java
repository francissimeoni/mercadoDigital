package ar.com.mercadodigital.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ar.com.mercadodigital.backend.entity.TipoUsuario;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUsuarioResponse {
    private UUID id;
    private String nombre;
    private String email;
    private TipoUsuario tipoUsuario;
    private List<String> roles;
    private boolean activo;
    private UUID proveedorId;
    private Boolean proveedorDespachante;
    private UUID revendedorId;
    private AdminRevendedorMercadoPagoResponse mercadoPago;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
