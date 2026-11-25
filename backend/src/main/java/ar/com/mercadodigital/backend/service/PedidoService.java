package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.service.CostoEnvioService;
import ar.com.mercadodigital.backend.service.CostoOperativoMdService;
import ar.com.mercadodigital.backend.service.DescuentoRevendedorService;
import ar.com.mercadodigital.backend.service.NotificationService;
import ar.com.mercadodigital.backend.service.OfertaProductoCanonicoService;
import ar.com.mercadodigital.backend.service.PricingService;
import ar.com.mercadodigital.backend.service.ProductoService;
import ar.com.mercadodigital.backend.service.PspConfigService;
import ar.com.mercadodigital.backend.service.RevendedorPricingV1Service;
import ar.com.mercadodigital.backend.service.UsuarioService;
import java.math.RoundingMode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.CrearPedidoCanonicoRequest;
import ar.com.mercadodigital.backend.dto.request.CrearPedidoRequest;
import ar.com.mercadodigital.backend.dto.request.PedidoItemCanonicoRequest;
import ar.com.mercadodigital.backend.dto.request.PedidoItemRequest;
import ar.com.mercadodigital.backend.dto.response.OfertaProductoResponse;
import ar.com.mercadodigital.backend.dto.response.PedidoResponse;
import ar.com.mercadodigital.backend.entity.AuditoriaEvento;
import ar.com.mercadodigital.backend.entity.Envio;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.OrigenPedido;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Producto;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.ProveedorStock;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorMarkup;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.entity.SplitPedidoItem;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.exception.SinStockException;
import ar.com.mercadodigital.backend.mapper.PedidoMapper;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.ProveedorStockRepository;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorMarkupRepository;
import ar.com.mercadodigital.backend.repository.RevendedorProductoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.SplitPedidoItemRepository;
import ar.com.mercadodigital.backend.service.postcompra.NotificacionInternaEmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {
    private final MercadoPagoClient mercadoPagoClient;

    private final PedidoRepository pedidoRepository;
    private final UsuarioService usuarioService;
    private final ProductoService productoService;
    private final RevendedorRepository revendedorRepository;
    private final ProveedorStockRepository proveedorStockRepository;
    private final RevendedorMarkupRepository revendedorMarkupRepository;
    private final RevendedorProductoRepository revendedorProductoRepository;
    private final SplitPedidoItemRepository splitPedidoItemRepository;
    private final OfertaProductoCanonicoService ofertaProductoCanonicoService;
    private final PricingService pricingService;
    private final PspConfigService pspConfigService;
    private final RevendedorPricingV1Service revendedorPricingV1Service;
    private final CostoOperativoMdService costoOperativoMdService;
    private final CostoEnvioService costoEnvioService;
    private final DescuentoRevendedorService descuentoRevendedorService;
    private final PedidoMapper pedidoMapper;
    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final NotificationService notificationService;
    private final NotificacionInternaEmailService notificacionInternaEmailService;
    private final CatalogoModoService modoCatalogo;
    private final RevendedorOwnershipService ownershipService;

    @Transactional
    public PedidoResponse crearPedido(CrearPedidoRequest request) {
        ownershipService.validarOwnership(request.getRevendedorId());
        return crearPedido(request, OrigenPedido.OPERATIVO, false);
    }

    @Transactional
    public PedidoResponse crearPedidoPublicoPorProducto(CrearPedidoRequest request) {
        return crearPedido(request, OrigenPedido.PUBLICO_DIRECTO, true);
    }

    private PedidoResponse crearPedido(CrearPedidoRequest request, OrigenPedido origenPedido, boolean usarTrackingToken) {
        mercadoPagoClient.validarMetodoPago(request.getMetodoPago());
        modoCatalogo.rechazarNuevaCompra(request.getRevendedorId());
        Usuario cliente = usuarioService.getEntity(request.getClienteId());
        Revendedor revendedor = revendedorRepository.findById(request.getRevendedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + request.getRevendedorId()));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRevendedor(revendedor);
        pedido.setEstadoPedido(PedidoEstado.CREADO);
        pedido.setOrigenPedido(origenPedido);
        if (usarTrackingToken) {
            pedido.setTrackingToken(generarTrackingToken(origenPedido));
        }

        BuildItemsResult buildItemsResult = buildItems(pedido, revendedor, request.getItems());
        pedido.setItems(buildItemsResult.items());
        pedido.setTotalBruto(buildItemsResult.totalBruto());
        pedido.setTotalPsp(buildItemsResult.totalPsp());
        pedido.setTotalEnvio(buildItemsResult.totalEnvio());
        pedido.setTotalSeguro(buildItemsResult.totalSeguro());
        pedido.setTotalDescuento(buildItemsResult.totalDescuento());
        pedido.setCodigoDescuentoAplicado(buildItemsResult.codigoDescuentoAplicado());
        pedido.setTotalFinal(buildItemsResult.totalFinal());
        pedido.setPago(buildPago(pedido, request.getMetodoPago(), buildItemsResult.totalFinal()));
        pedido.setEnvio(buildEnvio(pedido, request.getDireccionEnvio(), buildItemsResult.categoriaPaqueteEnvio(), buildItemsResult.totalEnvio()));

        Pedido saved = pedidoRepository.save(pedido);
        persistirSplits(saved.getItems(), buildItemsResult.splits());
        if (!OrigenPedido.PUBLICO_DIRECTO.equals(saved.getOrigenPedido())) {
            notificarActoresOperativosPedidoCreado(saved);
        }
        return pedidoMapper.toResponse(saved);
    }

    @Transactional
    public PedidoResponse crearPedidoCanonico(CrearPedidoCanonicoRequest request) {
        ownershipService.validarOwnership(request.getRevendedorId());
        return crearPedidoCanonico(request, "SIN_DIRECCION");
    }

    @Transactional
    public PedidoResponse crearPedidoCanonicoPublico(CrearPedidoCanonicoRequest request, String direccionEnvio) {
        return crearPedidoCanonico(request, direccionEnvio, OrigenPedido.PUBLICO_DIRECTO);
    }

    private PedidoResponse crearPedidoCanonico(CrearPedidoCanonicoRequest request, String direccionEnvio) {
        return crearPedidoCanonico(request, direccionEnvio, OrigenPedido.OPERATIVO);
    }

    private PedidoResponse crearPedidoCanonico(CrearPedidoCanonicoRequest request, String direccionEnvio, OrigenPedido origenPedido) {
        mercadoPagoClient.exigirHabilitado();
        modoCatalogo.rechazarNuevaCompra(request.getRevendedorId());
        Usuario cliente = usuarioService.getEntity(request.getClienteId());
        Revendedor revendedor = revendedorRepository.findById(request.getRevendedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + request.getRevendedorId()));
        boolean compraInternaRevendedor = OrigenPedido.OPERATIVO.equals(origenPedido);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRevendedor(revendedor);
        pedido.setEstadoPedido(PedidoEstado.CREADO);
        pedido.setOrigenPedido(origenPedido);
        pedido.setTrackingToken(generarTrackingToken(origenPedido));

        BuildItemsResult buildItemsResult = buildItemsCanonicos(
                pedido,
                revendedor,
                request.getItems(),
                request.getCodigoDescuento(),
                compraInternaRevendedor);
        pedido.setItems(buildItemsResult.items());
        pedido.setTotalBruto(buildItemsResult.totalBruto());
        pedido.setTotalPsp(buildItemsResult.totalPsp());
        pedido.setTotalEnvio(buildItemsResult.totalEnvio());
        pedido.setTotalSeguro(buildItemsResult.totalSeguro());
        pedido.setTotalDescuento(buildItemsResult.totalDescuento());
        pedido.setCodigoDescuentoAplicado(buildItemsResult.codigoDescuentoAplicado());
        pedido.setTotalFinal(buildItemsResult.totalFinal());
        pedido.setPago(buildPago(pedido, "PSP", buildItemsResult.totalFinal()));
        pedido.setEnvio(buildEnvio(pedido, direccionEnvio, buildItemsResult.categoriaPaqueteEnvio(), buildItemsResult.totalEnvio()));

        Pedido saved = pedidoRepository.save(pedido);
        persistirSplits(saved.getItems(), buildItemsResult.splits());
        registrarAuditoria("STOCK_RESERVADO", saved.getId(), payloadItems(saved.getId(), saved.getItems()));
        if (!OrigenPedido.PUBLICO_DIRECTO.equals(saved.getOrigenPedido())) {
            notificarActoresOperativosPedidoCreado(saved);
        }
        return pedidoMapper.toResponse(saved);
    }

    private void notificarActoresOperativosPedidoCreado(Pedido pedido) {
        if (pedido.getRevendedor() != null && pedido.getRevendedor().getUsuario() != null) {
            notificationService.createNotificationForUser(
                    pedido.getRevendedor().getUsuario(),
                    "PEDIDO_CREADO_REVENDEDOR",
                    "Nueva venta registrada",
                    "Se registro una nueva venta en tu storefront. Pedido " + pedido.getId() + ".");
        }

        notificationService.createNotificationsForUsers(
                pedido.getItems().stream()
                        .map(PedidoItem::getProveedor)
                        .filter(Objects::nonNull)
                        .map(Proveedor::getUsuario)
                        .filter(Objects::nonNull)
                        .toList(),
                "PEDIDO_CREADO_PROVEEDOR",
                "Nuevo pedido para preparar",
                "Ingreso un nuevo pedido para preparar en la operacion de Mercado Digital.");

        registrarAuditoria("NOTIFICACIONES_PEDIDO_CREADO_ADMIN", pedido.getId(),
                "{\"pedidoId\":\"" + pedido.getId()
                        + "\",\"revendedorId\":\""
                        + (pedido.getRevendedor() != null ? pedido.getRevendedor().getId() : "")
                        + "\",\"notificaRevendedor\":"
                        + (pedido.getRevendedor() != null && pedido.getRevendedor().getUsuario() != null)
                        + ",\"notificaProveedor\":true,\"origen\":\"ADMIN_MD\"}");
        notificacionInternaEmailService.notificarVentaInterna(pedido);
    }

    @Transactional
    public PedidoResponse cancelarPedido(UUID id) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
        validarAccesoAlPedido(pedido);
        if (!esCancelable(pedido.getEstadoPedido()) || pedido.getPago() == null
                || (pedido.getPago().getEstadoPago() != PagoEstado.PENDIENTE
                    && pedido.getPago().getEstadoPago() != PagoEstado.RECHAZADO)) {
            throw new OperacionEnConflictoException(
                    "No se puede cancelar el pedido en su estado actual");
        }

        for (PedidoItem item : pedido.getItems()) {
            if (item.getRevendedorProducto() != null) {
                RevendedorProducto productoPropio = revendedorProductoRepository.findForUpdate(
                                item.getRevendedorProducto().getId(), pedido.getRevendedor().getId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Producto propio no encontrado: " + item.getRevendedorProducto().getId()));
                productoPropio.setStock(productoPropio.getStock() + item.getCantidad());
                revendedorProductoRepository.save(productoPropio);
                continue;
            }
            ProveedorStock stockBloqueado = proveedorStockRepository
                    .findByProductoIdForUpdate(item.getProducto().getId()).stream().findFirst()
                    .orElseThrow(() -> new SinStockException(
                            "No existe proveedor_stock para producto: " + item.getProducto().getId()));
            stockBloqueado.setStock(stockBloqueado.getStock() + item.getCantidad());
            proveedorStockRepository.save(stockBloqueado);
        }

        pedido.setEstadoPedido(PedidoEstado.CANCELADO);
        pedido.getPago().setEstadoPago(PagoEstado.CANCELADO);
        if (pedido.getEnvio() != null) {
            pedido.getEnvio().setEstadoEnvio(EnvioEstado.CANCELADO);
        }
        Pedido pedidoCancelado = pedidoRepository.save(pedido);
        registrarAuditoria("PEDIDO_CANCELADO", pedidoCancelado.getId(),
                "{\"pedidoId\":\"" + pedidoCancelado.getId() + "\",\"estadoPedido\":\"CANCELADO\"}");
        registrarAuditoria("STOCK_REPUESTO", pedidoCancelado.getId(),
                payloadItems(pedidoCancelado.getId(), pedidoCancelado.getItems()));
        return pedidoMapper.toResponse(pedidoCancelado);
    }

    @Transactional(readOnly = true)
    public PedidoResponse getById(UUID id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
        validarAccesoAlPedido(pedido);
        return pedidoMapper.toResponse(pedido);
    }

    private void validarAccesoAlPedido(Pedido pedido) {
        if (pedido.getRevendedor() == null) {
            throw new ResourceNotFoundException("Pedido no encontrado");
        }
        ownershipService.validarOwnership(pedido.getRevendedor().getId());
    }

    private BuildItemsResult buildItems(Pedido pedido, Revendedor revendedor, List<PedidoItemRequest> requestItems) {
        List<PedidoItem> items = new ArrayList<>();
        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalPsp = BigDecimal.ZERO;
        BigDecimal totalEnvio = BigDecimal.ZERO;
        BigDecimal totalSeguro = BigDecimal.ZERO;
        BigDecimal totalFinal = BigDecimal.ZERO;
        List<SplitDraft> splits = new ArrayList<>();
        String categoriaPaqueteEnvio = null;

        for (PedidoItemRequest reqItem : requestItems) {
            Producto producto = productoService.getEntity(reqItem.getProductoId());
            ProveedorStock stockActual = proveedorStockRepository
                    .findFirstByProveedor_IdAndProducto_IdOrderByUpdatedAtDesc(
                            producto.getProveedor().getId(), producto.getId())
                    .orElse(null);
            RevendedorMarkup markup = revendedorMarkupRepository
                    .findByRevendedor_IdAndProducto_IdAndActivoTrue(revendedor.getId(), producto.getId())
                    .orElse(null);

            BigDecimal costoProveedorUnit = stockActual != null ? stockActual.getPrecioCostoActual() : producto.getPrecioCosto();
            BigDecimal costoMdUnit = costoOperativoMdService.getMontoParaCategoria(producto.getCategoriaPaquete(), costoProveedorUnit);
            BigDecimal costoEnvioUnit = costoEnvioService.getMontoParaCategoria(producto.getCategoriaPaquete());
            BigDecimal markupPorcentaje = markup != null ? markup.getMarkupPorcentaje() : null;
            BigDecimal markupFijo = markup != null ? markup.getMarkupFijo() : null;
            PricingService.PricingDetalle detalleUnitario = pricingService.calcularPrecioItem(
                    costoProveedorUnit,
                    costoMdUnit,
                    markupPorcentaje,
                    markupFijo,
                    BigDecimal.ZERO,
                    reqItem.isTieneSeguro());
            PedidoItem item = new PedidoItem();
            item.setPedido(pedido);
            item.setProducto(producto);
            item.setProveedor(producto.getProveedor());
            item.setCantidad(reqItem.getCantidad());
            item.setPrecioCosto(costoProveedorUnit);
            item.setMarkupAplicado(detalleUnitario.margenRevendedor());
            item.setPrecioItemFinal(detalleUnitario.precioItemFinal());
            item.setTieneSeguro(reqItem.isTieneSeguro());
            item.setMontoSeguro(detalleUnitario.montoSeguro());
            items.add(item);

            BigDecimal cantidad = BigDecimal.valueOf(reqItem.getCantidad());
            totalBruto = totalBruto.add(costoProveedorUnit.add(costoMdUnit).add(detalleUnitario.margenRevendedor()).multiply(cantidad));
            totalPsp = totalPsp.add(detalleUnitario.costoPsp().multiply(cantidad));
            totalEnvio = totalEnvio.add(costoEnvioUnit.multiply(cantidad));
            totalSeguro = totalSeguro.add(detalleUnitario.montoSeguro().multiply(cantidad));
            totalFinal = totalFinal.add(detalleUnitario.precioItemFinal().multiply(cantidad))
                    .add(costoEnvioUnit.multiply(cantidad))
                    .add(detalleUnitario.montoSeguro().multiply(cantidad));
            categoriaPaqueteEnvio = costoEnvioService.resolverCategoriaDominante(categoriaPaqueteEnvio, producto.getCategoriaPaquete());
            splits.add(buildSplitDraft(item, detalleUnitario, costoMdUnit, costoEnvioUnit));
        }
        return aplicarDescuentoRevendedor(items, splits, revendedor, null, totalBruto, totalPsp, totalEnvio, totalSeguro, totalFinal, categoriaPaqueteEnvio);
    }

    private BuildItemsResult buildItemsCanonicos(Pedido pedido, Revendedor revendedor,
            List<PedidoItemCanonicoRequest> requestItems, String codigoDescuento, boolean compraInternaRevendedor) {
        List<PedidoItem> items = new ArrayList<>();
        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalPsp = BigDecimal.ZERO;
        BigDecimal totalEnvio = BigDecimal.ZERO;
        BigDecimal totalSeguro = BigDecimal.ZERO;
        BigDecimal totalFinal = BigDecimal.ZERO;
        List<SplitDraft> splits = new ArrayList<>();
        String categoriaPaqueteEnvio = null;

        for (PedidoItemCanonicoRequest reqItem : requestItems) {
            OfertaProductoResponse oferta = seleccionarMejorOfertaConStock(
                    reqItem.getProductoCanonicoId(), revendedor.getId(), reqItem.getCantidad(), compraInternaRevendedor);
            Producto producto = productoService.getEntity(oferta.getProductoId());

            ProveedorStock stockActual = proveedorStockRepository
                    .findByProductoIdForUpdate(producto.getId()).stream().findFirst()
                    .orElseThrow(() -> new SinStockException(
                            "Sin stock para producto canonico: " + reqItem.getProductoCanonicoId()));
            if (stockActual.getStock() < reqItem.getCantidad()) {
                throw new SinStockException("Stock insuficiente para producto canonico: "
                        + reqItem.getProductoCanonicoId() + ". Disponible: " + stockActual.getStock()
                        + ", solicitado: " + reqItem.getCantidad());
            }
            stockActual.setStock(stockActual.getStock() - reqItem.getCantidad());
            proveedorStockRepository.save(stockActual);
            BigDecimal costoProveedorUnit = stockActual != null ? stockActual.getPrecioCostoActual() : producto.getPrecioCosto();
            BigDecimal costoMdUnit = costoOperativoMdService.getMontoParaCategoria(producto.getCategoriaPaquete(), costoProveedorUnit);
            BigDecimal costoEnvioUnit = costoEnvioService.getMontoParaCategoria(producto.getCategoriaPaquete());
            PricingService.PricingDetalle detalleUnitario = compraInternaRevendedor
                    ? pricingService.calcularCostoOperativoItem(costoProveedorUnit, costoMdUnit, BigDecimal.ZERO, reqItem.isTieneSeguro())
                    : calcularPrecioComercialRevendedor(revendedor, producto.getId(), costoProveedorUnit, costoMdUnit, costoEnvioUnit, reqItem.isTieneSeguro());

            PedidoItem item = new PedidoItem();
            item.setPedido(pedido);
            item.setProducto(producto);
            item.setProveedor(producto.getProveedor());
            item.setCantidad(reqItem.getCantidad());
            item.setPrecioCosto(costoProveedorUnit);
            item.setMarkupAplicado(detalleUnitario.margenRevendedor());
            item.setPrecioItemFinal(detalleUnitario.precioItemFinal());
            item.setTieneSeguro(reqItem.isTieneSeguro());
            item.setMontoSeguro(detalleUnitario.montoSeguro());
            items.add(item);

            BigDecimal cantidad = BigDecimal.valueOf(reqItem.getCantidad());
            totalBruto = totalBruto.add(costoProveedorUnit.add(costoMdUnit).add(detalleUnitario.margenRevendedor()).multiply(cantidad));
            totalPsp = totalPsp.add(detalleUnitario.costoPsp().multiply(cantidad));
            totalEnvio = totalEnvio.add(costoEnvioUnit.multiply(cantidad));
            totalSeguro = totalSeguro.add(detalleUnitario.montoSeguro().multiply(cantidad));
            totalFinal = totalFinal.add(detalleUnitario.precioItemFinal().multiply(cantidad))
                    .add(costoEnvioUnit.multiply(cantidad))
                    .add(detalleUnitario.montoSeguro().multiply(cantidad));
            categoriaPaqueteEnvio = costoEnvioService.resolverCategoriaDominante(categoriaPaqueteEnvio, producto.getCategoriaPaquete());
            splits.add(buildSplitDraft(item, detalleUnitario, costoMdUnit, costoEnvioUnit));
        }
        return aplicarDescuentoRevendedor(
                items,
                splits,
                revendedor,
                codigoDescuento,
                totalBruto,
                totalPsp,
                totalEnvio,
                totalSeguro,
                totalFinal,
                categoriaPaqueteEnvio);
    }

    private PricingService.PricingDetalle calcularPrecioComercialRevendedor(Revendedor revendedor, UUID productoId,
            BigDecimal costoProveedorUnit, BigDecimal costoMdUnit, BigDecimal costoEnvioUnit, boolean tieneSeguro) {
        Producto producto = productoService.getEntity(productoId);
        RevendedorPricingV1Service.PricingRuleResolution pricingRule = revendedorPricingV1Service
                .resolvePricingRule(revendedor.getId(), producto);
        return pricingService.calcularPrecioItem(
                costoProveedorUnit,
                costoMdUnit,
                pricingRule.markupPorcentaje(),
                pricingRule.markupFijo(),
                BigDecimal.ZERO,
                tieneSeguro);
    }

    private OfertaProductoResponse seleccionarMejorOfertaConStock(UUID productoCanonicoId, UUID revendedorId, Integer cantidad,
            boolean compraInternaRevendedor) {
        List<OfertaProductoResponse> ofertas = compraInternaRevendedor
                ? ofertaProductoCanonicoService.getOfertasInternasConStock(productoCanonicoId, revendedorId)
                : ofertaProductoCanonicoService.getOfertasConStock(productoCanonicoId, revendedorId);
        if (ofertas.isEmpty()) {
            throw new SinStockException("Sin stock para producto canonico: " + productoCanonicoId);
        }

        OfertaProductoResponse mejorOferta = ofertas.get(0);
        if (cantidad > mejorOferta.getStockDisponible()) {
            throw new SinStockException("Stock insuficiente para producto canonico: " + productoCanonicoId
                    + ". Disponible: " + mejorOferta.getStockDisponible() + ", solicitado: " + cantidad);
        }
        return mejorOferta;
    }

    private boolean esCancelable(PedidoEstado estadoPedido) {
        return PedidoEstado.CREADO.equals(estadoPedido) || "PENDIENTE_PAGO".equals(estadoPedido.name());
    }

    private void registrarAuditoria(String tipoEvento, UUID referenciaId, String payloadJson) {
        AuditoriaEvento evento = new AuditoriaEvento();
        evento.setTipoEvento(tipoEvento);
        evento.setReferenciaId(referenciaId);
        evento.setPayloadJson(payloadJson);
        auditoriaEventoRepository.save(evento);
    }

    private String payloadItems(UUID pedidoId, List<PedidoItem> items) {
        String payloadItems = items.stream()
                .map(item -> item.getRevendedorProducto() != null
                        ? "{\"revendedorProductoId\":\"" + item.getRevendedorProducto().getId()
                                + "\",\"cantidad\":" + item.getCantidad() + "}"
                        : "{\"productoId\":\"" + item.getProducto().getId()
                                + "\",\"cantidad\":" + item.getCantidad() + "}")
                .collect(Collectors.joining(","));
        return "{\"pedidoId\":\"" + pedidoId + "\",\"items\":[" + payloadItems + "]}";
    }

    private void persistirSplits(List<PedidoItem> items, List<SplitDraft> splitDrafts) {
        if (items.size() != splitDrafts.size()) {
            throw new IllegalStateException("La cantidad de items y splits no coincide.");
        }
        List<SplitPedidoItem> splits = IntStream.range(0, items.size())
                .mapToObj(index -> toSplit(items.get(index), splitDrafts.get(index)))
                .toList();
        splitPedidoItemRepository.saveAll(splits);
    }

    private SplitDraft buildSplitDraft(PedidoItem item, PricingService.PricingDetalle detalleUnitario, BigDecimal costoMdUnit,
            BigDecimal costoEnvioUnit) {
        BigDecimal cantidad = BigDecimal.valueOf(item.getCantidad());
        BigDecimal montoEnvio = costoEnvioUnit.multiply(cantidad);
        boolean proveedorDespacha = item.getProveedor() != null && item.getProveedor().isDespachaMercaderia();
        BigDecimal montoProveedorBase = item.getPrecioCosto().multiply(cantidad);
        BigDecimal montoProveedor = proveedorDespacha ? montoProveedorBase.add(montoEnvio) : montoProveedorBase;
        return new SplitDraft(
                montoProveedor,
                item.getMarkupAplicado().multiply(cantidad),
                costoMdUnit.multiply(cantidad),
                montoEnvio,
                item.getMontoSeguro().multiply(cantidad),
                detalleUnitario.costoPsp().multiply(cantidad));
    }

    private SplitPedidoItem toSplit(PedidoItem item, SplitDraft splitDraft) {
        SplitPedidoItem split = new SplitPedidoItem();
        split.setId(UUID.randomUUID());
        split.setPedidoItem(item);
        split.setMontoProveedor(splitDraft.montoProveedor());
        split.setMontoRevendedor(splitDraft.montoRevendedor());
        split.setMontoMd(splitDraft.montoMd());
        split.setMontoEnvio(splitDraft.montoEnvio());
        split.setMontoSeguro(splitDraft.montoSeguro());
        split.setMontoPsp(splitDraft.montoPsp());
        return split;
    }

    private BuildItemsResult aplicarDescuentoRevendedor(
            List<PedidoItem> items,
            List<SplitDraft> splits,
            Revendedor revendedor,
            String codigoDescuento,
            BigDecimal totalBruto,
            BigDecimal totalPsp,
            BigDecimal totalEnvio,
            BigDecimal totalSeguro,
            BigDecimal totalFinal,
            String categoriaPaqueteEnvio) {
        BigDecimal totalMarkup = splits.stream()
                .map(SplitDraft::montoRevendedor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        DescuentoRevendedorService.DescuentoAplicado descuento = descuentoRevendedorService
                .resolveDescuento(revendedor, codigoDescuento, totalMarkup);
        if (descuento.monto().signum() <= 0 || splits.isEmpty()) {
            return new BuildItemsResult(
                    items,
                    splits,
                    totalBruto,
                    totalPsp,
                    totalEnvio,
                    totalSeguro,
                    BigDecimal.ZERO,
                    totalFinal,
                    categoriaPaqueteEnvio,
                    null);
        }

        List<SplitDraft> splitsConDescuento = distribuirDescuentoEnSplits(splits, descuento.monto());
        BigDecimal totalFinalConDescuento = totalFinal.subtract(descuento.monto()).max(BigDecimal.ZERO);
        return new BuildItemsResult(
                items,
                splitsConDescuento,
                totalBruto,
                totalPsp,
                totalEnvio,
                totalSeguro,
                descuento.monto(),
                totalFinalConDescuento,
                categoriaPaqueteEnvio,
                descuento.codigoAplicado());
    }

    private List<SplitDraft> distribuirDescuentoEnSplits(List<SplitDraft> splits, BigDecimal descuentoTotal) {
        BigDecimal pendiente = descuentoTotal;
        List<SplitDraft> resultado = new ArrayList<>();

        for (int index = 0; index < splits.size(); index++) {
            SplitDraft split = splits.get(index);
            BigDecimal descuentoItem;
            if (index == splits.size() - 1) {
                descuentoItem = pendiente.min(split.montoRevendedor()).max(BigDecimal.ZERO);
            } else {
                BigDecimal proporcion = split.montoRevendedor().divide(
                        splits.stream().map(SplitDraft::montoRevendedor).reduce(BigDecimal.ZERO, BigDecimal::add),
                        10,
                        java.math.RoundingMode.HALF_UP);
                descuentoItem = descuentoTotal.multiply(proporcion).setScale(2, java.math.RoundingMode.HALF_UP)
                        .min(split.montoRevendedor())
                        .max(BigDecimal.ZERO);
                pendiente = pendiente.subtract(descuentoItem);
            }

            resultado.add(new SplitDraft(
                    split.montoProveedor(),
                    split.montoRevendedor().subtract(descuentoItem).max(BigDecimal.ZERO),
                    split.montoMd(),
                    split.montoEnvio(),
                    split.montoSeguro(),
                    split.montoPsp()));
        }

        return resultado;
    }

    private Pago buildPago(Pedido pedido, String metodoPago, BigDecimal monto) {
        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setEstadoPago(PagoEstado.PENDIENTE);
        pago.setMetodo(metodoPago);
        pago.setMonto(monto);
        return pago;
    }

    private Envio buildEnvio(Pedido pedido, String direccionEnvio, String categoriaPaquete, BigDecimal costoEnvio) {
        Envio envio = new Envio();
        envio.setPedido(pedido);
        envio.setCategoriaPaquete(categoriaPaquete);
        envio.setEstadoEnvio(OrigenPedido.PUBLICO_DIRECTO.equals(pedido.getOrigenPedido())
                ? EnvioEstado.PENDIENTE_PAGO
                : EnvioEstado.PENDIENTE_PREPARACION);
        envio.setDireccionEnvio(direccionEnvio);
        envio.setCostoEnvio(costoEnvio != null ? costoEnvio : BigDecimal.ZERO);
        return envio;
    }

    private String generarTrackingToken(OrigenPedido origenPedido) {
        if (!OrigenPedido.PUBLICO_DIRECTO.equals(origenPedido)) {
            return null;
        }
        return UUID.randomUUID().toString();
    }

    private record BuildItemsResult(
            List<PedidoItem> items,
            List<SplitDraft> splits,
            BigDecimal totalBruto,
            BigDecimal totalPsp,
            BigDecimal totalEnvio,
            BigDecimal totalSeguro,
            BigDecimal totalDescuento,
            BigDecimal totalFinal,
            String categoriaPaqueteEnvio,
            String codigoDescuentoAplicado) {
    }

    private record SplitDraft(
            BigDecimal montoProveedor,
            BigDecimal montoRevendedor,
            BigDecimal montoMd,
            BigDecimal montoEnvio,
            BigDecimal montoSeguro,
            BigDecimal montoPsp) {
    }


    @Transactional
    public PedidoResponse crearPedidoProductoPropio(UUID clienteId, UUID revendedorId, UUID revendedorProductoId, int cantidad, boolean tieneSeguro, String direccionEnvio, String metodoPago, BigDecimal recargoPspFactor) {
        mercadoPagoClient.validarMetodoPago(metodoPago);
        modoCatalogo.rechazarNuevaCompra(revendedorId);
        Usuario cliente = this.usuarioService.getEntity(clienteId);
        Revendedor revendedor = (Revendedor)this.revendedorRepository.findById(revendedorId).orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + String.valueOf(revendedorId)));
        RevendedorProducto producto = this.revendedorProductoRepository.findForUpdate(revendedorProductoId, revendedorId).filter(item -> item.isActivo()).orElseThrow(() -> new ResourceNotFoundException("Producto propio no disponible: " + String.valueOf(revendedorProductoId)));
        if (producto.getPrecioVenta() == null || producto.getCategoriaPaquete() == null) {
            throw new ar.com.mercadodigital.backend.exception.BadRequestException("Este producto no está configurado para el circuito de compra anterior.");
        }
        if (producto.getStock() == null || producto.getStock() < cantidad) {
            throw new SinStockException("Stock insuficiente para producto propio: " + String.valueOf(revendedorProductoId));
        }
        BigDecimal unidades = BigDecimal.valueOf(cantidad);
        BigDecimal montoProducto = producto.getPrecioVenta().multiply(unidades);
        BigDecimal envioUnitario = this.costoEnvioService.getMontoParaCategoria(producto.getCategoriaPaquete());
        BigDecimal totalEnvio = envioUnitario.multiply(unidades);
        BigDecimal seguroUnitario = tieneSeguro ? producto.getPrecioVenta().multiply(this.pspConfigService.getSeguroTransporteFactor()) : BigDecimal.ZERO;
        BigDecimal totalSeguro = seguroUnitario.multiply(unidades);
        BigDecimal baseUnitaria = producto.getPrecioVenta().add(envioUnitario).add(seguroUnitario);
        BigDecimal recargoPspUnitario = "PSP".equals(metodoPago) && recargoPspFactor != null ? baseUnitaria.multiply(recargoPspFactor).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal totalPsp = recargoPspUnitario.multiply(unidades);
        BigDecimal totalFinal = montoProducto.add(totalEnvio).add(totalSeguro).add(totalPsp);
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setRevendedor(revendedor);
        pedido.setEstadoPedido(PedidoEstado.CREADO);
        pedido.setOrigenPedido(OrigenPedido.PUBLICO_DIRECTO);
        pedido.setTrackingToken(this.generarTrackingToken(OrigenPedido.PUBLICO_DIRECTO));
        pedido.setTotalBruto(montoProducto);
        pedido.setTotalPsp(totalPsp);
        pedido.setTotalEnvio(totalEnvio);
        pedido.setTotalSeguro(totalSeguro);
        pedido.setTotalDescuento(BigDecimal.ZERO);
        pedido.setTotalFinal(totalFinal);
        PedidoItem item2 = new PedidoItem();
        item2.setPedido(pedido);
        item2.setRevendedorProducto(producto);
        item2.setCantidad(cantidad);
        item2.setPrecioCosto(producto.getPrecioVenta());
        item2.setMarkupAplicado(BigDecimal.ZERO);
        item2.setPrecioItemFinal(baseUnitaria.add(recargoPspUnitario));
        item2.setTieneSeguro(tieneSeguro);
        item2.setMontoSeguro(seguroUnitario);
        pedido.setItems(List.of(item2));
        pedido.setPago(this.buildPago(pedido, metodoPago, totalFinal));
        pedido.setEnvio(this.buildEnvio(pedido, direccionEnvio, producto.getCategoriaPaquete(), totalEnvio));
        producto.setStock(producto.getStock() - cantidad);
        this.revendedorProductoRepository.save(producto);
        return this.pedidoMapper.toResponse(this.pedidoRepository.save(pedido));
    }
}



