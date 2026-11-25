package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.service.PedidoService;
import ar.com.mercadodigital.backend.service.PspConfigService;
import ar.com.mercadodigital.backend.service.PublicCatalogoService;
import ar.com.mercadodigital.backend.service.RevendedorMercadoPagoService;
import ar.com.mercadodigital.backend.service.StorefrontService;
import ar.com.mercadodigital.backend.service.TransferenciaService;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.com.mercadodigital.backend.dto.request.CrearPedidoCanonicoRequest;
import ar.com.mercadodigital.backend.dto.request.CrearPedidoRequest;
import ar.com.mercadodigital.backend.dto.request.PedidoItemCanonicoRequest;
import ar.com.mercadodigital.backend.dto.request.PedidoItemRequest;
import ar.com.mercadodigital.backend.dto.request.PublicPedidoDirectoRequest;
import ar.com.mercadodigital.backend.dto.response.PedidoResponse;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoDirectoResponse;
import ar.com.mercadodigital.backend.dto.response.PublicProductoDetalleResponse;
import ar.com.mercadodigital.backend.entity.AsesorVenta;
import ar.com.mercadodigital.backend.entity.ClienteRevendedor;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.AsesorVentaRepository;
import ar.com.mercadodigital.backend.repository.ClienteRevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import ar.com.mercadodigital.backend.service.postcompra.EventoPedidoCreado;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicPedidoDirectoService {
    private final MercadoPagoClient mercadoPagoClient;
    private final CatalogoModoService modoCatalogo;

    private static final int DEMORA_DEFAULT_DIAS = 5;
    private static final String ORIGEN_ALTA_COMPRA_PUBLICA = "COMPRA_PUBLICA_DIRECTA_V1";

    private final StorefrontService storefrontService;
    private final PublicCatalogoService publicCatalogoService;
    private final PedidoService pedidoService;
    private final UsuarioRepository usuarioRepository;
    private final RevendedorRepository revendedorRepository;
    private final RevendedorStorefrontRepository revendedorStorefrontRepository;
    private final ClienteRevendedorRepository clienteRevendedorRepository;
    private final AsesorVentaRepository asesorVentaRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransferenciaService transferenciaService;

    @Transactional
    public PublicPedidoDirectoResponse crearPedidoDirecto(PublicPedidoDirectoRequest request) {
        // Antes de buscar/crear clientes, vínculos, pedidos o pagos.
        modoCatalogo.rechazarNuevaCompra(request.getRevendedorId());
        mercadoPagoClient.validarMetodoPago(request.getRevendedorProductoId() != null ? normalizarMetodoPago(request) : "PSP");
        if (request.getRevendedorProductoId() != null) {
            return crearPedidoDirectoProductoPropio(request);
        }
        if (request.getProveedorId() != null && request.getProductoId() != null) {
            return crearPedidoDirectoProveedor(request);
        }
        return crearPedidoDirectoRevendedor(request);
    }

    private PublicPedidoDirectoResponse crearPedidoDirectoProductoPropio(PublicPedidoDirectoRequest request) {
        this.storefrontService.validarRevendedorPublico(request.getRevendedorId());
        this.validarAsesorVenta(request);
        PublicProductoDetalleResponse producto = this.publicCatalogoService.getProductoDetalle(request.getRevendedorProductoId(), request.getRevendedorId());
        if (!"PROPIO_REVENDEDOR".equals(producto.getOrigen())) {
            throw new ResourceNotFoundException("Producto propio no disponible: " + String.valueOf(request.getRevendedorProductoId()));
        }
        Revendedor revendedor = (Revendedor)this.revendedorRepository.findById(request.getRevendedorId()).orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + String.valueOf(request.getRevendedorId())));
        Usuario cliente = this.buscarOCrearCliente(request);
        this.asegurarClienteRevendedor(cliente, revendedor);
        PedidoResponse pedido = this.pedidoService.crearPedidoProductoPropio(cliente.getId(), revendedor.getId(), request.getRevendedorProductoId(), request.getCantidad(), request.isSeguroTransporte(), this.formatearDireccion(request), this.normalizarMetodoPago(request), this.resolverRecargoMercadoPago(request));
        return this.finalizarPedido(pedido, producto.getDemoraEstimadaDias(), revendedor, cliente, request);
    }

    private PublicPedidoDirectoResponse crearPedidoDirectoRevendedor(PublicPedidoDirectoRequest request) {
        storefrontService.validarRevendedorPublico(request.getRevendedorId());
        validarAsesorVenta(request);

        PublicProductoDetalleResponse producto = publicCatalogoService
                .getProductoDetalle(request.getProductoCanonicoId(), request.getRevendedorId());

        Revendedor revendedor = revendedorRepository.findById(request.getRevendedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Revendedor no encontrado: " + request.getRevendedorId()));

        Usuario cliente = buscarOCrearCliente(request);
        asegurarClienteRevendedor(cliente, revendedor);

        CrearPedidoCanonicoRequest crearPedidoRequest = new CrearPedidoCanonicoRequest();
        crearPedidoRequest.setClienteId(cliente.getId());
        crearPedidoRequest.setRevendedorId(revendedor.getId());
        crearPedidoRequest.setCodigoDescuento(request.getCodigoDescuento());
        crearPedidoRequest.setItems(List.of(toPedidoItemCanonicoRequest(request)));

        PedidoResponse pedido = pedidoService.crearPedidoCanonicoPublico(crearPedidoRequest, formatearDireccion(request));
        return finalizarPedido(pedido, producto.getDemoraEstimadaDias(), revendedor, cliente, request);
    }

    private PublicPedidoDirectoResponse crearPedidoDirectoProveedor(PublicPedidoDirectoRequest request) {
        storefrontService.validarProveedorPublico(request.getProveedorId());
        PublicProductoDetalleResponse producto = publicCatalogoService
                .getProductoDetalleProveedor(request.getProductoId(), request.getProveedorId());

        Revendedor revendedor = resolverRevendedorShadowProveedor(request.getProveedorId());
        Usuario cliente = buscarOCrearCliente(request);
        asegurarClienteRevendedor(cliente, revendedor);

        CrearPedidoRequest crearPedidoRequest = new CrearPedidoRequest();
        crearPedidoRequest.setClienteId(cliente.getId());
        crearPedidoRequest.setRevendedorId(revendedor.getId());
        crearPedidoRequest.setMetodoPago("PSP");
        crearPedidoRequest.setDireccionEnvio(formatearDireccion(request));
        crearPedidoRequest.setItems(List.of(toPedidoItemRequest(request)));

        PedidoResponse pedido = pedidoService.crearPedidoPublicoPorProducto(crearPedidoRequest);
        return finalizarPedido(pedido, producto.getDemoraEstimadaDias(), revendedor, cliente, request);
    }

    private PedidoItemCanonicoRequest toPedidoItemCanonicoRequest(PublicPedidoDirectoRequest request) {
        PedidoItemCanonicoRequest item = new PedidoItemCanonicoRequest();
        item.setProductoCanonicoId(request.getProductoCanonicoId());
        item.setCantidad(request.getCantidad());
        item.setTieneSeguro(request.isSeguroTransporte());
        return item;
    }

    private PedidoItemRequest toPedidoItemRequest(PublicPedidoDirectoRequest request) {
        PedidoItemRequest item = new PedidoItemRequest();
        item.setProductoId(request.getProductoId());
        item.setCantidad(request.getCantidad());
        item.setTieneSeguro(request.isSeguroTransporte());
        return item;
    }

    private Revendedor resolverRevendedorShadowProveedor(java.util.UUID proveedorId) {
        RevendedorStorefront storefront = revendedorStorefrontRepository.findByProveedor_Id(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Storefront de proveedor no encontrado: " + proveedorId));
        if (storefront.getRevendedor() == null) {
            throw new ResourceNotFoundException(
                    "Storefront proveedor sin revendedor shadow configurado: " + proveedorId);
        }
        return storefront.getRevendedor();
    }

    private Usuario buscarOCrearCliente(PublicPedidoDirectoRequest request) {
        String emailNormalizado = request.getEmail().trim().toLowerCase();
        return this.usuarioRepository.findByEmailIgnoreCase(emailNormalizado).map(cliente -> cliente.getPasswordHash() == null || cliente.getPasswordHash().isBlank() ? this.actualizarDatosEntrega((Usuario)cliente, request) : cliente).orElseGet(() -> this.crearClienteNuevo(request, emailNormalizado));
    }

    private Usuario actualizarDatosEntrega(Usuario cliente, PublicPedidoDirectoRequest request) {
        cliente.setNombre(request.getNombreApellido().trim());
        cliente.setTelefonoWhatsapp(request.getTelefonoWhatsapp().trim());
        cliente.setProvincia(request.getProvincia().trim());
        cliente.setLocalidad(request.getLocalidad().trim());
        cliente.setCodigoPostal(request.getCodigoPostal().trim());
        cliente.setCalle(request.getCalle().trim());
        cliente.setNumero(request.getNumero().trim());
        cliente.setPisoDepto(normalizarOpcional(request.getPisoDepto()));
        cliente.setObservacionesEnvio(normalizarOpcional(request.getObservaciones()));
        return cliente;
    }

    private Usuario crearClienteNuevo(PublicPedidoDirectoRequest request, String emailNormalizado) {
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombreApellido().trim());
        usuario.setEmail(emailNormalizado);
        usuario.setTipoUsuario(TipoUsuario.CLIENTE);
        usuario.setActivo(true);
        actualizarDatosEntrega(usuario, request);
        return usuarioRepository.save(usuario);
    }

    private String normalizarOpcional(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private void asegurarClienteRevendedor(Usuario cliente, Revendedor revendedor) {
        ClienteRevendedor relacion = clienteRevendedorRepository
                .findByUsuario_IdAndRevendedor_Id(cliente.getId(), revendedor.getId())
                .orElseGet(() -> crearRelacion(cliente, revendedor));

        if (!relacion.isActivo()) {
            relacion.setActivo(true);
            clienteRevendedorRepository.save(relacion);
        }
    }

    private ClienteRevendedor crearRelacion(Usuario cliente, Revendedor revendedor) {
        ClienteRevendedor relacion = new ClienteRevendedor();
        relacion.setUsuario(cliente);
        relacion.setRevendedor(revendedor);
        relacion.setFechaAlta(LocalDateTime.now());
        relacion.setActivo(true);
        relacion.setOrigenAlta(ORIGEN_ALTA_COMPRA_PUBLICA);
        return clienteRevendedorRepository.save(relacion);
    }

    private void validarAsesorVenta(PublicPedidoDirectoRequest request) {
        if (request.getAsesorVentaId() == null) {
            return;
        }

        AsesorVenta asesorVenta = asesorVentaRepository.findById(request.getAsesorVentaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asesor de venta no disponible: " + request.getAsesorVentaId()));

        if (!asesorVenta.isActivo() || !asesorVenta.getRevendedor().getId().equals(request.getRevendedorId())) {
            throw new ResourceNotFoundException("Asesor de venta no disponible: " + request.getAsesorVentaId());
        }
    }

    private String formatearDireccion(PublicPedidoDirectoRequest request) {
        StringBuilder direccion = new StringBuilder();
        direccion.append(request.getCalle().trim())
                .append(" ")
                .append(request.getNumero().trim());

        if (request.getPisoDepto() != null && !request.getPisoDepto().isBlank()) {
            direccion.append(", ").append(request.getPisoDepto().trim());
        }

        direccion.append(", ")
                .append(request.getLocalidad().trim())
                .append(", ")
                .append(request.getProvincia().trim())
                .append(", CP ")
                .append(request.getCodigoPostal().trim());

        if (request.getObservaciones() != null && !request.getObservaciones().isBlank()) {
            direccion.append(". Obs: ").append(request.getObservaciones().trim());
        }

        return direccion.toString();
    }

    private PublicPedidoDirectoResponse finalizarPedido(PedidoResponse pedido, Integer demoraEstimadaDias, Revendedor revendedor, Usuario cliente, PublicPedidoDirectoRequest request) {
        String metodo = this.normalizarMetodoPago(request);
        String transferenciaToken = "TRANSFERENCIA".equals(metodo) ? this.transferenciaService.solicitar(pedido.getId()) : null;
        PublicPedidoDirectoResponse response = this.toResponse(pedido, demoraEstimadaDias, metodo, transferenciaToken);
        if ("TRANSFERENCIA".equals(metodo)) {
            this.publicarEventoPedidoCreado(pedido, revendedor, cliente);
        }
        return response;
    }

    private PublicPedidoDirectoResponse toResponse(PedidoResponse pedido, Integer demoraEstimadaDias, String metodoPago,
            String transferenciaToken) {
        BigDecimal totalEnvio = nvl(pedido.getTotalEnvio());
        BigDecimal totalSeguro = nvl(pedido.getTotalSeguro());
        BigDecimal totalDescuento = nvl(pedido.getTotalDescuento());
        BigDecimal totalFinal = nvl(pedido.getTotalFinal());
        BigDecimal totalProducto = totalFinal.subtract(totalEnvio).subtract(totalSeguro).add(totalDescuento);

        return PublicPedidoDirectoResponse.builder()
                .pedidoId(pedido.getId())
                .estadoPedido(pedido.getEstadoPedido())
                .estadoPago(pedido.getEstadoPago())
                .metodoPago(metodoPago)
                .totalProducto(totalProducto.max(BigDecimal.ZERO))
                .totalEnvio(totalEnvio)
                .totalSeguro(totalSeguro)
                .totalDescuento(totalDescuento)
                .totalFinal(totalFinal)
                .demoraEstimadaDias(demoraEstimadaDias != null ? demoraEstimadaDias : DEMORA_DEFAULT_DIAS)
                .trackingToken(pedido.getTrackingToken())
                .transferenciaToken(transferenciaToken)
                .build();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void publicarEventoPedidoCreado(PedidoResponse pedido, Revendedor revendedor, Usuario cliente) {
        applicationEventPublisher.publishEvent(new EventoPedidoCreado(
                pedido.getId(),
                revendedor.getId(),
                cliente.getEmail(),
                cliente.getNombre(),
                pedido.getTrackingToken(),
                pedido.getEstadoPedido(),
                pedido.getTotalFinal(),
                "ARS",
                pedido.getCreatedAt() != null ? pedido.getCreatedAt() : LocalDateTime.now()));
    }

    private final RevendedorMercadoPagoService revendedorMercadoPagoService;
    private final PspConfigService pspConfigService;

    private String normalizarMetodoPago(PublicPedidoDirectoRequest request) {
        return "TRANSFERENCIA".equalsIgnoreCase(request.getMetodoPago()) ? "TRANSFERENCIA" : "PSP";
    }

    private BigDecimal resolverRecargoMercadoPago(PublicPedidoDirectoRequest request) {
        if (!"PSP".equals(this.normalizarMetodoPago(request))) {
            return BigDecimal.ZERO;
        }
        if (!this.revendedorMercadoPagoService.resolveForRevendedor(request.getRevendedorId()).readyForCheckout()) {
            throw new OperacionEnConflictoException("La tienda no tiene una cuenta Mercado Pago lista para cobrar.");
        }
        return this.pspConfigService.getPspFeeFactor();
    }
}
