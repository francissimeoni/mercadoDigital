package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ar.com.mercadodigital.backend.entity.*;
import ar.com.mercadodigital.backend.repository.*;
import ar.com.mercadodigital.backend.dto.request.RevendedorAsesorRequest;

@ExtendWith(MockitoExtension.class)
class RevendedorAsesorCatalogoTest {
    @Test void edicionParcialConservaZonaYPermiteQuitarFotoExpresamente() {
        UUID tienda = UUID.randomUUID(), id = UUID.randomUUID(); var store = new Revendedor(); store.setId(tienda);
        var advisor = new AsesorVenta(); advisor.setId(id); advisor.setRevendedor(store); advisor.setNombre("Ana");
        advisor.setProvincia("Córdoba"); advisor.setLocalidad("Córdoba capital"); advisor.setZonaAtencion("Zona norte");
        advisor.setFotoUrl("https://example.invalid/foto-anterior.jpg"); advisor.setWhatsappComercial("+5493482670015");
        when(revendedorRepository.findById(tienda)).thenReturn(Optional.of(store));
        when(asesorVentaRepository.findByIdAndRevendedor_Id(id, tienda)).thenReturn(Optional.of(advisor));
        when(asesorVentaRepository.save(advisor)).thenReturn(advisor);
        var input = new RevendedorAsesorRequest(); input.setNombre("Ana"); input.setActivo(false);
        service.actualizar(tienda,id,input);
        assertEquals("Córdoba",advisor.getProvincia()); assertEquals("Córdoba capital",advisor.getLocalidad());
        assertEquals("Zona norte",advisor.getZonaAtencion()); assertNotNull(advisor.getFotoUrl());
        input.setQuitarFoto(true); input.setZonaAtencion("");
        var result=service.actualizar(tienda,id,input); assertNull(result.getFotoUrl()); assertNull(result.getZonaAtencion());
        assertEquals("+5493482670015",result.getWhatsappComercial());
    }
    @Test void noPermiteUsarFotoDeOtraTienda() {
        UUID tienda=UUID.randomUUID(); var store=new Revendedor();store.setId(tienda);
        when(revendedorRepository.findById(tienda)).thenReturn(Optional.of(store));
        var input=new RevendedorAsesorRequest();input.setNombre("Asesor");
        input.setFotoUrl("/public/assets/storefront/asesor-"+UUID.randomUUID()+"-"+UUID.randomUUID()+".jpg");
        assertThrows(ar.com.mercadodigital.backend.exception.BadRequestException.class,()->service.crear(tienda,input));
        verifyNoInteractions(asesorVentaRepository);
    }
    @Mock RevendedorOwnershipService ownershipService;
    @Mock RevendedorRepository revendedorRepository;
    @Mock AsesorVentaRepository asesorVentaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock RolRepository rolRepository;
    @Mock StorefrontCredencialRepository credencialRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks RevendedorAsesorService service;

    @Test void creaContactoSinUsuarioNiPassword() {
        UUID tienda = UUID.randomUUID(); var store = new Revendedor(); store.setId(tienda);
        when(revendedorRepository.findById(tienda)).thenReturn(Optional.of(store));
        when(asesorVentaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var input = new RevendedorAsesorRequest(); input.setNombre("Ventas"); input.setWhatsappComercial("+54 9 11 1234 5678");
        var result = service.crear(tienda, input);
        assertEquals("+5491112345678", result.getWhatsappComercial()); assertNull(result.getEmail());
        verifyNoInteractions(usuarioRepository, rolRepository, credencialRepository, passwordEncoder);
    }
    @Test void contactoNoCreaAccesoYCampoOmitidoConservaNumeroYFoto() {
        UUID tienda = UUID.randomUUID(), id = UUID.randomUUID(); var store = new Revendedor(); store.setId(tienda);
        var advisor = new AsesorVenta(); advisor.setId(id); advisor.setRevendedor(store); advisor.setNombre("Ana");
        advisor.setWhatsappComercial("5491112345678"); advisor.setFotoUrl("https://example.invalid/ana.jpg");
        when(revendedorRepository.findById(tienda)).thenReturn(Optional.of(store));
        when(asesorVentaRepository.findByIdAndRevendedor_Id(id, tienda)).thenReturn(Optional.of(advisor));
        when(asesorVentaRepository.save(advisor)).thenReturn(advisor);
        var input = new RevendedorAsesorRequest(); input.setNombre("Ana"); input.setActivo(false);
        service.actualizar(tienda, id, input);
        assertEquals("5491112345678", advisor.getWhatsappComercial()); assertEquals("https://example.invalid/ana.jpg", advisor.getFotoUrl());
        input.setWhatsappComercial(""); service.actualizar(tienda, id, input); assertNull(advisor.getWhatsappComercial());
        verifyNoInteractions(usuarioRepository, rolRepository, credencialRepository, passwordEncoder);
    }
    @Test void actualizaWhatsappConservandoUsuarioYCredencialExistentes() {
        UUID tienda = UUID.randomUUID(), id = UUID.randomUUID(); var store = new Revendedor(); store.setId(tienda);
        var user = new Usuario(); user.setId(UUID.randomUUID()); user.setEmail("ana@test.invalid"); user.setPasswordHash("hash-actual");
        var advisor = new AsesorVenta(); advisor.setId(id); advisor.setRevendedor(store); advisor.setUsuario(user);
        when(revendedorRepository.findById(tienda)).thenReturn(Optional.of(store));
        when(asesorVentaRepository.findByIdAndRevendedor_Id(id, tienda)).thenReturn(Optional.of(advisor));
        when(asesorVentaRepository.save(advisor)).thenReturn(advisor);
        var input = new RevendedorAsesorRequest(); input.setNombre("Ana"); input.setWhatsappComercial("+54 9 11 1234 5678");
        service.actualizar(tienda, id, input);
        assertSame(user, advisor.getUsuario()); assertEquals("hash-actual", user.getPasswordHash());
        assertEquals("ana@test.invalid", user.getEmail()); verify(credencialRepository).save(any());
        verifyNoInteractions(rolRepository, passwordEncoder);
    }
}
