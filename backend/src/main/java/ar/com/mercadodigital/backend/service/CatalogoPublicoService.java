package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ar.com.mercadodigital.backend.dto.response.PublicCatalogoContactoResponse;
import ar.com.mercadodigital.backend.dto.response.PublicCatalogoContactoResponse.Contacto;
import ar.com.mercadodigital.backend.entity.CatalogoProductoConfiguracion;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.*;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoPublicoService {
    private final CatalogoConfiguracionRepository configuraciones;
    private final CatalogoProductoConfiguracionRepository precios;
    private final RevendedorStorefrontRepository storefronts;
    private final AsesorVentaRepository asesores;
    private final CatalogoPrecioCalculator calculator;

    public record Publicacion(RevendedorProducto producto, BigDecimal precioArs, String condicionesEntrega) { }

    public List<Publicacion> listado(UUID tienda) {
        var config = configuraciones.findByRevendedor_Id(tienda).orElse(null);
        BigDecimal cotizacion = config == null ? null : config.getCotizacionUsd();
        String condiciones = config == null ? null : config.getCondicionesEntrega();
        return precios.findByProducto_Revendedor_IdOrderByProducto_NombreAsc(tienda).stream()
                .filter(p -> tienda.equals(p.getProducto().getRevendedor().getId()))
                .filter(p -> p.getProducto().isActivo())
                .map(p -> publicacion(tienda, p, cotizacion, condiciones))
                .filter(p -> p != null).toList();
    }

    public Publicacion detalle(UUID tienda, UUID productoId) {
        var precio = precios.findByProducto_IdAndProducto_Revendedor_Id(productoId, tienda)
                .filter(p -> tienda.equals(p.getProducto().getRevendedor().getId()))
                .filter(p -> p.getProducto().isActivo())
                .orElseThrow(this::noPublicado);
        var config = configuraciones.findByRevendedor_Id(tienda).orElse(null);
        var result = publicacion(tienda, precio, config == null ? null : config.getCotizacionUsd(),
                config == null ? null : config.getCondicionesEntrega());
        if (result == null) throw noPublicado();
        return result;
    }

    public PublicCatalogoContactoResponse contacto(UUID tienda, UUID productoId, String host) {
        var publicacion = detalle(tienda, productoId);
        var producto = publicacion.producto();
        var sf = storefronts.findByRevendedor_Id(tienda).orElseThrow(this::noPublicado);
        List<Contacto> contactos = new ArrayList<>();
        String propio = telefono(sf.getTelefonoComercial());
        if (propio != null) contactos.add(new Contacto("responsable", nombreResponsable(sf), true, propio));
        asesores.findByRevendedor_IdOrderByNombreAsc(tienda).stream()
                .filter(a -> a.isActivo() && tienda.equals(a.getRevendedor().getId()))
                .forEach(a -> {
                    String numero = telefono(a.getWhatsappComercial());
                    if (numero != null) contactos.add(new Contacto(a.getId().toString(), a.getNombre(), false, numero,
                            a.getFotoUrl(), a.getProvincia(), a.getLocalidad(), a.getZonaAtencion()));
                });
        return new PublicCatalogoContactoResponse(producto.getId(), producto.getNombre(), producto.getSku(),
                publicacion.precioArs().toPlainString(), publicacion.condicionesEntrega(), host, List.copyOf(contactos));
    }

    private Publicacion publicacion(UUID tienda, CatalogoProductoConfiguracion p, BigDecimal cotizacion, String condiciones) {
        var categoria = p.getCategoriaEnvio();
        // Un vínculo ajeno o faltante nunca se convierte en un costo cero.
        BigDecimal costo = categoria != null && tienda.equals(categoria.getRevendedor().getId())
                ? categoria.getCostoArs() : null;
        var calculo = calculator.calcular(p.getMoneda(), p.getPrecioOriginal(), p.isIncluyeEnvio(), cotizacion, costo);
        return "LISTO".equals(calculo.estado())
                ? new Publicacion(p.getProducto(), calculo.precioPublicoArs(), condiciones) : null;
    }

    private String telefono(String valor) {
        try { return WhatsAppComercial.normalizar(valor); }
        catch (BadRequestException ex) { return null; }
    }

    private String nombreResponsable(RevendedorStorefront sf) {
        return sf.getNombreComercial() == null || sf.getNombreComercial().isBlank()
                ? "Responsable de la tienda" : sf.getNombreComercial();
    }

    private ResourceNotFoundException noPublicado() {
        return new ResourceNotFoundException("Este producto no está publicado en el catálogo de esta tienda.");
    }
}
