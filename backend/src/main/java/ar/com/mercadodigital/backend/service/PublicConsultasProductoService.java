package ar.com.mercadodigital.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.PublicDisponibilidadRequest;
import ar.com.mercadodigital.backend.dto.request.PublicProductoPreguntaRequest;
import ar.com.mercadodigital.backend.dto.response.ConsultaDisponibilidadResponse;
import ar.com.mercadodigital.backend.dto.response.ProductoPreguntaResponse;
import ar.com.mercadodigital.backend.entity.ConsultaDisponibilidad;
import ar.com.mercadodigital.backend.entity.ProductoPregunta;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ConsultaDisponibilidadRepository;
import ar.com.mercadodigital.backend.repository.ProductoPreguntaRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicConsultasProductoService {

    private final RevendedorProductoRepository productoRepository;
    private final ProductoPreguntaRepository preguntaRepository;
    private final ConsultaDisponibilidadRepository disponibilidadRepository;
    private final RevendedorConsultasService mapper;
    private final NotificacionConsultaEmailService emailService;

    @Transactional(readOnly = true)
    public List<ProductoPreguntaResponse> listarPreguntas(UUID productoId, UUID revendedorId) {
        RevendedorProducto producto = getProductoVisible(productoId, revendedorId);
        return preguntaRepository.findByRevendedorProducto_IdAndEstadoOrderByFechaPreguntaDesc(
                        producto.getId(), "RESPONDIDA")
                .stream().map(mapper::toPreguntaResponse).toList();
    }

    @Transactional
    public ProductoPreguntaResponse crearPregunta(PublicProductoPreguntaRequest request) {
        RevendedorProducto producto = getProductoVisible(request.getProductoId(), request.getRevendedorId());
        ProductoPregunta pregunta = new ProductoPregunta();
        pregunta.setRevendedor(producto.getRevendedor());
        pregunta.setRevendedorProducto(producto);
        pregunta.setNombreContacto(normalizar(request.getNombreContacto()));
        pregunta.setEmailContacto(normalizar(request.getEmailContacto()));
        pregunta.setTelefonoContacto(normalizar(request.getTelefonoContacto()));
        pregunta.setPregunta(request.getPregunta().trim());
        pregunta.setEstado("PENDIENTE");
        pregunta.setFechaPregunta(LocalDateTime.now());
        ProductoPregunta guardada = preguntaRepository.save(pregunta);
        emailService.notificarNuevaPregunta(guardada);
        return mapper.toPreguntaResponse(guardada);
    }

    @Transactional
    public ConsultaDisponibilidadResponse crearDisponibilidad(PublicDisponibilidadRequest request) {
        RevendedorProducto producto = getProductoVisible(request.getProductoId(), request.getRevendedorId());
        ConsultaDisponibilidad consulta = new ConsultaDisponibilidad();
        consulta.setRevendedor(producto.getRevendedor());
        consulta.setRevendedorProducto(producto);
        consulta.setNombreContacto(normalizar(request.getNombreContacto()));
        consulta.setEmailContacto(normalizar(request.getEmailContacto()));
        consulta.setTelefonoContacto(normalizar(request.getTelefonoContacto()));
        consulta.setLocalidad(valorO(request.getLocalidad(), "Sin especificar"));
        consulta.setObservaciones(normalizar(request.getObservaciones()));
        consulta.setEstado("PENDIENTE");
        consulta.setCheckoutToken(UUID.randomUUID().toString());
        consulta.setAccionToken(UUID.randomUUID().toString());
        consulta.setAccionTokenExpiraAt(LocalDateTime.now().plusDays(7));
        consulta.setFechaConsulta(LocalDateTime.now());
        ConsultaDisponibilidad guardada = disponibilidadRepository.save(consulta);
        emailService.notificarNuevaDisponibilidad(guardada);
        return mapper.toDisponibilidadResponse(guardada);
    }

    @Transactional(readOnly = true)
    public ConsultaDisponibilidadResponse getDisponibilidad(String token) {
        ConsultaDisponibilidad consulta = disponibilidadRepository.findByCheckoutToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de disponibilidad no encontrada"));
        return mapper.toDisponibilidadResponse(consulta);
    }

    private RevendedorProducto getProductoVisible(UUID productoId, UUID revendedorId) {
        return productoRepository.findByIdAndRevendedor_Id(productoId, revendedorId)
                .filter(RevendedorProducto::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    private String normalizar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String valorO(String value, String fallback) {
        String normalized = normalizar(value);
        return normalized == null ? fallback : normalized;
    }
}
