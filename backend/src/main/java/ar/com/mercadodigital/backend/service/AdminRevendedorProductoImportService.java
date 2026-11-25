package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import ar.com.mercadodigital.backend.dto.request.AdminRevendedorProductoImportRequest;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorProductoImportErrorResponse;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorProductoImportResponse;
import ar.com.mercadodigital.backend.entity.CategoriaPaquete;
import ar.com.mercadodigital.backend.entity.CategoriaProducto;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.entity.RevendedorProductoMedia;
import ar.com.mercadodigital.backend.entity.TipoMedia;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.CategoriaProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoMediaRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminRevendedorProductoImportService {

    private static final int MAX_ITEMS = 2000;
    private static final int MAX_IMAGENES = 5;

    private final RevendedorRepository revendedorRepository;
    private final RevendedorProductoRepository productoRepository;
    private final RevendedorProductoMediaRepository mediaRepository;
    private final CategoriaProductoRepository categoriaProductoRepository;
    private final PlatformTransactionManager transactionManager;
    private final CatalogoModoService modoCatalogo;

    public AdminRevendedorProductoImportResponse importar(
            UUID revendedorId,
            AdminRevendedorProductoImportRequest request) {
        modoCatalogo.rechazarImportacionAnterior(revendedorId);
        if (!revendedorRepository.existsById(revendedorId)) {
            throw new ResourceNotFoundException("Revendedor no encontrado: " + revendedorId);
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("La importacion no contiene productos");
        }
        if (request.getItems().size() > MAX_ITEMS) {
            throw new BadRequestException("La importacion permite hasta " + MAX_ITEMS + " productos por operacion");
        }

        Map<String, Integer> repeticiones = contarSkuIncluidos(request.getItems());
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        int rechazados = 0;
        List<AdminRevendedorProductoImportErrorResponse> errores = new ArrayList<>();

        for (AdminRevendedorProductoImportRequest.Item item : request.getItems()) {
            if (!Boolean.TRUE.equals(item.getIncluir())) {
                omitidos++;
                continue;
            }
            try {
                String skuNormalizado = normalizarSku(item.getSku());
                if (repeticiones.getOrDefault(skuNormalizado.toLowerCase(Locale.ROOT), 0) > 1) {
                    throw new BadRequestException("El SKU esta repetido dentro del archivo");
                }
                ResultadoFila resultado = transactionTemplate.execute(status ->
                        importarFila(revendedorId, item, request, skuNormalizado));
                if (resultado == ResultadoFila.CREADO) {
                    creados++;
                } else {
                    actualizados++;
                }
            } catch (RuntimeException ex) {
                rechazados++;
                errores.add(AdminRevendedorProductoImportErrorResponse.builder()
                        .fila(item.getFila())
                        .sku(item.getSku())
                        .mensaje(mensajeSeguro(ex))
                        .build());
            }
        }

        return AdminRevendedorProductoImportResponse.builder()
                .filasRecibidas(request.getItems().size())
                .creados(creados)
                .actualizados(actualizados)
                .omitidos(omitidos)
                .rechazados(rechazados)
                .errores(errores)
                .build();
    }

    private ResultadoFila importarFila(
            UUID revendedorId,
            AdminRevendedorProductoImportRequest.Item item,
            AdminRevendedorProductoImportRequest request,
            String sku) {
        RevendedorProducto producto = productoRepository
                .findByRevendedor_IdAndSkuIgnoreCase(revendedorId, sku)
                .orElse(null);
        boolean existente = producto != null;
        if (existente && producto.getPrecioVenta() == null) {
            throw new BadRequestException("El producto pertenece al nuevo catálogo. Editá su ficha y precio desde Catálogo.");
        }
        if (!existente) {
            producto = new RevendedorProducto();
            producto.setRevendedor(revendedorRepository.getReferenceById(revendedorId));
            producto.setSku(sku);
            producto.setNombre(validarNombre(item.getNombre()));
            producto.setDescripcion(normalizarOpcional(item.getDescripcion()));
            producto.setPrecioVenta(validarPrecio(item.getPrecioVenta()));
            producto.setStock(validarStock(item.getStock()));
            producto.setCategoriaPaquete(normalizarCategoriaPaquete(item.getCategoriaPaquete()));
            producto.setCategoriaProducto(resolveCategoria(item.getCategoriaProductoId()));
            producto.setActivo(item.getActivo() == null || Boolean.TRUE.equals(item.getActivo()));
            producto.setPromocion(Boolean.TRUE.equals(item.getPromocion()));
            producto.setNovedad(Boolean.TRUE.equals(item.getNovedad()));
            producto.setMoneda("ARS");
        } else {
            if (Boolean.TRUE.equals(request.getActualizarNombre())) {
                producto.setNombre(validarNombre(item.getNombre()));
            }
            if (Boolean.TRUE.equals(request.getActualizarDescripcion())) {
                producto.setDescripcion(normalizarOpcional(item.getDescripcion()));
            }
            if (Boolean.TRUE.equals(request.getActualizarPrecio())) {
                producto.setPrecioVenta(validarPrecio(item.getPrecioVenta()));
            }
            if (Boolean.TRUE.equals(request.getActualizarStock())) {
                producto.setStock(validarStock(item.getStock()));
            }
            if (Boolean.TRUE.equals(request.getActualizarCategoriaPaquete())) {
                producto.setCategoriaPaquete(normalizarCategoriaPaquete(item.getCategoriaPaquete()));
            }
            if (Boolean.TRUE.equals(request.getActualizarCategoria())) {
                producto.setCategoriaProducto(resolveCategoria(item.getCategoriaProductoId()));
            }
            if (Boolean.TRUE.equals(request.getActualizarEstado())) {
                producto.setActivo(Boolean.TRUE.equals(item.getActivo()));
            }
            if (Boolean.TRUE.equals(request.getActualizarDestacados())) {
                producto.setPromocion(Boolean.TRUE.equals(item.getPromocion()));
                producto.setNovedad(Boolean.TRUE.equals(item.getNovedad()));
            }
        }

        RevendedorProducto guardado = productoRepository.save(producto);
        if (!existente || Boolean.TRUE.equals(request.getActualizarImagenes())) {
            aplicarImagenes(guardado, item.getImagenes(), item.getModoImagenes(), existente);
        }
        return existente ? ResultadoFila.ACTUALIZADO : ResultadoFila.CREADO;
    }

    private void aplicarImagenes(
            RevendedorProducto producto,
            List<String> imagenesCrudas,
            String modoCrudo,
            boolean existente) {
        List<String> imagenes = normalizarImagenes(imagenesCrudas);
        if (imagenes.isEmpty()) {
            return;
        }
        ModoImagenes modo = normalizarModoImagenes(modoCrudo, existente);
        if (existente && modo == ModoImagenes.CONSERVAR) {
            return;
        }

        if (modo == ModoImagenes.REEMPLAZAR_PORTADA) {
            producto.getMedia().forEach(media -> media.setEsPrincipal(false));
        }
        int siguienteOrden = producto.getMedia().stream()
                .mapToInt(RevendedorProductoMedia::getOrden)
                .max()
                .orElse(-1) + 1;
        boolean tienePrincipal = producto.getMedia().stream().anyMatch(RevendedorProductoMedia::isEsPrincipal);
        Set<String> existentes = new HashSet<>();
        producto.getMedia().stream()
                .filter(media -> TipoMedia.IMAGEN.equals(media.getTipoMedia()))
                .map(RevendedorProductoMedia::getUrl)
                .forEach(existentes::add);
        long imagenesNuevas = imagenes.stream().filter(url -> !existentes.contains(url)).count();
        if (existentes.size() + imagenesNuevas > MAX_IMAGENES) {
            throw new BadRequestException("El producto admite hasta " + MAX_IMAGENES + " imagenes en total");
        }

        for (String url : imagenes) {
            if (!existentes.add(url)) {
                continue;
            }
            RevendedorProductoMedia media = new RevendedorProductoMedia();
            media.setRevendedorProducto(producto);
            media.setTipoMedia(TipoMedia.IMAGEN);
            media.setUrl(url);
            media.setOrden(siguienteOrden++);
            media.setEsPrincipal(!tienePrincipal || modo == ModoImagenes.REEMPLAZAR_PORTADA);
            tienePrincipal = true;
            producto.getMedia().add(media);
            mediaRepository.save(media);
            if (modo == ModoImagenes.REEMPLAZAR_PORTADA) {
                modo = ModoImagenes.AGREGAR;
            }
        }
    }

    private Map<String, Integer> contarSkuIncluidos(List<AdminRevendedorProductoImportRequest.Item> items) {
        Map<String, Integer> result = new HashMap<>();
        for (AdminRevendedorProductoImportRequest.Item item : items) {
            if (!Boolean.TRUE.equals(item.getIncluir()) || !StringUtils.hasText(item.getSku())) {
                continue;
            }
            String key = item.getSku().trim().toLowerCase(Locale.ROOT);
            result.merge(key, 1, Integer::sum);
        }
        return result;
    }

    private String normalizarSku(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("SKU es obligatorio");
        }
        String normalized = value.trim();
        if (normalized.length() > 80) {
            throw new BadRequestException("SKU supera los 80 caracteres");
        }
        return normalized;
    }

    private String validarNombre(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Titulo es obligatorio");
        }
        String normalized = value.trim();
        if (normalized.length() > 180) {
            throw new BadRequestException("Titulo supera los 180 caracteres");
        }
        return normalized;
    }

    private BigDecimal validarPrecio(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new BadRequestException("Importe debe ser mayor o igual a 0");
        }
        return value;
    }

    private int validarStock(Integer value) {
        int stock = value == null ? 0 : value;
        if (stock < 0) {
            throw new BadRequestException("Stock debe ser mayor o igual a 0");
        }
        return stock;
    }

    private String normalizarCategoriaPaquete(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : CategoriaPaquete.MEDIANO.name();
        try {
            return CategoriaPaquete.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Tipo de paquete invalido: " + value);
        }
    }

    private CategoriaProducto resolveCategoria(UUID categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return categoriaProductoRepository.findById(categoriaId)
                .filter(CategoriaProducto::isActiva)
                .orElseThrow(() -> new BadRequestException("Categoria inexistente o inactiva: " + categoriaId));
    }

    private List<String> normalizarImagenes(List<String> values) {
        if (values == null) {
            return List.of();
        }
        List<String> result = values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (result.size() > MAX_IMAGENES) {
            throw new BadRequestException("Cada producto admite hasta " + MAX_IMAGENES + " imagenes");
        }
        result.forEach(this::validarUrlImagen);
        return result;
    }

    private void validarUrlImagen(String value) {
        if (value.length() > 500) {
            throw new BadRequestException("La URL de imagen supera los 500 caracteres");
        }
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || !StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("URL de imagen invalida: " + value);
        }
    }

    private ModoImagenes normalizarModoImagenes(String value, boolean existente) {
        if (!existente) {
            return ModoImagenes.AGREGAR;
        }
        if (!StringUtils.hasText(value)) {
            return ModoImagenes.CONSERVAR;
        }
        try {
            return ModoImagenes.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Modo de imagenes invalido: " + value);
        }
    }

    private String normalizarOpcional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String mensajeSeguro(RuntimeException ex) {
        return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "No se pudo importar la fila";
    }

    private enum ResultadoFila {
        CREADO,
        ACTUALIZADO
    }

    private enum ModoImagenes {
        CONSERVAR,
        AGREGAR,
        REEMPLAZAR_PORTADA
    }
}
