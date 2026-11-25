package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import ar.com.mercadodigital.backend.dto.request.StorefrontClienteRegistroRequest;
import ar.com.mercadodigital.backend.dto.request.StorefrontLoginRequest;
import ar.com.mercadodigital.backend.dto.request.StorefrontRefreshRequest;
import ar.com.mercadodigital.backend.dto.response.StorefrontAuthResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.entity.ClienteRevendedor;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.Rol;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.repository.AsesorVentaRepository;
import ar.com.mercadodigital.backend.repository.ClienteRevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RolRepository;
import ar.com.mercadodigital.backend.repository.StorefrontCredencialRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import ar.com.mercadodigital.backend.security.JwtService;

@ExtendWith(MockitoExtension.class)
class StorefrontAuthServiceTest {

    @Mock
    private StorefrontService storefrontService;
    @Mock
    private AuthService authService;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RevendedorRepository revendedorRepository;
    @Mock
    private ClienteRevendedorRepository clienteRevendedorRepository;
    @Mock
    private AsesorVentaRepository asesorVentaRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private StorefrontCredencialRepository storefrontCredencialRepository;

    @InjectMocks
    private StorefrontAuthService storefrontAuthService;

    @Test
    void refreshRenuevaLaSesionSinPedirCredenciales() {
        UUID revendedorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId, "tienda@test.com", "Infonel");
        StorefrontRefreshRequest request = new StorefrontRefreshRequest();
        request.setStorefrontHost("www.infonel.com.ar");
        request.setRefreshToken("refresh-anterior");

        when(jwtService.validarRefreshToken("refresh-anterior"))
                .thenReturn(new JwtService.ClaimsJwt(usuarioId, "tienda@test.com", List.of()));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(storefrontService.resolverPorHost("www.infonel.com.ar")).thenReturn(storefront(revendedorId));
        when(authService.resolverRoles(usuario)).thenReturn(List.of("REVENDEDOR"));
        when(authService.resolveRevendedorId(usuarioId)).thenReturn(revendedorId.toString());
        when(jwtService.generarToken(usuarioId, "tienda@test.com", List.of("REVENDEDOR")))
                .thenReturn("access-nuevo");
        when(jwtService.generarRefreshToken(usuarioId, "tienda@test.com"))
                .thenReturn("refresh-nuevo");

        StorefrontAuthResponse response = storefrontAuthService.refresh(request);

