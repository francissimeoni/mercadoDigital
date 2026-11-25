package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class StorefrontClienteCuentaRecoveryTest {
    private final StorefrontService storefront = mock(StorefrontService.class);
    private final PublicPedidoTrackingService tracking = mock(PublicPedidoTrackingService.class);
    private final UsuarioRepository users = mock(UsuarioRepository.class);
    private final ClienteRevendedorRepository relations = mock(ClienteRevendedorRepository.class);
    private final PedidoRepository orders = mock(PedidoRepository.class);
    private final StorefrontClienteCuentaService service = new StorefrontClienteCuentaService(storefront, tracking, users, relations, orders);

    @Test void tiendaSinRelacionActivaNoExponePerfilNiPedidos() {
        UUID storeId = UUID.randomUUID(); var user = new Usuario(); user.setId(UUID.randomUUID());
        when(storefront.resolverPorHost("other.test")).thenReturn(StorefrontResponse.builder().revendedorId(storeId).build());
        when(users.findByEmailIgnoreCase("customer@test.com")).thenReturn(Optional.of(user));
        var relation = new ClienteRevendedor(); relation.setActivo(false);
        when(relations.findByUsuario_IdAndRevendedor_Id(user.getId(), storeId)).thenReturn(Optional.of(relation));
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPerfil("other.test", "customer@test.com"));
        assertThrows(ResourceNotFoundException.class, () -> service.listarPedidos("other.test", "customer@test.com"));
        verifyNoInteractions(orders, tracking);
    }

    @Test void pedidosConsultaUsuarioYTiendaYFiltraOrigenNoPublico() {
        UUID storeId = UUID.randomUUID(); var user = new Usuario(); user.setId(UUID.randomUUID());
        var store = new Revendedor(); store.setId(storeId);
        var relation = new ClienteRevendedor(); relation.setActivo(true); relation.setUsuario(user); relation.setRevendedor(store);
        when(storefront.resolverPorHost("shop.test")).thenReturn(StorefrontResponse.builder().revendedorId(storeId).build());
        when(users.findByEmailIgnoreCase("customer@test.com")).thenReturn(Optional.of(user));
        when(relations.findByUsuario_IdAndRevendedor_Id(user.getId(), storeId)).thenReturn(Optional.of(relation));
        var nonPublic = new Pedido(); nonPublic.setOrigenPedido(null);
        when(orders.findTop100ByCliente_IdAndRevendedor_IdOrderByCreatedAtDesc(user.getId(), storeId)).thenReturn(List.of(nonPublic));
        assertTrue(service.listarPedidos("shop.test", "customer@test.com").isEmpty());
        verify(orders).findTop100ByCliente_IdAndRevendedor_IdOrderByCreatedAtDesc(user.getId(), storeId);
        verifyNoInteractions(tracking);
    }
}
