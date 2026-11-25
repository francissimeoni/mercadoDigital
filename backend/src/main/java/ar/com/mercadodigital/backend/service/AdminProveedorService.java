package ar.com.mercadodigital.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.response.AdminProveedorDespachoResponse;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional
    public AdminProveedorDespachoResponse actualizarDespacho(UUID proveedorId, boolean despachaMercaderia) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + proveedorId));
        proveedor.setDespachaMercaderia(despachaMercaderia);
        Proveedor saved = proveedorRepository.save(proveedor);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdminProveedorDespachoResponse getDespacho(UUID proveedorId) {
        Proveedor proveedor = proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + proveedorId));
        return toResponse(proveedor);
    }

    private AdminProveedorDespachoResponse toResponse(Proveedor proveedor) {
        return AdminProveedorDespachoResponse.builder()
                .proveedorId(proveedor.getId())
                .nombre(proveedor.getNombre())
                .despachaMercaderia(proveedor.isDespachaMercaderia())
                .updatedAt(proveedor.getUpdatedAt())
                .build();
    }
}
