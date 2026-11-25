package ar.com.mercadodigital.backend.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import ar.com.mercadodigital.backend.dto.response.ProductoResponse;
import ar.com.mercadodigital.backend.entity.Producto;

@Component
public class ProductoMapper {

    public ProductoResponse toResponse(Producto producto, BigDecimal precioFinalEstimado, Integer stockDisponible,
            Integer demoraEstimadaDias) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .productoCanonicoId(producto.getProductoCanonico() != null ? producto.getProductoCanonico().getId() : null)
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precioFinalEstimado(precioFinalEstimado)
                .categoriaPaquete(producto.getCategoriaPaquete())
                .stockDisponible(stockDisponible)
                .demoraEstimadaDias(demoraEstimadaDias)
                .build();
    }
}
