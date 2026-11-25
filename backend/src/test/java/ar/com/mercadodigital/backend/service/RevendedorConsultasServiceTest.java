package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.request.ResponderDisponibilidadRequest;
import ar.com.mercadodigital.backend.dto.request.ResponderProductoPreguntaRequest;
import ar.com.mercadodigital.backend.entity.ConsultaDisponibilidad;
import ar.com.mercadodigital.backend.entity.ProductoPregunta;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.repository.ConsultaDisponibilidadRepository;
import ar.com.mercadodigital.backend.repository.ProductoPreguntaRepository;

@ExtendWith(MockitoExtension.class)
class RevendedorConsultasServiceTest {

    @Mock RevendedorOwnershipService ownershipService;
    @Mock ProductoPreguntaRepository preguntaRepository;
    @Mock ConsultaDisponibilidadRepository disponibilidadRepository;
    @Mock NotificacionConsultaEmailService emailService;
    @InjectMocks RevendedorConsultasService service;

    @Test
    void responderPreguntaValidaTenantYMarcaRespondida() {
        UUID revendedorId = UUID.randomUUID();
        UUID preguntaId = UUID.randomUUID();
        ProductoPregunta pregunta = pregunta(preguntaId);
        when(preguntaRepository.findByIdAndRevendedor_IdAndRevendedorProductoIsNotNull(preguntaId, revendedorId))
                .thenReturn(Optional.of(pregunta));
        when(preguntaRepository.save(pregunta)).thenReturn(pregunta);
        ResponderProductoPreguntaRequest request = new ResponderProductoPreguntaRequest();
        request.setRespuesta("  Sí, está disponible.  ");

        var response = service.responderPregunta(revendedorId, preguntaId, request);

        verify(ownershipService).validarOwnership(revendedorId);
        assertEquals("RESPONDIDA", response.getEstado());
        assertEquals("Sí, está disponible.", response.getRespuesta());
    }

    @Test
    void responderDisponibilidadValidaTenantYRespetaResultado() {
        UUID revendedorId = UUID.randomUUID();
        UUID consultaId = UUID.randomUUID();
        ConsultaDisponibilidad consulta = disponibilidad(consultaId);
        when(disponibilidadRepository.findByIdAndRevendedor_IdAndRevendedorProductoIsNotNull(consultaId, revendedorId))
                .thenReturn(Optional.of(consulta));
        when(disponibilidadRepository.save(consulta)).thenReturn(consulta);
        ResponderDisponibilidadRequest request = new ResponderDisponibilidadRequest();
        request.setDisponible(false);
        request.setRespuestaProveedor("Sin stock esta semana");

        var response = service.responderDisponibilidad(revendedorId, consultaId, request);

        verify(ownershipService).validarOwnership(revendedorId);
        assertEquals("RECHAZADA", response.getEstado());
        assertEquals("Sin stock esta semana", response.getRespuestaProveedor());
    }

    private ProductoPregunta pregunta(UUID id) {
        ProductoPregunta pregunta = new ProductoPregunta();
        pregunta.setId(id);
        pregunta.setPregunta("¿Hay stock?");
        pregunta.setEstado("PENDIENTE");
        pregunta.setRevendedorProducto(producto());
        return pregunta;
    }

    private ConsultaDisponibilidad disponibilidad(UUID id) {
        ConsultaDisponibilidad consulta = new ConsultaDisponibilidad();
        consulta.setId(id);
        consulta.setEstado("PENDIENTE");
        consulta.setLocalidad("Reconquista");
        consulta.setRevendedorProducto(producto());
        return consulta;
    }

    private RevendedorProducto producto() {
        RevendedorProducto producto = new RevendedorProducto();
        producto.setId(UUID.randomUUID());
        producto.setNombre("Producto propio");
        return producto;
    }
}
