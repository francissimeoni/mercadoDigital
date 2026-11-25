package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ar.com.mercadodigital.backend.dto.request.*;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.repository.*;

@ExtendWith(MockitoExtension.class)
class CatalogoCompraBloqueoTest {
    @org.mockito.Mock private MercadoPagoClient mercadoPagoClient;
    static final UUID TIENDA = UUID.fromString("00000000-0000-0000-0000-000000000002");
    @Spy CatalogoModoService modoCatalogo = new CatalogoModoService(TIENDA.toString());
    @Mock PedidoRepository pedidos;
    @Mock UsuarioRepository usuarios;
    @Mock RevendedorProductoRepository productos;
    @Mock UsuarioService usuarioService;
    @Mock RevendedorOwnershipService ownershipService;
    @InjectMocks PedidoService service;
    @InjectMocks PublicPedidoDirectoService publico;

    @Test void predeterminadoDesactivadoYListaPorTienda() {
        assertFalse(new CatalogoModoService("").activo(TIENDA));
        assertTrue(modoCatalogo.activo(TIENDA)); assertFalse(modoCatalogo.activo(UUID.randomUUID())); assertFalse(modoCatalogo.activo(null));
        assertThrows(IllegalArgumentException.class, () -> new CatalogoModoService("incorrecto"));
    }
    @Test void compraPublicaSeRechazaAntesDeCrearCliente() {
        var request = new PublicPedidoDirectoRequest(); request.setRevendedorId(TIENDA);
        assertThrows(OperacionEnConflictoException.class, () -> publico.crearPedidoDirecto(request));
        verifyNoInteractions(usuarios, pedidos, productos, usuarioService);
    }
    @Test void todasLasEntradasDeCreacionDePedidoTienenBloqueo() {
        var normal = new CrearPedidoRequest(); normal.setRevendedorId(TIENDA);
        var canonico = new CrearPedidoCanonicoRequest(); canonico.setRevendedorId(TIENDA);
        assertThrows(OperacionEnConflictoException.class, () -> service.crearPedido(normal));
        assertThrows(OperacionEnConflictoException.class, () -> service.crearPedidoPublicoPorProducto(normal));
        assertThrows(OperacionEnConflictoException.class, () -> service.crearPedidoCanonico(canonico));
        assertThrows(OperacionEnConflictoException.class, () -> service.crearPedidoCanonicoPublico(canonico, "local"));
        assertThrows(OperacionEnConflictoException.class, () -> service.crearPedidoProductoPropio(UUID.randomUUID(), TIENDA, UUID.randomUUID(), 1, false, "local", "TRANSFERENCIA", null));
        verifyNoInteractions(usuarios, pedidos, productos, usuarioService);
    }
}
