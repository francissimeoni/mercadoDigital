package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.com.mercadodigital.backend.dto.request.AdminRevendedorRequest;
import ar.com.mercadodigital.backend.dto.request.AdminStorefrontRequest;
import ar.com.mercadodigital.backend.dto.response.AdminRevendedorResponse;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.entity.Rol;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;
import ar.com.mercadodigital.backend.repository.RolRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AdminRevendedorServiceTest {

    @Mock
    private RevendedorRepository revendedorRepository;

    @Mock
    private RevendedorStorefrontRepository revendedorStorefrontRepository;
    @Mock
    private RevendedorMercadoPagoService revendedorMercadoPagoService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminRevendedorService adminRevendedorService;

    @Test
    void crearNormalizaDominioYDejaStorefrontAusente() {
        AdminRevendedorRequest request = new AdminRevendedorRequest();
        request.setNombre("  Tienda Demo  ");
        request.setDominio("HTTPS://MiTienda.com:443/catalogo");
        request.setUsuarioEmail("revendedor@mitienda.com");
        request.setUsuarioPassword("Secreta123!");
        request.setActivo(true);

        when(revendedorRepository.findByDominioIgnoreCase("mitienda.com")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("revendedor@mitienda.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secreta123!")).thenReturn("hash-demo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            if (usuario.getId() == null) {
                usuario.setId(UUID.randomUUID());
            }
            return usuario;
        });
        when(rolRepository.findByNombre("REVENDEDOR")).thenReturn(Optional.of(rolRevendedor()));
        when(revendedorRepository.save(any(Revendedor.class))).thenAnswer(invocation -> {
            Revendedor revendedor = invocation.getArgument(0);
            revendedor.setId(UUID.randomUUID());
            return revendedor;
        });

        AdminRevendedorResponse response = adminRevendedorService.crear(request);

        assertEquals("Tienda Demo", response.getNombre());
        assertEquals("mitienda.com", response.getDominio());
        assertEquals(true, response.isActivo());
        assertEquals("revendedor@mitienda.com", response.getUsuarioEmail());
        assertEquals(true, response.isUsuarioConfigurado());
        assertNull(response.getStorefront());
    }

    @Test
    void crearConDominioDuplicadoDevuelveConflicto() {
        AdminRevendedorRequest request = new AdminRevendedorRequest();
        request.setNombre("Tienda Demo");
        request.setDominio("mitienda.com");
        request.setActivo(true);

        Revendedor existente = new Revendedor();
        existente.setId(UUID.randomUUID());
        existente.setDominio("mitienda.com");

        when(revendedorRepository.findByDominioIgnoreCase("mitienda.com")).thenReturn(Optional.of(existente));

        OperacionEnConflictoException ex = assertThrows(OperacionEnConflictoException.class,
                () -> adminRevendedorService.crear(request));

        assertEquals("Ya existe un revendedor con dominio: mitienda.com", ex.getMessage());
    }

    @Test
    void upsertStorefrontCreaCuandoNoExiste() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        revendedor.setNombre("Tienda Demo");
        revendedor.setDominio("mitienda.com");
        revendedor.setActivo(true);

        AdminStorefrontRequest request = new AdminStorefrontRequest();
        request.setNombreComercial("Mi Tienda");
        request.setLogoUrl(" https://cdn/logo.png ");
        request.setSmtpHost(" smtp.mitienda.com ");
        request.setSmtpPort(587);
        request.setSmtpUsername("mailer@mitienda.com ");
        request.setSmtpPassword(" smtp-secret ");
        request.setPublicada(true);

        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(revendedorStorefrontRepository.findByRevendedor_Id(revendedorId)).thenReturn(Optional.empty());
        when(revendedorStorefrontRepository.save(any(RevendedorStorefront.class))).thenAnswer(invocation -> {
            RevendedorStorefront storefront = invocation.getArgument(0);
            storefront.setId(UUID.randomUUID());
            return storefront;
        });

        AdminRevendedorResponse response = adminRevendedorService.upsertStorefront(revendedorId, request);

        assertNotNull(response.getStorefront());
        assertEquals("Mi Tienda", response.getStorefront().getNombreComercial());
        assertEquals("https://cdn/logo.png", response.getStorefront().getLogoUrl());
        assertEquals("smtp.mitienda.com", response.getStorefront().getSmtpHost());
        assertEquals(587, response.getStorefront().getSmtpPort());
        assertEquals("mailer@mitienda.com", response.getStorefront().getSmtpUsername());
        assertEquals(true, response.getStorefront().isSmtpConfigured());
        assertEquals(true, response.getStorefront().isPublicada());
    }

    @Test
    void upsertStorefrontPublicadoSobreRevendedorInactivoDevuelveBadRequest() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        revendedor.setDominio("mitienda.com");
        revendedor.setActivo(false);

        AdminStorefrontRequest request = new AdminStorefrontRequest();
        request.setNombreComercial("Mi Tienda");
        request.setPublicada(true);

        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> adminRevendedorService.upsertStorefront(revendedorId, request));

        assertEquals("No se puede publicar storefront para un revendedor inactivo", ex.getMessage());
    }

    @Test
    void crearConDominioInvalidoDevuelveBadRequest() {
        AdminRevendedorRequest request = new AdminRevendedorRequest();
        request.setNombre("Tienda Demo");
        request.setDominio("invalido");
        request.setActivo(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> adminRevendedorService.crear(request));

        assertEquals("dominio invalido: invalido", ex.getMessage());
    }

    @Test
    void actualizarRevendedorPermiteConfigurarCredencialesSinCambiarPasswordSiQuedaVacia() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        revendedor.setNombre("Tienda Demo");
        revendedor.setDominio("mitienda.com");
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("actual@mitienda.com");
        usuario.setPasswordHash("hash-existente");
        revendedor.setUsuario(usuario);

        AdminRevendedorRequest request = new AdminRevendedorRequest();
        request.setNombre("Tienda Demo");
        request.setDominio("mitienda.com");
        request.setUsuarioEmail("nuevo@mitienda.com");
        request.setUsuarioPassword("");
        request.setActivo(true);

        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(revendedorRepository.findByDominioIgnoreCase("mitienda.com")).thenReturn(Optional.of(revendedor));
        when(usuarioRepository.findByEmailIgnoreCase("nuevo@mitienda.com")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre("REVENDEDOR")).thenReturn(Optional.of(rolRevendedor()));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(revendedorRepository.save(any(Revendedor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminRevendedorResponse response = adminRevendedorService.actualizar(revendedorId, request);

        assertEquals("nuevo@mitienda.com", response.getUsuarioEmail());
        assertEquals(true, response.isUsuarioConfigurado());
        verify(passwordEncoder, org.mockito.Mockito.never()).encode(any());
    }

    @Test
    void pausarSincronizacionRegistraActorYFecha() {
        UUID revendedorId = UUID.randomUUID();
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        revendedor.setNombre("Tienda Demo");
        revendedor.setDominio("mitienda.com");
        revendedor.setActivo(true);
        RevendedorStorefront storefront = new RevendedorStorefront();
        storefront.setId(UUID.randomUUID());
        storefront.setRevendedor(revendedor);
        storefront.setNombreComercial("Mi Tienda");
        revendedor.setStorefront(storefront);

        AdminStorefrontRequest request = new AdminStorefrontRequest();
        request.setNombreComercial("Mi Tienda");
        request.setPublicada(true);
        request.setSincronizacionPausada(true);

        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        when(revendedorStorefrontRepository.findByRevendedor_Id(revendedorId))
                .thenReturn(Optional.of(storefront));
        when(revendedorStorefrontRepository.save(any(RevendedorStorefront.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@md.test", null));
        try {
            AdminRevendedorResponse response = adminRevendedorService.upsertStorefront(revendedorId, request);

            assertEquals(true, response.getStorefront().isSincronizacionPausada());
            assertEquals("admin@md.test", response.getStorefront().getSincronizacionPausadaPor());
            assertNotNull(response.getStorefront().getSincronizacionPausadaAt());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Rol rolRevendedor() {
        Rol rol = new Rol();
        rol.setId(UUID.randomUUID());
        rol.setNombre("REVENDEDOR");
        return rol;
    }
}
