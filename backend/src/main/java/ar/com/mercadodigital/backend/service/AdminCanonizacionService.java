package ar.com.mercadodigital.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.dto.request.AdminCanonizacionCrearCanonicoRequest;
import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionAsignadoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionCandidatoResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionPendienteResponse;
import ar.com.mercadodigital.backend.dto.response.AdminCanonizacionResultadoResponse;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.ProductoCanonico;
import ar.com.mercadodigital.backend.entity.ProductoMedia;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProductoCanonicoRepository;
import ar.com.mercadodigital.backend.repository.ProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.ProductoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCanonizacionService {

    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT = 100;

    private final ProductoRepository productoRepository;
    private final ProductoCanonicoRepository productoCanonicoRepository;
    private final ProveedorStockRepository proveedorStockRepository;
    private final ProductoMediaRepository productoMediaRepository;

    @Transactional(readOnly = true)
    public List<AdminCanonizacionPendienteResponse> listarPendientes() {
        return productoRepository.findByProductoCanonicoIsNullAndActivoTrueOrderByUpdatedAtDesc().stream()
                .map(this::toPendienteResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminCanonizacionAsignadoResponse> listarAsignados(String query, Integer limit) {
        String normalizedQuery = normalize(query);
        final String needle = normalizedQuery != null ? normalizedQuery.toLowerCase(Locale.ROOT) : null;
        int max = resolveLimit(limit);
        return productoRepository.findByProductoCanonicoIsNotNullAndActivoTrueOrderByUpdatedAtDesc().stream()
                .filter(item -> !StringUtils.hasText(needle) || matchesAsignado(item, needle))
                .limit(max)
                .map(this::toAsignadoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminCanonizacionCandidatoResponse> listarCandidatos(String query, Integer limit) {
        String normalizedQuery = normalize(query);
        final String needle = normalizedQuery != null ? normalizedQuery.toLowerCase(Locale.ROOT) : null;
        int max = resolveLimit(limit);
        return productoCanonicoRepository.findByActivoTrueOrderByNombrePublicoAsc().stream()
                .filter(item -> !StringUtils.hasText(needle) || matches(item, needle))
                .sorted(Comparator.comparing(ProductoCanonico::getNombrePublico, String.CASE_INSENSITIVE_ORDER))
                .limit(max)
                .map(this::toCandidatoResponse)
                .toList();
    }

    @Transactional
    public AdminCanonizacionResultadoResponse asociarAExistente(UUID productoId, UUID productoCanonicoId) {
        Producto producto = getProducto(productoId);
        ProductoCanonico canonico = getCanonico(productoCanonicoId);
        producto.setProductoCanonico(canonico);
        Producto saved = productoRepository.save(producto);
        return toResultadoResponse(saved, canonico, "ASOCIADO_EXISTENTE");
    }

    @Transactional
    public AdminCanonizacionResultadoResponse crearCanonicoYAsociar(UUID productoId, AdminCanonizacionCrearCanonicoRequest request) {
        Producto producto = getProducto(productoId);

        String marca = normalizeOrDefault(request != null ? request.getMarca() : null, producto.getProveedor().getNombre());
        String modelo = normalizeOrDefault(request != null ? request.getModelo() : null, producto.getSku());
        String nombrePublico = normalizeOrDefault(request != null ? request.getNombrePublico() : null, producto.getNombre());
        String atributosJson = normalize(request != null ? request.getAtributosJson() : null);

        validateLength("marca", marca, 80);
        validateLength("modelo", modelo, 120);
        validateLength("nombrePublico", nombrePublico, 200);

        ProductoCanonico canonico = new ProductoCanonico();
        canonico.setMarca(marca);
        canonico.setModelo(modelo);
        canonico.setNombrePublico(nombrePublico);
        canonico.setAtributosJson(atributosJson);
        canonico.setActivo(true);
        ProductoCanonico canonicoGuardado = productoCanonicoRepository.save(canonico);

        producto.setProductoCanonico(canonicoGuardado);
        Producto productoGuardado = productoRepository.save(producto);
        return toResultadoResponse(productoGuardado, canonicoGuardado, "CREADO_Y_ASOCIADO");
    }

    @Transactional
    public AdminCanonizacionResultadoResponse desasociarCanonico(UUID productoId) {
        Producto producto = getProducto(productoId);
        ProductoCanonico canonicoActual = producto.getProductoCanonico();
        if (canonicoActual == null) {
            throw new BadRequestException("El producto ya no tiene canonico asociado.");
        }
        producto.setProductoCanonico(null);
        Producto productoGuardado = productoRepository.save(producto);
        return toResultadoResponse(productoGuardado, canonicoActual, "DESASOCIADO");
    }

    private AdminCanonizacionPendienteResponse toPendienteResponse(Producto producto) {
        Integer stock = proveedorStockRepository.sumStockByProductoId(producto.getId());
        List<ProductoMedia> media = productoMediaRepository.findByProducto_IdOrderByOrdenAsc(producto.getId());
        String imagenPrincipal = media.stream()
                .filter(item -> item.isEsPrincipal() && TipoMedia.IMAGEN.equals(item.getTipoMedia()))
                .findFirst()
                .or(() -> media.stream().filter(item -> TipoMedia.IMAGEN.equals(item.getTipoMedia())).findFirst())
                .map(ProductoMedia::getUrl)
                .orElse(null);

        return AdminCanonizacionPendienteResponse.builder()
                .productoId(producto.getId())
                .sku(producto.getSku())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precioCosto(producto.getPrecioCosto())
                .moneda(producto.getMoneda())
                .categoriaPaquete(producto.getCategoriaPaquete())
                .stockDisponible(stock != null ? stock : 0)
                .imagenPrincipalUrl(imagenPrincipal)
                .proveedorId(producto.getProveedor().getId())
                .proveedorNombre(producto.getProveedor().getNombre())
                .createdAt(producto.getCreatedAt())
                .updatedAt(producto.getUpdatedAt())
                .build();
    }

    private AdminCanonizacionCandidatoResponse toCandidatoResponse(ProductoCanonico canonico) {
        return AdminCanonizacionCandidatoResponse.builder()
                .productoCanonicoId(canonico.getId())
                .marca(canonico.getMarca())
                .modelo(canonico.getModelo())
                .nombrePublico(canonico.getNombrePublico())
                .updatedAt(canonico.getUpdatedAt())
                .build();
    }

    private AdminCanonizacionAsignadoResponse toAsignadoResponse(Producto producto) {
        ProductoCanonico canonico = producto.getProductoCanonico();
        return AdminCanonizacionAsignadoResponse.builder()
                .productoId(producto.getId())
                .sku(producto.getSku())
                .nombreProductoProveedor(producto.getNombre())
                .proveedorId(producto.getProveedor().getId())
                .proveedorNombre(producto.getProveedor().getNombre())
                .productoCanonicoId(canonico != null ? canonico.getId() : null)
                .productoCanonicoNombre(canonico != null ? canonico.getNombrePublico() : null)
                .updatedAt(producto.getUpdatedAt())
                .build();
    }

    private AdminCanonizacionResultadoResponse toResultadoResponse(Producto producto, ProductoCanonico canonico, String accion) {
        return AdminCanonizacionResultadoResponse.builder()
                .productoId(producto.getId())
                .sku(producto.getSku())
                .nombreProductoProveedor(producto.getNombre())
                .proveedorId(producto.getProveedor().getId())
                .proveedorNombre(producto.getProveedor().getNombre())
                .productoCanonicoId(canonico.getId())
                .productoCanonicoNombre(canonico.getNombrePublico())
                .accion(accion)
                .updatedAt(producto.getUpdatedAt())
                .build();
    }

    private Producto getProducto(UUID productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));
    }

    private ProductoCanonico getCanonico(UUID productoCanonicoId) {
        return productoCanonicoRepository.findByIdAndActivoTrue(productoCanonicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto canonico no encontrado: " + productoCanonicoId));
    }

    private boolean matches(ProductoCanonico canonico, String needle) {
        return contains(canonico.getNombrePublico(), needle)
                || contains(canonico.getMarca(), needle)
                || contains(canonico.getModelo(), needle);
    }

    private boolean matchesAsignado(Producto producto, String needle) {
        ProductoCanonico canonico = producto.getProductoCanonico();
        return contains(producto.getSku(), needle)
                || contains(producto.getNombre(), needle)
                || contains(producto.getProveedor().getNombre(), needle)
                || (canonico != null && contains(canonico.getNombrePublico(), needle));
    }

    private boolean contains(String value, String needle) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private int resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized;
    }

    private String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        if (normalized != null) {
            return normalized;
        }
        String fallbackNormalized = normalize(fallback);
        if (fallbackNormalized == null) {
            throw new BadRequestException("No se pudo construir el canonico por falta de datos base.");
        }
        return fallbackNormalized;
    }

    private void validateLength(String field, String value, int maxLength) {
        if (value.length() > maxLength) {
            throw new BadRequestException(field + " supera el maximo de " + maxLength + " caracteres");
        }
    }
}
