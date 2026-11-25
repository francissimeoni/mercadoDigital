package ar.com.mercadodigital.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.PublicStorefrontEventoRequest;
import ar.com.mercadodigital.backend.dto.response.RevendedorMetricaRankingResponse;
import ar.com.mercadodigital.backend.dto.response.RevendedorMetricasResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.entity.StorefrontEvento;
import ar.com.mercadodigital.backend.entity.StorefrontEventoTipo;
import ar.com.mercadodigital.backend.entity.TipoStorefront;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ProductoPreguntaRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.StorefrontEventoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorefrontMetricasService {

    private static final int MAX_DIAS = 366;
    private static final int RANKING_SIZE = 10;

    private final StorefrontService storefrontService;
    private final RevendedorRepository revendedorRepository;
    private final RevendedorProductoRepository productoRepository;
    private final ProductoPreguntaRepository preguntaRepository;
    private final StorefrontEventoRepository eventoRepository;

    @Transactional
    public void registrar(PublicStorefrontEventoRequest request) {
        StorefrontResponse storefront = storefrontService.resolverPorHost(request.getHost());
        if (!TipoStorefront.REVENDEDOR.equals(storefront.getTipoStorefront()) || storefront.getRevendedorId() == null) {
            return;
        }

        UUID revendedorId = storefront.getRevendedorId();
        Revendedor revendedor = revendedorRepository.findById(revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado"));
        StorefrontEvento evento = new StorefrontEvento();
        evento.setRevendedor(revendedor);
        evento.setTipoEvento(request.getTipo());
        evento.setVisitanteHash(hash(revendedorId + ":" + request.getVisitanteId().trim()));

        if (StorefrontEventoTipo.VISTA_PRODUCTO.equals(request.getTipo())) {
            if (request.getProductoId() == null) {
                throw new BadRequestException("El producto es obligatorio para registrar una vista");
            }
            RevendedorProducto producto = productoRepository
                    .findByIdAndRevendedor_Id(request.getProductoId(), revendedorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado en esta tienda"));
            evento.setProductoId(producto.getId());
            evento.setProductoNombre(producto.getNombre());
            evento.setRubroNombre(producto.getCategoriaProducto() == null ? "Sin categoria" : producto.getCategoriaProducto().getNombre());
        }

        eventoRepository.save(evento);
    }

    @Transactional(readOnly = true)
    public RevendedorMetricasResponse obtener(UUID revendedorId, LocalDate desde, LocalDate hasta) {
        LocalDate fechaHasta = hasta == null ? LocalDate.now() : hasta;
        LocalDate fechaDesde = desde == null ? fechaHasta.minusDays(29) : desde;
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new BadRequestException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        if (fechaHasta.isAfter(LocalDate.now())) throw new BadRequestException("La fecha hasta no puede ser futura");
        if (ChronoUnit.DAYS.between(fechaDesde, fechaHasta) >= MAX_DIAS) {
            throw new BadRequestException("El rango maximo permitido es de 366 dias");
        }

        LocalDateTime inicio = fechaDesde.atStartOfDay();
        LocalDateTime finExclusivo = fechaHasta.plusDays(1).atStartOfDay();
        long visitantes = eventoRepository
                .countDistinctVisitanteHashByRevendedor_IdAndTipoEventoAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        revendedorId, StorefrontEventoTipo.VISITA, inicio, finExclusivo);
        long visitas = eventoRepository
                .countByRevendedor_IdAndTipoEventoAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        revendedorId, StorefrontEventoTipo.VISITA, inicio, finExclusivo);
        long vistasProducto = eventoRepository
                .countByRevendedor_IdAndTipoEventoAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        revendedorId, StorefrontEventoTipo.VISTA_PRODUCTO, inicio, finExclusivo);
        long clicksWhatsapp = eventoRepository
                .countByRevendedor_IdAndTipoEventoAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        revendedorId, StorefrontEventoTipo.CLICK_WHATSAPP, inicio, finExclusivo);
        long consultas = preguntaRepository.countByRevendedor_IdAndFechaPreguntaGreaterThanEqualAndFechaPreguntaLessThan(
                revendedorId, inicio, finExclusivo);

        List<RevendedorMetricaRankingResponse> rubros = eventoRepository
                .rankingRubros(revendedorId, StorefrontEventoTipo.VISTA_PRODUCTO, inicio, finExclusivo,
                        PageRequest.of(0, RANKING_SIZE))
                .stream().map(item -> RevendedorMetricaRankingResponse.builder()
                        .nombre(item.getNombre()).total(item.getTotal()).build()).toList();
        var ranking = eventoRepository
                .rankingProductos(revendedorId, StorefrontEventoTipo.VISTA_PRODUCTO, inicio, finExclusivo,
                        PageRequest.of(0, RANKING_SIZE));
        var ids = ranking.stream().map(StorefrontEventoRepository.RankingProducto::getProductoId).distinct().toList();
        var imagenes = new java.util.HashMap<UUID, String>();
        if (!ids.isEmpty()) productoRepository.findByRevendedor_IdAndIdIn(revendedorId, ids).forEach(p -> {
            p.getMedia().stream().filter(m -> ar.com.mercadodigital.backend.entity.TipoMedia.IMAGEN.equals(m.getTipoMedia()))
                    .sorted(java.util.Comparator.comparing((ar.com.mercadodigital.backend.entity.RevendedorProductoMedia m) -> !m.isEsPrincipal())
                            .thenComparing(m -> m.getOrden() == null ? 0 : m.getOrden()).thenComparing(m -> m.getId().toString()))
                    .map(ar.com.mercadodigital.backend.entity.RevendedorProductoMedia::getUrl)
                    .filter(java.util.Objects::nonNull).findFirst().ifPresent(url -> imagenes.put(p.getId(), url));
        });
        List<RevendedorMetricaRankingResponse> productos = ranking.stream().map(item -> RevendedorMetricaRankingResponse.builder()
                .productoId(item.getProductoId()).nombre(item.getNombre()).total(item.getTotal())
                .imagenPrincipalUrl(imagenes.get(item.getProductoId())).build()).toList();

        return RevendedorMetricasResponse.builder()
                .desde(fechaDesde).hasta(fechaHasta).visitantesUnicos(visitantes).visitas(visitas)
                .vistasProductos(vistasProducto).consultas(consultas).clicksWhatsapp(clicksWhatsapp)
                .rubrosMasVistos(rubros).productosMasVistos(productos).build();
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }
}
