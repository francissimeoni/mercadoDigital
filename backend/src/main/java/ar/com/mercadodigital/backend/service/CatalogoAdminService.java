package ar.com.mercadodigital.backend.service;

import static ar.com.mercadodigital.backend.dto.CatalogoAdminDto.*;
import ar.com.mercadodigital.backend.dto.CatalogoAdminDto.Producto;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.repository.*;
import ar.com.mercadodigital.backend.exception.*;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoAdminService {
    private final RevendedorOwnershipService ownership;
    private final RevendedorStorefrontRepository storefronts;
    private final RevendedorProductoRepository productos;
    private final CatalogoConfiguracionRepository configuraciones;
    private final CatalogoCategoriaEnvioRepository categorias;
    private final CatalogoProductoConfiguracionRepository precios;
    private final CatalogoPrecioCalculator calculator;
    private final AuditoriaEventoRepository auditoria;
    private final ObjectMapper json;

    public Config configuracion(UUID tienda) {
        var sf = tienda(tienda, false);
        return configResponse(configuraciones.findByRevendedor_Id(tienda).orElse(null), sf);
    }

    public List<Categoria> categorias(UUID tienda) {
        tienda(tienda, false);
        return categorias.findByRevendedor_IdOrderByNombreAsc(tienda).stream().map(this::categoriaResponse).toList();
    }

    public List<Producto> productos(UUID tienda) {
        tienda(tienda, false);
        var config = configuraciones.findByRevendedor_Id(tienda).orElse(null);
        Map<UUID, CatalogoProductoConfiguracion> porProducto = new HashMap<>();
        precios.findByProducto_Revendedor_IdOrderByProducto_NombreAsc(tienda)
                .forEach(p -> porProducto.put(p.getProducto().getId(), p));
        // No filtra por stock, ni modifica productos o el catálogo público legado.
        return productos.findByRevendedor_IdOrderByUpdatedAtDesc(tienda).stream()
                .map(p -> productoResponse(p, porProducto.get(p.getId()), cotizacion(config))).toList();
    }

    public ProductoPreview previewProducto(UUID tienda, UUID producto, ProductoInput input) {
        tienda(tienda, false);
        producto(tienda, producto);
        return previewProductoInterno(tienda, producto, input);
    }

    public ProductoPreview previewNuevo(UUID tienda, ProductoInput input) {
        tienda(tienda, false);
        version(-1, input.version());
        return previewProductoInterno(tienda, null, input);
    }

    public Producto productoConfigurado(UUID tienda, UUID productoId) {
        tienda(tienda, false);
        return productoResponse(producto(tienda, productoId),
                precios.findByProducto_IdAndProducto_Revendedor_Id(productoId, tienda).orElse(null), cotizacionActual(tienda));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Producto guardarProducto(UUID tienda, UUID productoId, ProductoInput input) {
        tienda(tienda, true);
        var producto = producto(tienda, productoId);
        var actual = precios.findByProducto_IdAndProducto_Revendedor_Id(productoId, tienda).orElse(null);
        version(actual == null ? -1 : actual.getVersion(), input.version());
        var preview = previewProductoInterno(tienda, productoId, input);
        var calculo = preview.calculo();
        if (!"LISTO".equals(calculo.estado())) throw new BadRequestException(String.join(" ", calculo.pendientes()));
        confirmar(preview.token(), input.previewToken());
        var antes = actual == null ? null : productoResponse(producto, actual, cotizacionActual(tienda));
        var nuevo = actual == null ? new CatalogoProductoConfiguracion() : actual;
        nuevo.setProducto(producto);
        nuevo.setMoneda(input.moneda());
        nuevo.setPrecioOriginal(input.precioOriginal());
        nuevo.setIncluyeEnvio(input.incluyeEnvio());
        nuevo.setCategoriaEnvio(Boolean.TRUE.equals(input.incluyeEnvio()) ? null : categoria(tienda, input.categoriaEnvioId()));
        var guardado = precios.saveAndFlush(nuevo);
        var respuesta = productoResponse(producto, guardado, cotizacionActual(tienda));
        auditar("CATALOGO_PRECIO", tienda, productoId, antes, respuesta);
        return respuesta;
    }

    public Impacto previewConfig(UUID tienda, ConfigInput input) {
        var sf = tienda(tienda, false);
        return impactoConfig(tienda, sf, input);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Config guardarConfig(UUID tienda, ConfigInput input) {
        var sf = tienda(tienda, true);
        var impacto = impactoConfig(tienda, sf, input);
        confirmar(impacto.token(), input.impactoToken());
        var actual = configuraciones.findByRevendedor_Id(tienda).orElse(null);
        var antes = configResponse(actual, sf);
        var nueva = actual == null ? new CatalogoConfiguracion() : actual;
        nueva.setRevendedor(sf.getRevendedor());
        nueva.setCotizacionUsd(input.cotizacionUsd());
        nueva.setCondicionesEntrega(texto(input.condicionesEntrega()));
        sf.setTelefonoComercial(WhatsAppComercial.normalizar(input.telefonoComercial()));
        storefronts.save(sf);
        var guardada = configuraciones.saveAndFlush(nueva);
        var respuesta = configResponse(guardada, sf);
        auditar("CATALOGO_CONFIG", tienda, tienda, antes, respuesta);
        return respuesta;
    }

    public Impacto previewCategoria(UUID tienda, UUID categoriaId, CategoriaInput input) {
        tienda(tienda, false);
        return impactoCategoria(tienda, categoria(tienda, categoriaId), input);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Categoria crearCategoria(UUID tienda, CategoriaInput input) {
        var sf = tienda(tienda, true);
        version(-1, input.version());
        nombreDisponible(tienda, input.nombre(), new UUID(0, 0));
        var nueva = new CatalogoCategoriaEnvio();
        nueva.setRevendedor(sf.getRevendedor());
        nueva.setNombre(input.nombre().trim());
        nueva.setCostoArs(input.costoArs());
        var guardada = categorias.saveAndFlush(nueva);
        var respuesta = categoriaResponse(guardada);
        auditar("CATALOGO_ENVIO_ALTA", tienda, guardada.getId(), null, respuesta);
        return respuesta;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Categoria actualizarCategoria(UUID tienda, UUID categoriaId, CategoriaInput input) {
        tienda(tienda, true);
        var actual = categoria(tienda, categoriaId);
        var impacto = impactoCategoria(tienda, actual, input);
        confirmar(impacto.token(), input.impactoToken());
        nombreDisponible(tienda, input.nombre(), categoriaId);
        var antes = categoriaResponse(actual);
        actual.setNombre(input.nombre().trim());
        actual.setCostoArs(input.costoArs());
        var respuesta = categoriaResponse(categorias.saveAndFlush(actual));
        auditar("CATALOGO_ENVIO_CAMBIO", tienda, categoriaId, antes, respuesta);
        return respuesta;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void eliminarCategoria(UUID tienda, UUID categoriaId, long expectedVersion) {
        tienda(tienda, true);
        var actual = categoria(tienda, categoriaId);
        version(actual.getVersion(), expectedVersion);
        if (precios.existsByCategoriaEnvio_Id(categoriaId)) throw new OperacionEnConflictoException(
                "La categoría está en uso. Reasigná sus productos antes de eliminarla.");
        auditar("CATALOGO_ENVIO_BAJA", tienda, categoriaId, categoriaResponse(actual), null);
        categorias.delete(actual);
    }

    private Impacto impactoConfig(UUID tienda, RevendedorStorefront sf, ConfigInput input) {
        var actual = configuraciones.findByRevendedor_Id(tienda).orElse(null);
        version(actual == null ? -1 : actual.getVersion(), input.version());
        String telefono = WhatsAppComercial.normalizar(input.telefonoComercial());
        var cambios = precios.findByProducto_Revendedor_IdOrderByProducto_NombreAsc(tienda).stream()
                .filter(p -> "USD".equals(p.getMoneda()))
                .sorted(Comparator.comparing(p -> p.getProducto().getId()))
                .map(p -> new Cambio(p.getProducto().getId(), p.getProducto().getNombre(),
                        calcular(p, cotizacion(actual), null), calcular(p, input.cotizacionUsd(), null))).toList();
        // No deja quitar la cotización si existen productos USD preparados.
        if (input.cotizacionUsd() == null && !cambios.isEmpty()) throw new BadRequestException(
                "Hay productos configurados en USD. Convertí sus precios a ARS antes de quitar la cotización.");
        validarImpacto(cambios);
        return new Impacto(cambios, hash(Arrays.asList(tienda, configResponse(actual, sf),
                input.cotizacionUsd(), texto(input.condicionesEntrega()), telefono, cambios)));
    }

    private Impacto impactoCategoria(UUID tienda, CatalogoCategoriaEnvio categoria, CategoriaInput input) {
        version(categoria.getVersion(), input.version());
        var cotizacion = cotizacionActual(tienda);
        var cambios = precios.findByProducto_Revendedor_IdOrderByProducto_NombreAsc(tienda).stream()
                .filter(p -> !p.isIncluyeEnvio() && p.getCategoriaEnvio() != null && p.getCategoriaEnvio().getId().equals(categoria.getId()))
                .sorted(Comparator.comparing(p -> p.getProducto().getId()))
                .map(p -> new Cambio(p.getProducto().getId(), p.getProducto().getNombre(),
                        calcular(p, cotizacion, null), calcular(p, cotizacion, input.costoArs()))).toList();
        validarImpacto(cambios);
        return new Impacto(cambios, hash(Arrays.asList(tienda, categoriaResponse(categoria), input.nombre().trim(), input.costoArs(), cambios)));
    }

    private void validarImpacto(List<Cambio> cambios) {
        if (cambios.stream().anyMatch(c -> !"LISTO".equals(c.propuesto().estado()))) throw new BadRequestException(
                "El cambio deja precios sin calcular o fuera de rango. Revisá los productos afectados.");
    }

    private Precio calcularInput(UUID tienda, ProductoInput input) {
        BigDecimal envio = null;
        if (Boolean.FALSE.equals(input.incluyeEnvio()) && input.categoriaEnvioId() != null)
            envio = categoria(tienda, input.categoriaEnvioId()).getCostoArs();
        return calculator.calcular(input.moneda(), input.precioOriginal(), input.incluyeEnvio(), cotizacionActual(tienda), envio);
    }

    private ProductoPreview previewProductoInterno(UUID tienda, UUID producto, ProductoInput input) {
        var calculo = calcularInput(tienda, input);
        return new ProductoPreview(calculo, hash(Arrays.asList(tienda, producto, input.moneda(),
                input.precioOriginal(), input.incluyeEnvio(), input.categoriaEnvioId(), input.version(), calculo)));
    }

    private Precio calcular(CatalogoProductoConfiguracion p, BigDecimal cotizacion, BigDecimal envioOverride) {
        BigDecimal envio = envioOverride != null ? envioOverride : p.getCategoriaEnvio() == null
                || !p.getProducto().getRevendedor().getId().equals(p.getCategoriaEnvio().getRevendedor().getId())
                ? null : p.getCategoriaEnvio().getCostoArs();
        return calculator.calcular(p.getMoneda(), p.getPrecioOriginal(), p.isIncluyeEnvio(), cotizacion, envio);
    }

    private Producto productoResponse(RevendedorProducto p, CatalogoProductoConfiguracion c, BigDecimal cotizacion) {
        if (c == null) return new Producto(p.getId(), p.getNombre(), p.getSku(), null, null, null, null, -1,
                new Precio("PENDIENTE", List.of("Configurá moneda, precio y envío para el nuevo catálogo."), null, null, null, null, null, null), p.isActivo());
        return new Producto(p.getId(), p.getNombre(), p.getSku(), c.getMoneda(), c.getPrecioOriginal(), c.isIncluyeEnvio(),
                c.getCategoriaEnvio() == null ? null : c.getCategoriaEnvio().getId(), c.getVersion(), calcular(c, cotizacion, null), p.isActivo());
    }

    private Config configResponse(CatalogoConfiguracion c, RevendedorStorefront sf) {
        List<String> pendientes = new ArrayList<>();
        if (cotizacion(c) == null) pendientes.add("Cotización pendiente: necesaria únicamente para productos en USD.");
        try { if (WhatsAppComercial.normalizar(sf.getTelefonoComercial()) == null) pendientes.add("Configurá el WhatsApp comercial de la tienda."); }
        catch (BadRequestException ex) { pendientes.add("Revisá el formato internacional del WhatsApp comercial."); }
        if (c == null || texto(c.getCondicionesEntrega()) == null) pendientes.add("Indicá las condiciones y cobertura de entrega.");
        return new Config(cotizacion(c), c == null ? null : c.getCondicionesEntrega(), sf.getTelefonoComercial(),
                c == null ? -1 : c.getVersion(), "PREPARACION", List.copyOf(pendientes));
    }

    private Categoria categoriaResponse(CatalogoCategoriaEnvio c) { return new Categoria(c.getId(), c.getNombre(), c.getCostoArs(), c.getVersion()); }
    private BigDecimal cotizacion(CatalogoConfiguracion c) { return c == null ? null : c.getCotizacionUsd(); }
    private BigDecimal cotizacionActual(UUID tienda) { return configuraciones.findByRevendedor_Id(tienda).map(CatalogoConfiguracion::getCotizacionUsd).orElse(null); }
    private String texto(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private RevendedorStorefront tienda(UUID id, boolean lock) {
        ownership.validarOwnership(id);
        return (lock ? storefronts.findForCatalogoUpdate(id) : storefronts.findByRevendedor_Id(id))
                .orElseThrow(() -> new ResourceNotFoundException("La tienda no tiene storefront configurado."));
    }
    private RevendedorProducto producto(UUID tienda, UUID id) {
        return productos.findByIdAndRevendedor_Id(id, tienda).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado en esta tienda."));
    }
    private CatalogoCategoriaEnvio categoria(UUID tienda, UUID id) {
        if (id == null) throw new BadRequestException("Elegí una categoría de envío.");
        return categorias.findByIdAndRevendedor_Id(id, tienda).orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada en esta tienda."));
    }
    private void nombreDisponible(UUID tienda, String nombre, UUID excepto) {
        if (categorias.existsByRevendedor_IdAndNombreIgnoreCaseAndIdNot(tienda, nombre.trim(), excepto))
            throw new OperacionEnConflictoException("Ya existe una categoría de envío con ese nombre.");
    }
    private void version(long actual, Long enviada) {
        if (enviada == null || actual != enviada) throw new OperacionEnConflictoException("La configuración cambió. Recargá y revisá los valores antes de guardar.");
    }
    private void confirmar(String actual, String enviado) {
        if (!actual.equals(enviado)) throw new OperacionEnConflictoException("Revisá nuevamente la vista previa: los valores o los productos afectados cambiaron.");
    }
    private String hash(Object value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(value).getBytes(StandardCharsets.UTF_8))); }
        catch (JsonProcessingException | NoSuchAlgorithmException ex) { throw new IllegalStateException("No se pudo generar la vista previa", ex); }
    }
    private void auditar(String tipo, UUID tienda, UUID referencia, Object antes, Object despues) {
        var evento = new AuditoriaEvento();
        evento.setTipoEvento(tipo); evento.setReferenciaId(referencia);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tiendaId", tienda); payload.put("actor", auth == null ? "sin-contexto" : auth.getName());
        payload.put("anterior", antes); payload.put("nuevo", despues);
        try { evento.setPayloadJson(json.writeValueAsString(payload)); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("No se pudo registrar el cambio", ex); }
        auditoria.save(evento);
    }
}
