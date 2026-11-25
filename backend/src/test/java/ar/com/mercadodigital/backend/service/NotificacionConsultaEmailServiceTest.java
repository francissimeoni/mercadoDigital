package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.com.mercadodigital.backend.entity.ProductoPregunta;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorProducto;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;
import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompra;
import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompraResolver;
import ar.com.mercadodigital.backend.service.postcompra.EmailSender;
import ar.com.mercadodigital.backend.service.postcompra.MensajeEmail;

@ExtendWith(MockitoExtension.class)
class NotificacionConsultaEmailServiceTest {

    @Mock EmailSender emailSender;
    @Mock BrandingPostCompraResolver brandingResolver;
    @Mock RevendedorStorefrontRepository storefrontRepository;
    @InjectMocks NotificacionConsultaEmailService service;

    @Test
    void enviaNuevaPreguntaAlEmailComercialConSmtpDeLaTienda() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);

        RevendedorStorefront storefront = new RevendedorStorefront();
        storefront.setRevendedor(revendedor);
        storefront.setEmailComercial("ventas@infonel.com.ar");

        RevendedorProducto producto = new RevendedorProducto();
        producto.setNombre("Telefono de prueba");

        ProductoPregunta pregunta = new ProductoPregunta();
        pregunta.setId(UUID.randomUUID());
        pregunta.setRevendedor(revendedor);
        pregunta.setRevendedorProducto(producto);
        pregunta.setNombreContacto("Cliente");
        pregunta.setEmailContacto("cliente@example.com");
        pregunta.setPregunta("¿Tiene garantia?");

        BrandingPostCompra branding = new BrandingPostCompra(
                "INFONEL", "#0f172a", null, "https://infonel.com.ar",
                "ventas@infonel.com.ar", "ventas@infonel.com.ar", "INFONEL",
                "smtp.example.com", 587, "ventas@infonel.com.ar", "secret", true, true);
        when(storefrontRepository.findByRevendedor_Id(revendedorId)).thenReturn(Optional.of(storefront));
        when(brandingResolver.resolver(revendedorId)).thenReturn(branding);

        service.notificarNuevaPregunta(pregunta);

        ArgumentCaptor<MensajeEmail> captor = ArgumentCaptor.forClass(MensajeEmail.class);
        verify(emailSender).send(captor.capture());
        MensajeEmail mensaje = captor.getValue();
        assertEquals("ventas@infonel.com.ar", mensaje.getTo());
        assertEquals("cliente@example.com", mensaje.getReplyTo());
        assertEquals("smtp.example.com", mensaje.getSmtpHost());
        assertTrue(mensaje.getSubject().contains("Telefono de prueba"));
    }

    @Test
    void unaFallaDeCorreoNoImpideRegistrarLaConsulta() {
        ProductoPregunta pregunta = new ProductoPregunta();
        pregunta.setId(UUID.randomUUID());
        Revendedor revendedor = new Revendedor();
        revendedor.setId(UUID.randomUUID());
        pregunta.setRevendedor(revendedor);
        RevendedorProducto producto = new RevendedorProducto();
        producto.setNombre("Producto");
        pregunta.setRevendedorProducto(producto);
        pregunta.setPregunta("Consulta");
        when(brandingResolver.resolver(any())).thenThrow(new IllegalStateException("SMTP caido"));

        service.notificarNuevaPregunta(pregunta);
    }
}
