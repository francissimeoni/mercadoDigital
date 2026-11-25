package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.dto.response.LiquidacionProveedorResponse;
import ar.com.mercadodigital.backend.entity.LiquidacionProveedorEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.Proveedor;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.SplitPedidoItem;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.SplitPayoutEjecucionRepository;
import ar.com.mercadodigital.backend.repository.SplitPedidoItemRepository;
import ar.com.mercadodigital.backend.service.split.SplitPayoutGateway;

@ExtendWith(MockitoExtension.class)
class LiquidacionProveedorServiceTest {
    @org.mockito.Mock private MercadoPagoClient mercadoPagoClient;

    @Mock
    private SplitPedidoItemRepository splitPedidoItemRepository;
    @Mock
    private AuditoriaEventoRepository auditoriaEventoRepository;
    @Mock
    private SplitPayoutGateway splitPayoutGateway;
    @Mock
    private SplitPayoutEjecucionRepository splitPayoutEjecucionRepository;
    @Mock
    private RevendedorMercadoPagoService revendedorMercadoPagoService;
    @Mock
    private MercadoPagoMdConfigService mercadoPagoMdConfigService;

    @InjectMocks
    private LiquidacionProveedorService liquidacionProveedorService;

    @Test
    void liberarAutomaticamenteMarcaEstadoYAudita() {
        when(mercadoPagoClient.isEnabled()).thenReturn(true);
        UUID pedidoId = UUID.randomUUID();
        SplitPedidoItem split = split(pedidoId, new BigDecimal("1000"), LiquidacionProveedorEstado.PENDIENTE, PedidoEstado.PAGADO);

        when(splitPedidoItemRepository.findByPedidoItem_Pedido_Id(pedidoId)).thenReturn(List.of(split));
        when(splitPedidoItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriaEventoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(splitPayoutEjecucionRepository.findByPedidoId(pedidoId)).thenReturn(java.util.Optional.empty());
        when(splitPayoutEjecucionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(revendedorMercadoPagoService.resolveForRevendedor(any())).thenReturn(
                new RevendedorMercadoPagoService.RevendedorMercadoPagoResolution("token", "collector-1", false, true));
        when(mercadoPagoMdConfigService.resolveForPayout()).thenReturn(
                new MercadoPagoMdConfigService.MercadoPagoMdPayoutConfig("token-md", "md@test.com", true));
        when(splitPayoutGateway.intentarPayoutRetirado(any())).thenReturn(
                new SplitPayoutGateway.SplitPayoutResult("payout-1", "idem-1", "created", "{}"));

        LiquidacionProveedorResponse response = liquidacionProveedorService.liberarAutomaticamentePorRetiro(pedidoId);

        assertEquals("LIBERADO_AUTOMATICO", response.getEstadoLiquidacionProveedor());
        assertEquals(0, new BigDecimal("1000").compareTo(response.getTotalProveedor()));
        assertNotNull(response.getLiberadoProveedorAt());
        verify(splitPedidoItemRepository).saveAll(any());
        verify(auditoriaEventoRepository, org.mockito.Mockito.atLeastOnce()).save(any());
        verify(splitPayoutGateway).intentarPayoutRetirado(any());
    }

    @Test
    void liberarManualExigePedidoPagado() {
        UUID pedidoId = UUID.randomUUID();
        SplitPedidoItem split = split(pedidoId, new BigDecimal("1000"), LiquidacionProveedorEstado.PENDIENTE, PedidoEstado.CREADO);

        when(splitPedidoItemRepository.findByPedidoItem_Pedido_Id(pedidoId)).thenReturn(List.of(split));

        assertThrows(OperacionEnConflictoException.class,
                () -> liquidacionProveedorService.liberarManual(pedidoId, "urgencia logistica"));
    }

    @Test
    void retiroDeMercadoPagoNoEnviaFondosNiRegistraPayoutExitoso() {
        UUID pedidoId = UUID.randomUUID();
        SplitPedidoItem split = split(pedidoId, new BigDecimal("1000"), LiquidacionProveedorEstado.PENDIENTE, PedidoEstado.PAGADO);
        when(splitPedidoItemRepository.findByPedidoItem_Pedido_Id(pedidoId)).thenReturn(List.of(split));
        // El cliente deshabilitado devuelve false por defecto.
        liquidacionProveedorService.liberarAutomaticamentePorRetiro(pedidoId);
        org.mockito.Mockito.verifyNoInteractions(splitPayoutGateway, splitPayoutEjecucionRepository,
                revendedorMercadoPagoService, mercadoPagoMdConfigService);
        verify(auditoriaEventoRepository).save(org.mockito.ArgumentMatchers.argThat(
                evento -> "SPLIT_PAYOUT_RETIRADO".equals(evento.getTipoEvento())));
    }

    @Test
    void liberarManualMarcaMotivoYTotalProveedor() {
        UUID pedidoId = UUID.randomUUID();
        SplitPedidoItem splitA = split(pedidoId, new BigDecimal("1000"), LiquidacionProveedorEstado.PENDIENTE, PedidoEstado.PAGADO);
        SplitPedidoItem splitB = split(pedidoId, new BigDecimal("500"), LiquidacionProveedorEstado.PENDIENTE, PedidoEstado.PAGADO);

        when(splitPedidoItemRepository.findByPedidoItem_Pedido_Id(pedidoId)).thenReturn(List.of(splitA, splitB));
        when(splitPedidoItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(auditoriaEventoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LiquidacionProveedorResponse response = liquidacionProveedorService.liberarManual(pedidoId, "despacho directo del proveedor");

        assertEquals("LIBERADO_MANUAL", response.getEstadoLiquidacionProveedor());
        assertEquals("despacho directo del proveedor", response.getMotivoLiberacionManual());
        assertEquals(0, new BigDecimal("1500").compareTo(response.getTotalProveedor()));
        verify(splitPedidoItemRepository).saveAll(any());
        verify(auditoriaEventoRepository).save(any());
    }

    private SplitPedidoItem split(UUID pedidoId, BigDecimal montoProveedor, LiquidacionProveedorEstado estado, PedidoEstado pedidoEstado) {
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setEstadoPedido(pedidoEstado);
        Revendedor revendedor = new Revendedor();
        revendedor.setId(UUID.randomUUID());
        Usuario usuarioRevendedor = new Usuario();
        usuarioRevendedor.setEmail("revendedor@test.com");
        revendedor.setUsuario(usuarioRevendedor);
        pedido.setRevendedor(revendedor);

        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        Proveedor proveedor = new Proveedor();
        Usuario proveedorUsuario = new Usuario();
        proveedorUsuario.setEmail("proveedor@test.com");
        proveedor.setUsuario(proveedorUsuario);
        proveedor.setDespachaMercaderia(true);
        item.setProveedor(proveedor);

        SplitPedidoItem split = new SplitPedidoItem();
        split.setPedidoItem(item);
        split.setMontoProveedor(montoProveedor);
        split.setMontoRevendedor(BigDecimal.ZERO);
        split.setMontoMd(BigDecimal.ZERO);
        split.setMontoEnvio(BigDecimal.ZERO);
        split.setMontoSeguro(BigDecimal.ZERO);
        split.setMontoPsp(BigDecimal.ZERO);
        split.setEstadoLiquidacionProveedor(estado);
        return split;
    }
}
