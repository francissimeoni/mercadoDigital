package ar.com.mercadodigital.backend.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.core.Authentication;

import ar.com.mercadodigital.backend.dto.request.AdminCategoriaProductoRequest;
import ar.com.mercadodigital.backend.dto.request.ProveedorCategoriaProductoSugerenciaRequest;
import ar.com.mercadodigital.backend.dto.response.AdminCategoriaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorCategoriaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.ProveedorCategoriaProductoSugerenciaResponse;
import ar.com.mercadodigital.backend.entity.CategoriaProducto;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.CategoriaProductoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaProductoService {

    private final CategoriaProductoRepository categoriaProductoRepository;
    private final ProveedorOwnershipService proveedorOwnershipService;

    @Transactional(readOnly = true)
    public List<AdminCategoriaProductoResponse> getCategoriasAdmin() {
        return categoriaProductoRepository.findAllByOrderByActivaDescNombreAsc().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public AdminCategoriaProductoResponse crearCategoriaAdmin(AdminCategoriaProductoRequest request) {
        String nombre = normalizeNombre(request.getNombre());
        String slug = slugify(nombre);
        CategoriaProducto categoria = categoriaProductoRepository.findBySlug(slug).orElseGet(CategoriaProducto::new);
        categoria.setNombre(nombre);
        categoria.setSlug(slug);
        categoria.setActiva(request.getActiva() == null || request.getActiva());
        if (categoria.isActiva()) {
            categoria.setSugerida(false);
            categoria.setSugeridaPorProveedor(null);
        }
        return toAdminResponse(categoriaProductoRepository.save(categoria));
    }

    @Transactional
    public AdminCategoriaProductoResponse actualizarCategoriaAdmin(UUID categoriaId, AdminCategoriaProductoRequest request) {
        CategoriaProducto categoria = categoriaProductoRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria de producto no encontrada: " + categoriaId));

        String nombre = normalizeNombre(request.getNombre());
        String slug = slugify(nombre);
        categoriaProductoRepository.findBySlug(slug)
                .filter(existing -> !existing.getId().equals(categoriaId))
                .ifPresent(existing -> {
                    throw new BadRequestException("La categoria ya existe: " + nombre);
                });

        categoria.setNombre(nombre);
        categoria.setSlug(slug);
        if (request.getActiva() != null) {
            categoria.setActiva(request.getActiva());
        }
        if (categoria.isActiva()) {
            categoria.setSugerida(false);
            categoria.setSugeridaPorProveedor(null);
        }
        return toAdminResponse(categoriaProductoRepository.save(categoria));
    }

    @Transactional(readOnly = true)
    public List<ProveedorCategoriaProductoResponse> getCategoriasActivasProveedor(UUID proveedorId, Authentication authentication) {
        proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        return categoriaProductoRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(this::toProveedorResponse)
                .toList();
    }

    @Transactional
    public ProveedorCategoriaProductoSugerenciaResponse sugerirCategoriaProveedor(
            UUID proveedorId,
            ProveedorCategoriaProductoSugerenciaRequest request,
            Authentication authentication) {
        Proveedor proveedor = proveedorOwnershipService.resolveProveedorAutorizado(proveedorId, authentication);
        String nombre = normalizeNombre(request.getNombre());
        String slug = slugify(nombre);

        CategoriaProducto existente = categoriaProductoRepository.findBySlug(slug).orElse(null);
        if (existente != null) {
            if (existente.isActiva()) {
                return ProveedorCategoriaProductoSugerenciaResponse.builder()
                        .categoriaId(existente.getId())
                        .nombre(existente.getNombre())
                        .activa(true)
                        .estado("YA_EXISTE_ACTIVA")
                        .mensaje("La categoria ya existe y esta disponible para seleccionar.")
                        .build();
            }
            return ProveedorCategoriaProductoSugerenciaResponse.builder()
                    .categoriaId(existente.getId())
                    .nombre(existente.getNombre())
                    .activa(false)
                    .estado("YA_EXISTE_PENDIENTE")
                    .mensaje("La categoria ya fue sugerida y esta pendiente de revision admin.")
                    .build();
        }

        CategoriaProducto nueva = new CategoriaProducto();
        nueva.setNombre(nombre);
        nueva.setSlug(slug);
        nueva.setActiva(false);
        nueva.setSugerida(true);
        nueva.setSugeridaPorProveedor(proveedor);
        CategoriaProducto saved = categoriaProductoRepository.save(nueva);
        return ProveedorCategoriaProductoSugerenciaResponse.builder()
                .categoriaId(saved.getId())
                .nombre(saved.getNombre())
                .activa(false)
                .estado("CREADA_PENDIENTE")
                .mensaje("Categoria sugerida correctamente. Queda pendiente de activacion en admin.")
                .build();
    }

    @Transactional(readOnly = true)
    public CategoriaProducto resolveCategoriaProducto(UUID categoriaProductoId) {
        if (categoriaProductoId == null) {
            return null;
        }
        return categoriaProductoRepository.findById(categoriaProductoId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria de producto no encontrada: " + categoriaProductoId));
    }

    private AdminCategoriaProductoResponse toAdminResponse(CategoriaProducto categoria) {
        return AdminCategoriaProductoResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .slug(categoria.getSlug())
                .activa(categoria.isActiva())
                .sugerida(categoria.isSugerida())
                .sugeridaPorProveedorId(categoria.getSugeridaPorProveedor() != null ? categoria.getSugeridaPorProveedor().getId() : null)
                .createdAt(categoria.getCreatedAt())
                .updatedAt(categoria.getUpdatedAt())
                .build();
    }

    private ProveedorCategoriaProductoResponse toProveedorResponse(CategoriaProducto categoria) {
        return ProveedorCategoriaProductoResponse.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .build();
    }

    private String normalizeNombre(String nombre) {
        if (!StringUtils.hasText(nombre)) {
            throw new BadRequestException("nombre es obligatorio");
        }
        String normalized = nombre.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 120) {
            throw new BadRequestException("nombre supera el maximo permitido");
        }
        return normalized;
    }

    private String slugify(String nombre) {
        String ascii = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (!StringUtils.hasText(ascii)) {
            throw new BadRequestException("nombre de categoria invalido");
        }
        if (ascii.length() > 140) {
            ascii = ascii.substring(0, 140);
            ascii = ascii.replaceAll("-+$", "");
        }
        return ascii;
    }
}