        assertEquals("access-nuevo", response.getToken());
        assertEquals("refresh-nuevo", response.getRefreshToken());
        assertEquals(revendedorId.toString(), response.getRevendedorId());
    }

    @Test
    void loginRevendedorDeLaTiendaDevuelveSesion() {
        UUID revendedorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId, "tienda@test.com", "Infonel");
        StorefrontLoginRequest request = loginRequest("www.infonel.com.ar", "tienda@test.com", "secret");

        when(storefrontService.resolverPorHost("www.infonel.com.ar")).thenReturn(storefront(revendedorId));
        when(authService.autenticarUsuario("tienda@test.com", "secret")).thenReturn(usuario);
        when(authService.resolverRoles(usuario)).thenReturn(List.of("REVENDEDOR"));
        when(authService.resolveRevendedorId(usuarioId)).thenReturn(revendedorId.toString());
        when(jwtService.generarToken(eq(usuarioId), eq("tienda@test.com"), eq(List.of("REVENDEDOR"))))
                .thenReturn("jwt-storefront");

        StorefrontAuthResponse response = storefrontAuthService.login(request);

        assertEquals("jwt-storefront", response.getToken());
        assertEquals("tienda@test.com", response.getEmail());
        assertEquals("Infonel", response.getNombre());
        assertEquals(revendedorId.toString(), response.getRevendedorId());
        assertNull(response.getClienteId());
        assertEquals(List.of("REVENDEDOR"), response.getRoles());
    }

    @Test
    void loginAdministradorSeContextualizaEnLaTiendaActual() {
        UUID revendedorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId, "admin@mercado-digital.com.ar", "Administrador");
        StorefrontLoginRequest request = loginRequest(
                "www.infonel.com.ar", "admin@mercado-digital.com.ar", "secret");

        when(storefrontService.resolverPorHost("www.infonel.com.ar")).thenReturn(storefront(revendedorId));
        when(authService.autenticarUsuario("admin@mercado-digital.com.ar", "secret")).thenReturn(usuario);
        when(authService.resolverRoles(usuario)).thenReturn(List.of("ADMIN_MD"));
        when(jwtService.generarToken(usuarioId, "admin@mercado-digital.com.ar", List.of("ADMIN_MD")))
                .thenReturn("jwt-admin");

        StorefrontAuthResponse response = storefrontAuthService.login(request);

        assertEquals("jwt-admin", response.getToken());
        assertEquals(revendedorId.toString(), response.getRevendedorId());
        assertEquals(List.of("ADMIN_MD"), response.getRoles());
        assertNull(response.getClienteId());
    }

    @Test
    void credencialClienteNoTapaLasCredencialesDelAdministradorCentral() {
        UUID revendedorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Usuario administrador = usuario(usuarioId, "admin@mercado-digital.com.ar", "Administrador");
        administrador.setTipoUsuario(TipoUsuario.ADMIN_MD);
        administrador.setPasswordHash("hash-admin");
        StorefrontLoginRequest request = loginRequest(
                "www.infonel.com.ar", "admin@mercado-digital.com.ar", "clave-admin");

        when(storefrontService.resolverPorHost("www.infonel.com.ar")).thenReturn(storefront(revendedorId));
        when(usuarioRepository.findByEmailIgnoreCase("admin@mercado-digital.com.ar"))
                .thenReturn(Optional.of(administrador));
        when(authService.resolverRoles(administrador)).thenReturn(List.of("ADMIN_MD"));
        when(passwordEncoder.matches("clave-admin", "hash-admin")).thenReturn(true);
        when(jwtService.generarToken(usuarioId, "admin@mercado-digital.com.ar", List.of("ADMIN_MD")))
                .thenReturn("jwt-admin-central");

        StorefrontAuthResponse response = storefrontAuthService.login(request);

        assertEquals("jwt-admin-central", response.getToken());
        assertEquals(revendedorId.toString(), response.getRevendedorId());
        assertEquals(List.of("ADMIN_MD"), response.getRoles());
        verifyNoInteractions(storefrontCredencialRepository);
    }

    @Test
    void loginRevendedorDeOtraTiendaEsRechazado() {
        UUID storeRevendedorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId, "otro@test.com", "Otro");
        StorefrontLoginRequest request = loginRequest("www.infonel.com.ar", "otro@test.com", "secret");

        when(storefrontService.resolverPorHost("www.infonel.com.ar")).thenReturn(storefront(storeRevendedorId));
        when(authService.autenticarUsuario("otro@test.com", "secret")).thenReturn(usuario);
        when(authService.resolverRoles(usuario)).thenReturn(List.of("REVENDEDOR"));
        when(authService.resolveRevendedorId(usuarioId)).thenReturn(UUID.randomUUID().toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> storefrontAuthService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void loginClienteVinculadoDevuelveSesion() {
        UUID revendedorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId, "cliente@test.com", "Ana");
        ClienteRevendedor relacion = new ClienteRevendedor();
        relacion.setActivo(true);

        when(storefrontService.resolverPorHost("www.infonel.com.ar")).thenReturn(storefront(revendedorId));
        when(authService.autenticarUsuario("cliente@test.com", "secret")).thenReturn(usuario);
        when(authService.resolverRoles(usuario)).thenReturn(List.of("CLIENTE"));
        when(clienteRevendedorRepository.findByUsuario_IdAndRevendedor_Id(usuarioId, revendedorId))
                .thenReturn(Optional.of(relacion));
        when(jwtService.generarToken(eq(usuarioId), eq("cliente@test.com"), eq(List.of("CLIENTE"))))
                .thenReturn("jwt-cliente");

        StorefrontAuthResponse response = storefrontAuthService.login(loginRequest(
                "www.infonel.com.ar", "cliente@test.com", "secret"));

        assertEquals("jwt-cliente", response.getToken());
        assertEquals(usuarioId.toString(), response.getClienteId());
        assertEquals(revendedorId.toString(), response.getRevendedorId());
        assertEquals(List.of("CLIENTE"), response.getRoles());
    }

    @Test
    void registroClienteNuevoCreaUsuarioYRelacion() {
        UUID revendedorId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Usuario guardado = usuario(usuarioId, "nuevo@test.com", "Nuevo");
        Revendedor revendedor = new Revendedor();
        revendedor.setId(revendedorId);
        Rol rolCliente = new Rol();
        rolCliente.setNombre("CLIENTE");

        when(storefrontService.resolverPorHost("www.infonel.com.ar")).thenReturn(storefront(revendedorId));
        when(usuarioRepository.findByEmailIgnoreCase("nuevo@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret1")).thenReturn("hash");
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);
        when(revendedorRepository.findById(revendedorId)).thenReturn(Optional.of(revendedor));
        ClienteRevendedor relacion = new ClienteRevendedor();
        relacion.setActivo(true);
        when(clienteRevendedorRepository.findByUsuario_IdAndRevendedor_Id(usuarioId, revendedorId))
                .thenReturn(Optional.empty(), Optional.of(relacion));
        when(clienteRevendedorRepository.save(any(ClienteRevendedor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authService.resolverRoles(guardado)).thenReturn(List.of("CLIENTE"));
        when(jwtService.generarToken(eq(usuarioId), eq("nuevo@test.com"), eq(List.of("CLIENTE"))))
                .thenReturn("jwt-nuevo");

        StorefrontAuthResponse response = storefrontAuthService.registrarCliente(registroRequest());

        assertEquals("jwt-nuevo", response.getToken());
        assertEquals(usuarioId.toString(), response.getClienteId());
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertEquals(TipoUsuario.CLIENTE, usuarioCaptor.getValue().getTipoUsuario());
        assertEquals("hash", usuarioCaptor.getValue().getPasswordHash());
    }

    @Test
    void registroEmailExistenteConPasswordIncorrectaConflicto() {
        UUID revendedorId = UUID.randomUUID();
        Usuario existente = usuario(UUID.randomUUID(), "nuevo@test.com", "Nuevo");
        existente.setPasswordHash("hash");
        existente.setTipoUsuario(TipoUsuario.CLIENTE);

        when(storefrontService.resolverPorHost("www.infonel.com.ar")).thenReturn(storefront(revendedorId));
        when(usuarioRepository.findByEmailIgnoreCase("nuevo@test.com")).thenReturn(Optional.of(existente));
        when(passwordEncoder.matches("secret1", "hash")).thenReturn(false);

        assertThrows(OperacionEnConflictoException.class,
                () -> storefrontAuthService.registrarCliente(registroRequest()));
    }

    @Test
    void googleDeCuentaOperativaEmiteSoloClienteYRefreshNoRecuperaPrivilegios() {
        UUID storeId = UUID.randomUUID();
        Usuario user = usuario(UUID.randomUUID(), "admin@test.com", "Admin");
        Revendedor store = new Revendedor(); store.setId(storeId);
        ClienteRevendedor relation = new ClienteRevendedor(); relation.setUsuario(user); relation.setRevendedor(store); relation.setActivo(true);
        when(storefrontService.resolverPorHost("shop.test")).thenReturn(storefront(storeId));
        when(usuarioRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(authService.resolverRoles(user)).thenReturn(List.of("ADMIN_MD", "REVENDEDOR"));
        when(revendedorRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(clienteRevendedorRepository.findByUsuario_IdAndRevendedor_Id(user.getId(), storeId)).thenReturn(Optional.of(relation));
        when(jwtService.generarToken(user.getId(), user.getEmail(), List.of("CLIENTE"))).thenReturn("limited-access");
        when(jwtService.generarRefreshToken(user.getId(), user.getEmail(), List.of("CLIENTE"))).thenReturn("limited-refresh");
        var session = storefrontAuthService.autenticarClienteGoogle("shop.test", user.getEmail(), "Nombre Google");
        assertEquals(List.of("CLIENTE"), session.getRoles());
        assertEquals(user.getId().toString(), session.getClienteId());
        org.mockito.Mockito.verify(usuarioRepository, org.mockito.Mockito.never()).save(any());
        org.mockito.Mockito.clearInvocations(authService);
        when(jwtService.validarRefreshToken("limited-refresh")).thenReturn(new JwtService.ClaimsJwt(user.getId(), user.getEmail(), List.of("CLIENTE")));
        when(usuarioRepository.findById(user.getId())).thenReturn(Optional.of(user));
        var request = new StorefrontRefreshRequest(); request.setStorefrontHost("shop.test"); request.setRefreshToken("limited-refresh");
        assertEquals(List.of("CLIENTE"), storefrontAuthService.refresh(request).getRoles());
        verifyNoInteractions(authService);
    }

    private static StorefrontLoginRequest loginRequest(String host, String email, String password) {
        StorefrontLoginRequest request = new StorefrontLoginRequest();
        request.setStorefrontHost(host);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private static StorefrontClienteRegistroRequest registroRequest() {
        StorefrontClienteRegistroRequest request = new StorefrontClienteRegistroRequest();
        request.setStorefrontHost("www.infonel.com.ar");
        request.setNombre("Nuevo");
        request.setEmail("nuevo@test.com");
        request.setPassword("secret1");
        request.setTelefonoWhatsapp("111");
        request.setProvincia("Santa Fe");
        request.setLocalidad("Rosario");
        request.setCodigoPostal("2000");
        request.setCalle("Calle");
        request.setNumero("1");
        return request;
    }

    private static StorefrontResponse storefront(UUID revendedorId) {
        return StorefrontResponse.builder()
                .revendedorId(revendedorId)
                .dominio("infonel.com.ar")
                .nombreComercial("INFONEL")
                .publicada(true)
                .build();
    }

    private static Usuario usuario(UUID id, String email, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setActivo(true);
        usuario.setTipoUsuario(TipoUsuario.REVENDEDOR);
        return usuario;
    }
}
