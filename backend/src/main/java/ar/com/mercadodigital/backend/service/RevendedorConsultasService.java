package ar.com.mercadodigital.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.ResponderDisponibilidadRequest;
import ar.com.mercadodigital.backend.dto.request.ResponderProductoPreguntaRequest;
import ar.com.mercadodigital.backend.dto.response.ConsultaDisponibilidadResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoPreguntaResponse;
import ar.com.mercadodigital.backend.entity.ConsultaDisponibilidad;
import ar.com.mercadodigital.backend.entity.ProductoPregunta;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ConsultaDisponibilidadRepository;
import ar.com.mercadodigital.backend.repository.ProductoPreguntaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RevendedorConsultasService {

    private final RevendedorOwnershipService ownershipService;
    private final ProductoPreguntaRepository preguntaRepository;
    private final ConsultaDisponibilidadRepository disponibilidadRepository;
    private final NotificacionConsultaEmailService emailService;

    @Transactional(readOnly = true)
    public List<ProductoPreguntaResponse> listarPreguntas(UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return preguntaRepository
                .findByRevendedor_IdAndRevendedorProductoIsNotNullOrderByFechaPreguntaDesc(revendedorId)
                .stream().map(this::toPreguntaResponse).toList();
    }

    @Transactional
    public ProductoPreguntaResponse responderPregunta(
            UUID revendedorId, UUID preguntaId, ResponderProductoPreguntaRequest request) {
        ownershipService.validarOwnership(revendedorId);
        ProductoPregunta pregunta = preguntaRepository
                .findByIdAndRevendedor_IdAndRevendedorProductoIsNotNull(preguntaId, revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta no encontrada: " + preguntaId));
        pregunta.setRespuesta(request.getRespuesta().trim());
        pregunta.setEstado("RESPONDIDA");
        pregunta.setFechaRespuesta(LocalDateTime.now());
        ProductoPregunta guardada = preguntaRepository.save(pregunta);
        emailService.notificarRespuestaPregunta(guardada);
        return toPreguntaResponse(guardada);
    }

    @Transactional(readOnly = true)
    public List<ConsultaDisponibilidadResponse> listarDisponibilidad(UUID revendedorId) {
        ownershipService.validarOwnership(revendedorId);
        return disponibilidadRepository
                .findByRevendedor_IdAndRevendedorProductoIsNotNullOrderByFechaConsultaDesc(revendedorId)
                .stream().map(this::toDisponibilidadResponse).toList();
    }

    @Transactional
    public ConsultaDisponibilidadResponse responderDisponibilidad(
            UUID revendedorId, UUID consultaId, ResponderDisponibilidadRequest request) {
        ownershipService.validarOwnership(revendedorId);
        ConsultaDisponibilidad consulta = disponibilidadRepository
                .findByIdAndRevendedor_IdAndRevendedorProductoIsNotNull(consultaId, revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de disponibilidad no encontrada: " + consultaId));
        consulta.setEstado(Boolean.TRUE.equals(request.getDisponible()) ? "CONFIRMADA" : "RECHAZADA");
        consulta.setRespuestaProveedor(normalizarOpcional(request.getRespuestaProveedor()));
        consulta.setFechaRespuesta(LocalDateTime.now());
        ConsultaDisponibilidad guardada = disponibilidadRepository.save(consulta);
        emailService.notificarRespuestaDisponibilidad(guardada);
        return toDisponibilidadResponse(guardada);
    }

    public ProductoPreguntaResponse toPreguntaResponse(ProductoPregunta pregunta) {
        return ProductoPreguntaResponse.builder()
                .id(pregunta.getId())
                .productoId(pregunta.getRevendedorProducto().getId())
                .productoNombre(pregunta.getRevendedorProducto().getNombre())
                .nombreContacto(pregunta.getNombreContacto())
                .emailContacto(pregunta.getEmailContacto())
                .telefonoContacto(pregunta.getTelefonoContacto())
                .pregunta(pregunta.getPregunta())
                .respuesta(pregunta.getRespuesta())
                .estado(pregunta.getEstado())
                .fechaPregunta(pregunta.getFechaPregunta())
                .fechaRespuesta(pregunta.getFechaRespuesta())
                .build();
    }

    public ConsultaDisponibilidadResponse toDisponibilidadResponse(ConsultaDisponibilidad consulta) {
        return ConsultaDisponibilidadResponse.builder()
                .id(consulta.getId())
                .productoId(consulta.getRevendedorProducto().getId())
                .productoNombre(consulta.getRevendedorProducto().getNombre())
                .nombreContacto(consulta.getNombreContacto())
                .emailContacto(consulta.getEmailContacto())
                .telefonoContacto(consulta.getTelefonoContacto())
                .localidad(consulta.getLocalidad())
                .observaciones(consulta.getObservaciones())
                .estado(consulta.getEstado())
                .respuestaProveedor(consulta.getRespuestaProveedor())
                .checkoutToken(consulta.getCheckoutToken())
                .fechaConsulta(consulta.getFechaConsulta())
                .fechaRespuesta(consulta.getFechaRespuesta())
                .build();
    }

    private String normalizarOpcional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
