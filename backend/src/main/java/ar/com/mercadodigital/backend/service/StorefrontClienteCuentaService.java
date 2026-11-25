/*
 * Decompiled with CFR 0.152.
 */
package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.dto.request.StorefrontClientePerfilRequest;
import ar.com.mercadodigital.backend.dto.response.PublicPedidoTrackingResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontClientePedidoResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontClientePerfilResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.entity.ClienteRevendedor;
import ar.com.mercadodigital.backend.entity.OrigenPedido;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.ClienteRevendedorRepository;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import ar.com.mercadodigital.backend.service.PublicPedidoTrackingService;
import ar.com.mercadodigital.backend.service.StorefrontService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StorefrontClienteCuentaService {
    private final StorefrontService storefrontService;
    private final PublicPedidoTrackingService trackingService;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRevendedorRepository clienteRevendedorRepository;
    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly=true)
    public List<StorefrontClientePedidoResponse> listarPedidos(String storefrontHost, String emailAutenticado) {
        ClienteRevendedor relacion = this.resolverRelacionActiva(storefrontHost, emailAutenticado);
        return this.pedidoRepository.findTop100ByCliente_IdAndRevendedor_IdOrderByCreatedAtDesc(relacion.getUsuario().getId(), relacion.getRevendedor().getId()).stream().filter(pedido -> OrigenPedido.PUBLICO_DIRECTO.equals((Object)pedido.getOrigenPedido())).map(pedido -> this.toResponse((Pedido)pedido, storefrontHost)).toList();
    }

    @Transactional(readOnly=true)
    public StorefrontClientePerfilResponse obtenerPerfil(String storefrontHost, String emailAutenticado) {
        return this.toPerfilResponse(this.resolverRelacionActiva(storefrontHost, emailAutenticado).getUsuario());
    }

    @Transactional
    public StorefrontClientePerfilResponse actualizarPerfil(StorefrontClientePerfilRequest request, String emailAutenticado) {
        Usuario cliente = this.resolverRelacionActiva(request.getStorefrontHost(), emailAutenticado).getUsuario();
        cliente.setNombre(request.getNombre().trim());
        cliente.setTelefonoWhatsapp(request.getTelefonoWhatsapp().trim());
        cliente.setProvincia(request.getProvincia().trim());
        cliente.setLocalidad(request.getLocalidad().trim());
        cliente.setCodigoPostal(request.getCodigoPostal().trim());
        cliente.setCalle(request.getCalle().trim());
        cliente.setNumero(request.getNumero().trim());
        cliente.setPisoDepto(this.normalizarOpcional(request.getPisoDepto()));
        cliente.setObservacionesEnvio(this.normalizarOpcional(request.getObservacionesEnvio()));
        return this.toPerfilResponse(this.usuarioRepository.save(cliente));
    }

    private ClienteRevendedor resolverRelacionActiva(String storefrontHost, String emailAutenticado) {
        StorefrontResponse storefront = this.storefrontService.resolverPorHost(storefrontHost);
        if (storefront.getRevendedorId() == null) {
            throw new ResourceNotFoundException("Cuenta de cliente no disponible para esta tienda");
        }
        Usuario cliente = this.usuarioRepository.findByEmailIgnoreCase(emailAutenticado).orElseThrow(() -> new ResourceNotFoundException("Cuenta de cliente no encontrada"));
        return this.clienteRevendedorRepository.findByUsuario_IdAndRevendedor_Id(cliente.getId(), storefront.getRevendedorId()).filter(ClienteRevendedor::isActivo).orElseThrow(() -> new ResourceNotFoundException("Cuenta de cliente no disponible para esta tienda"));
    }

    private StorefrontClientePedidoResponse toResponse(Pedido pedido, String storefrontHost) {
        PublicPedidoTrackingResponse detalle = this.trackingService.getByTrackingToken(pedido.getTrackingToken(), storefrontHost);
        return StorefrontClientePedidoResponse.builder().pedidoId(pedido.getId()).trackingToken(pedido.getTrackingToken()).fechaCreacion(pedido.getCreatedAt()).estadoPedido(detalle.getEstadoPedido()).estadoPago(detalle.getEstadoPago()).metodoPago(detalle.getMetodoPago()).totalFinal(detalle.getTotalFinal()).items(detalle.getItems()).envio(detalle.getEnvio()).transferenciaToken(pedido.getPago() != null ? pedido.getPago().getTransferenciaToken() : null).comprobanteCargado(pedido.getPago() != null && pedido.getPago().getComprobanteCargadoAt() != null).build();
    }

    private StorefrontClientePerfilResponse toPerfilResponse(Usuario cliente) {
        return StorefrontClientePerfilResponse.builder().nombre(cliente.getNombre()).email(cliente.getEmail()).telefonoWhatsapp(cliente.getTelefonoWhatsapp()).provincia(cliente.getProvincia()).localidad(cliente.getLocalidad()).codigoPostal(cliente.getCodigoPostal()).calle(cliente.getCalle()).numero(cliente.getNumero()).pisoDepto(cliente.getPisoDepto()).observacionesEnvio(cliente.getObservacionesEnvio()).build();
    }

    private String normalizarOpcional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public StorefrontClienteCuentaService(StorefrontService storefrontService, PublicPedidoTrackingService trackingService, UsuarioRepository usuarioRepository, ClienteRevendedorRepository clienteRevendedorRepository, PedidoRepository pedidoRepository) {
        this.storefrontService = storefrontService;
        this.trackingService = trackingService;
        this.usuarioRepository = usuarioRepository;
        this.clienteRevendedorRepository = clienteRevendedorRepository;
        this.pedidoRepository = pedidoRepository;
    }
}

