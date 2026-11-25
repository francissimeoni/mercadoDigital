package ar.com.mercadodigital.backend.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ar.com.mercadodigital.backend.config.SecurityConfig;
import ar.com.mercadodigital.backend.security.*;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.repository.*;
import ar.com.mercadodigital.backend.service.*;

/** HTTP, seguridad y servicios de tracking/PSP reales; repositorios y proveedor externos simulados. */
@WebMvcTest(controllers = {PublicPedidoPagoPspController.class, PublicPedidoTrackingController.class}, properties = {
    "mercadopago.enabled=true", // Solo regresion del contrato historico; retirado en produccion.
    "app.security.enabled=true", "app.security.jwt-secret=catalogo-paso3-local-test",
    "catalogo.publico.revendedores=00000000-0000-0000-0000-000000000002"})
@Import({SecurityConfig.class, JwtFilter.class, JwtService.class, PublicPedidoTrackingService.class,
    PublicPedidoPagoPspService.class, CatalogoModoService.class})
class CatalogoPedidoHistoricoControllerTest {
    @Autowired MockMvc mvc;
    @Autowired CatalogoModoService modo;
    @MockBean PedidoRepository pedidos;
    @MockBean ProductoMediaRepository media;
    @MockBean MercadoPagoClient psp;
    @MockBean RevendedorMercadoPagoService cuentas;
    @MockBean PedidoService creacion;
    @MockBean EnvioService envios;
    final UUID tienda = UUID.fromString("00000000-0000-0000-0000-000000000002");
    final String host = "tienda.test.invalid", token = "seguimiento-anterior";

    Pedido pedido() {
        var rev = new Revendedor(); rev.setId(tienda); rev.setDominio(host);
        var pedido = new Pedido(); pedido.setId(UUID.randomUUID()); pedido.setRevendedor(rev);
        pedido.setEstadoPedido(PedidoEstado.CREADO); pedido.setOrigenPedido(OrigenPedido.PUBLICO_DIRECTO);
        pedido.setItems(List.of()); pedido.setTotalFinal(new BigDecimal("123.45"));
        var pago = new Pago(); pago.setId(UUID.randomUUID()); pago.setEstadoPago(PagoEstado.PENDIENTE);
        pago.setMetodo("PSP"); pago.setMonto(new BigDecimal("123.45"));
        pago.setPspReferencia("preferencia-existente"); pago.setPspCheckoutUrl("https://www.mercadopago.com/checkout/v1/redirect?pref=anterior");
        pedido.setPago(pago); pago.setPedido(pedido);
        when(pedidos.findByTrackingToken(token)).thenReturn(Optional.of(pedido));
        return pedido;
    }
    @Test void catalogoPermiteRetomarPedidoExistenteSinCrearloNiRecrearPreferencia() throws Exception {
        assertTrue(modo.activo(tienda));
        var pedido = pedido(); when(pedidos.findByIdForUpdate(pedido.getId())).thenReturn(Optional.of(pedido));
        mvc.perform(post("/public/pedido/{token}/reanudar-pago", token).param("storefrontHost", host))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.pedidoId").value(pedido.getId().toString()))
            .andExpect(jsonPath("$.checkoutUrl").value(pedido.getPago().getPspCheckoutUrl()));
        verify(pedidos, never()).save(any()); verify(psp).exigirHabilitado(); verifyNoMoreInteractions(psp); verifyNoInteractions( cuentas, creacion, envios);
    }
    @Test void tokenDeOtraTiendaNoPreparaPago() throws Exception {
        pedido();
        mvc.perform(post("/public/pedido/{token}/reanudar-pago", token).param("storefrontHost", "otra.test.invalid"))
            .andExpect(status().isNotFound());
        verify(pedidos, never()).findById(any()); verifyNoInteractions(psp, cuentas, creacion);
    }
    @Test void tokenInexistenteNoPreparaPago() throws Exception {
        mvc.perform(post("/public/pedido/{token}/reanudar-pago", token).param("storefrontHost", host))
            .andExpect(status().isNotFound());
        verify(pedidos, never()).findById(any()); verifyNoInteractions(psp, cuentas, creacion);
    }
    @Test void hostObligatorioAntesDeConsultarReferencias() throws Exception {
        mvc.perform(post("/public/pedido/{token}/reanudar-pago", token)).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidos, psp, cuentas, creacion);
    }
    @Test void pedidoOperativoNoSeExponePorTokenPublico() throws Exception {
        pedido().setOrigenPedido(OrigenPedido.OPERATIVO);
        mvc.perform(post("/public/pedido/{token}/reanudar-pago", token).param("storefrontHost", host))
            .andExpect(status().isNotFound());
        verify(pedidos, never()).findById(any()); verifyNoInteractions(psp, cuentas, creacion);
    }
    @Test void pedidoCanceladoNoReanudaPago() throws Exception {
        var pedido = pedido(); pedido.setEstadoPedido(PedidoEstado.CANCELADO);
        when(pedidos.findByIdForUpdate(pedido.getId())).thenReturn(Optional.of(pedido));
        mvc.perform(post("/public/pedido/{token}/reanudar-pago", token).param("storefrontHost", host))
            .andExpect(status().isConflict());
        verify(psp).exigirHabilitado(); verifyNoMoreInteractions(psp); verifyNoInteractions( cuentas, creacion);
    }
    @Test void pagoConfirmadoNoVuelveACobrarse() throws Exception {
        var pedido = pedido(); pedido.getPago().setEstadoPago(PagoEstado.CONFIRMADO);
        when(pedidos.findByIdForUpdate(pedido.getId())).thenReturn(Optional.of(pedido));
        mvc.perform(post("/public/pedido/{token}/reanudar-pago", token).param("storefrontHost", host))
            .andExpect(status().isConflict());
        verify(psp).exigirHabilitado(); verifyNoMoreInteractions(psp); verifyNoInteractions( cuentas, creacion);
    }
    @Test void comprobantePendienteSoloSeOfreceEnPedidoVigenteValidado() throws Exception {
        var pedido = pedido(); pedido.getPago().setMetodo("TRANSFERENCIA");
        pedido.getPago().setTransferenciaToken("comprobante-anterior");
        mvc.perform(get("/public/pedido/{token}", token).param("storefrontHost", host))
            .andExpect(status().isOk()).andExpect(jsonPath("$.transferenciaToken").value("comprobante-anterior"));
        pedido.setEstadoPedido(PedidoEstado.CANCELADO);
        mvc.perform(get("/public/pedido/{token}", token).param("storefrontHost", host))
            .andExpect(status().isOk()).andExpect(jsonPath("$.transferenciaToken").isEmpty());
        verifyNoInteractions(psp, cuentas, creacion);
    }
    @Test void comprobanteCargadoNoSeSolicitaNuevamente() throws Exception {
        var pedido = pedido(); pedido.getPago().setMetodo("TRANSFERENCIA");
        pedido.getPago().setTransferenciaToken("comprobante-anterior"); pedido.getPago().setComprobanteArchivo("recibo.pdf");
        mvc.perform(get("/public/pedido/{token}", token).param("storefrontHost", host))
            .andExpect(status().isOk()).andExpect(jsonPath("$.comprobanteCargado").value(true))
            .andExpect(jsonPath("$.transferenciaToken").isEmpty());
    }
}
