package ar.com.mercadodigital.backend.service;

import static ar.com.mercadodigital.backend.dto.CatalogoFichaDto.*;
import ar.com.mercadodigital.backend.dto.CatalogoAdminDto.ProductoInput;
import ar.com.mercadodigital.backend.dto.CatalogoAdminDto.ProductoPreview;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.repository.*;
import ar.com.mercadodigital.backend.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoFichaService {
    private final RevendedorOwnershipService ownership;
    private final RevendedorStorefrontRepository storefronts;
    private final RevendedorProductoRepository productos;
    private final CategoriaProductoRepository categorias;
    private final CatalogoAdminService catalogo;
    private final RevendedorProductoService fichas;
    private final StorefrontAssetStorageService assets;
    private final AuditoriaEventoRepository auditoria;
    private final ObjectMapper json;

    public Ficha obtener(UUID tienda, UUID id) {
        ownership.validarOwnership(tienda);
        return respuesta(tienda, producto(tienda, id, false));
    }

    public ProductoPreview preview(UUID tienda, UUID id, Input input) {
        ownership.validarOwnership(tienda);
        var actual = id == null ? null : producto(tienda, id, false);
        validar(tienda, actual, input);
        var precio = id == null ? catalogo.previewNuevo(tienda, input.precio())
                : catalogo.previewProducto(tienda, id, input.precio());
        return new ProductoPreview(precio.calculo(), hash(Arrays.asList(tienda, id,
                fingerprint(input), precio.token())));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Ficha guardar(UUID tienda, UUID id, Input input, List<MultipartFile> archivos, String baseUrl) {
        ownership.validarOwnership(tienda);
        var sf = storefronts.findForCatalogoUpdate(tienda)
                .orElseThrow(() -> new ResourceNotFoundException("La tienda no tiene storefront configurado."));
        // La misma clave de alta y contenido permite recuperar una respuesta perdida sin duplicar ficha ni archivos.
        if (id == null) {
            var repetido = productos.findByRevendedor_IdAndCatalogoAltaToken(tienda, input.operacionId().toString());
            if (repetido.isPresent()) {
                if (!fingerprint(input).equals(repetido.get().getCatalogoAltaHash()))
                    throw new OperacionEnConflictoException("Esta alta ya fue guardada con otros datos. Recargá la lista antes de editarla.");
                return respuesta(tienda, repetido.get());
            }
        }
        var actual = id == null ? null : producto(tienda, id, true);
        var revisado = preview(tienda, id, input);
        if (!Objects.equals(revisado.token(), input.previewToken()))
            throw new OperacionEnConflictoException("Los datos o costos cambiaron. Volvé a revisar el precio antes de guardar.");
        if (!"LISTO".equals(revisado.calculo().estado()))
            throw new BadRequestException(String.join(" ", revisado.calculo().pendientes()));
        if (archivos.size() != input.archivos().size()) throw new BadRequestException("Revisá los archivos seleccionados.");
        for (int i = 0; i < archivos.size(); i++) validarArchivo(archivos.get(i), input.archivos().get(i));
        Object antes = actual == null ? null : respuesta(tienda, actual);
        var producto = actual == null ? new RevendedorProducto() : actual;
        if (actual == null) {
            producto.setRevendedor(sf.getRevendedor());
            producto.setCatalogoAltaToken(input.operacionId().toString());
            producto.setCatalogoAltaHash(fingerprint(input));
        }
        producto.setSku(input.sku().trim()); producto.setNombre(input.nombre().trim());
        producto.setDescripcion(input.descripcion() == null || input.descripcion().isBlank() ? null : input.descripcion().trim());
        producto.setCategoriaProducto(input.categoriaProductoId() == null ? null : categorias.findById(input.categoriaProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría de producto no encontrada.")));
        producto.setActivo(input.activo()); producto.setPromocion(input.promocion()); producto.setNovedad(input.novedad());
        producto.setUpdatedAt(LocalDateTime.now()); // También versiona cambios exclusivos de imágenes.
        aplicarImagenes(producto, input.imagenes());
        for (var archivo : archivos) {
            var asset = assets.storeTransactional(archivo, baseUrl);
            agregarImagen(producto, asset.getUrl());
        }
        var guardado = productos.saveAndFlush(producto);
        var precio = input.precio();
        var token = catalogo.previewProducto(tienda, guardado.getId(), precio).token();
        catalogo.guardarProducto(tienda, guardado.getId(), new ProductoInput(precio.moneda(), precio.precioOriginal(),
                precio.incluyeEnvio(), precio.categoriaEnvioId(), precio.version(), token));
        var resultado = respuesta(tienda, guardado);
        var evento = new AuditoriaEvento(); evento.setTipoEvento("CATALOGO_FICHA"); evento.setReferenciaId(guardado.getId());
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        try { evento.setPayloadJson(json.writeValueAsString(Arrays.asList(tienda, auth == null ? "sin-contexto" : auth.getName(), antes, resultado))); }
        catch (java.io.IOException ex) { throw new IllegalStateException("No se pudo registrar la ficha", ex); }
        auditoria.save(evento);
        return resultado;
    }

    private void validar(UUID tienda, RevendedorProducto actual, Input input) {
        if (input.imagenes().size() + input.archivos().size() > 20)
            throw new BadRequestException("El producto admite hasta 20 imágenes en total.");
        if ((actual == null ? -1 : actual.getVersion()) != input.versionFicha())
            throw new OperacionEnConflictoException("La ficha cambió. Recargá sus datos antes de guardar.");
        if (productos.existsByRevendedor_IdAndSkuIgnoreCaseAndIdNot(tienda, input.sku().trim(),
                actual == null ? new UUID(0, 0) : actual.getId()))
            throw new OperacionEnConflictoException("Ya existe un producto con ese SKU en esta tienda.");
        var ids = new HashSet<UUID>();
        for (var imagen : input.imagenes()) {
            validarUrl(imagen.url());
            if (imagen.id() != null && (!ids.add(imagen.id()) || actual == null || actual.getMedia().stream()
                    .noneMatch(m -> m.getId().equals(imagen.id()) && TipoMedia.IMAGEN.equals(m.getTipoMedia()))))
                throw new BadRequestException("La imagen no pertenece a este producto o está repetida.");
        }
        if (input.categoriaProductoId() != null && !categorias.existsById(input.categoriaProductoId()))
            throw new ResourceNotFoundException("Categoría de producto no encontrada.");
    }

    private void aplicarImagenes(RevendedorProducto p, List<Imagen> imagenes) {
        Map<UUID, RevendedorProductoMedia> existentes = new HashMap<>();
        p.getMedia().forEach(m -> existentes.put(m.getId(), m));
        p.getMedia().removeIf(m -> TipoMedia.IMAGEN.equals(m.getTipoMedia())
                && imagenes.stream().noneMatch(i -> m.getId().equals(i.id())));
        for (var imagen : imagenes) {
            var media = imagen.id() == null ? new RevendedorProductoMedia() : existentes.get(imagen.id());
            media.setRevendedorProducto(p); media.setTipoMedia(TipoMedia.IMAGEN); media.setUrl(imagen.url().trim());
            media.setOrden(imagenes.indexOf(imagen)); media.setEsPrincipal(imagenes.indexOf(imagen) == 0);
            if (imagen.id() == null) p.getMedia().add(media);
        }
    }

    private void agregarImagen(RevendedorProducto p, String url) {
        var media = new RevendedorProductoMedia(); media.setRevendedorProducto(p);
        media.setTipoMedia(TipoMedia.IMAGEN); media.setUrl(url);
        int cantidad = (int) p.getMedia().stream().filter(m -> TipoMedia.IMAGEN.equals(m.getTipoMedia())).count();
        media.setOrden(cantidad); media.setEsPrincipal(cantidad == 0); p.getMedia().add(media);
    }

    private void validarUrl(String value) {
        try {
            var uri = URI.create(value.trim());
            if (value.length() > 500 || uri.getUserInfo() != null || uri.getHost() == null
                    || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())))
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) { throw new BadRequestException("Usá una URL de imagen http o https válida."); }
    }

    private void validarArchivo(MultipartFile file, Archivo esperado) {
        try {
            if (file.getSize() != esperado.size() || !Objects.equals(file.getOriginalFilename(), esperado.nombre())
                    || !hashBytes(file.getBytes()).equals(esperado.sha256()))
                throw new BadRequestException("Los archivos cambiaron. Revisá nuevamente la ficha.");
        } catch (java.io.IOException ex) { throw new BadRequestException("No se pudo leer la imagen seleccionada."); }
    }

    private RevendedorProducto producto(UUID tienda, UUID id, boolean lock) {
        return (lock ? productos.findForUpdate(id, tienda) : productos.findByIdAndRevendedor_Id(id, tienda))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado en esta tienda."));
    }
    private Ficha respuesta(UUID tienda, RevendedorProducto p) {
        return new Ficha(fichas.toResponse(p), p.getVersion(), catalogo.productoConfigurado(tienda, p.getId()));
    }
    private String fingerprint(Input input) {
        try { return hashBytes(json.writeValueAsString(Arrays.asList(input.operacionId(), input.sku(), input.nombre(),
                input.descripcion(), input.categoriaProductoId(), input.activo(), input.promocion(), input.novedad(),
                input.versionFicha(), input.precio().moneda(), input.precio().precioOriginal(), input.precio().incluyeEnvio(),
                input.precio().categoriaEnvioId(), input.precio().version(), input.imagenes(), input.archivos())).getBytes(StandardCharsets.UTF_8)); }
        catch (java.io.IOException ex) { throw new IllegalStateException("No se pudo revisar la ficha", ex); }
    }
    private String hash(Object value) {
        try { return hashBytes(json.writeValueAsBytes(value)); }
        catch (java.io.IOException ex) { throw new IllegalStateException("No se pudo revisar la ficha", ex); }
    }
    private String hashBytes(byte[] value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
