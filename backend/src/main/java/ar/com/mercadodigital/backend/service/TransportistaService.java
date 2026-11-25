package ar.com.mercadodigital.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.dto.request.CrearTransportistaRequest;
import ar.com.mercadodigital.backend.dto.response.TransportistaResponse;
import ar.com.mercadodigital.backend.entity.Transportista;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.repository.TransportistaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransportistaService {

    private final TransportistaRepository transportistaRepository;

    @Transactional(readOnly = true)
    public List<TransportistaResponse> getActivos() {
        return transportistaRepository.findAll().stream()
                .filter(transportista -> transportista.isActivo())
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TransportistaResponse crear(CrearTransportistaRequest request) {
        String nombreNormalizado = normalizarNombre(request.getNombre());
        transportistaRepository.findByNombreIgnoreCase(nombreNormalizado)
                .ifPresent(transportista -> {
                    throw new OperacionEnConflictoException("Ya existe un transportista con nombre: " + nombreNormalizado);
                });

        return toResponse(transportistaRepository.save(crearNuevo(nombreNormalizado)));
    }

    @Transactional
    public Transportista resolveOrCreate(UUID transportistaId, String transportistaNombre) {
        if (transportistaId != null) {
            return transportistaRepository.findById(transportistaId)
                    .orElseThrow(() -> new ar.com.mercadodigital.backend.exception.ResourceNotFoundException(
                            "Transportista no encontrado: " + transportistaId));
        }

        String nombreNormalizado = normalizarNombre(transportistaNombre);
        return transportistaRepository.findByNombreIgnoreCase(nombreNormalizado)
                .orElseGet(() -> transportistaRepository.save(crearNuevo(nombreNormalizado)));
    }

    private String normalizarNombre(String nombre) {
        if (!StringUtils.hasText(nombre)) {
            throw new BadRequestException("Debes indicar un transportista valido.");
        }
        return nombre.trim();
    }

    private Transportista crearNuevo(String nombreNormalizado) {
        Transportista transportista = new Transportista();
        transportista.setId(UUID.randomUUID());
        transportista.setNombre(nombreNormalizado);
        transportista.setActivo(true);
        return transportista;
    }

    private TransportistaResponse toResponse(Transportista transportista) {
        return TransportistaResponse.builder()
                .id(transportista.getId())
                .nombre(transportista.getNombre())
                .build();
    }
}
