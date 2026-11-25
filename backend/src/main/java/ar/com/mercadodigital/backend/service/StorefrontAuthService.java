package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.service.AuthService;
import ar.com.mercadodigital.backend.service.StorefrontService;
import java.util.Set;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import ar.com.mercadodigital.backend.dto.request.StorefrontClienteRegistroRequest;
import ar.com.mercadodigital.backend.dto.request.StorefrontLoginRequest;
import ar.com.mercadodigital.backend.dto.request.StorefrontRefreshRequest;
import ar.com.mercadodigital.backend.dto.response.StorefrontAuthResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.entity.AsesorVenta;
import ar.com.mercadodigital.backend.entity.ClienteRevendedor;
import ar.com.mercadodigital.backend.entity.Revendedor;
import ar.com.mercadodigital.backend.entity.Rol;
import ar.com.mercadodigital.backend.entity.StorefrontCredencial;
import ar.com.mercadodigital.backend.entity.TipoUsuario;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.AsesorVentaRepository;
import ar.com.mercadodigital.backend.repository.ClienteRevendedorRepository;
import ar.com.mercadodigital.backend.repository.RevendedorRepository;
import ar.com.mercadodigital.backend.repository.RolRepository;
import ar.com.mercadodigital.backend.repository.StorefrontCredencialRepository;
import ar.com.mercadodigital.backend.repository.UsuarioRepository;
import ar.com.mercadodigital.backend.security.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorefrontAuthService {

    private static final String ORIGEN_ALTA_REGISTRO = "STOREFRONT_REGISTRO_V1";
    private static final DateTimeFormatter ISSUED_AT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final StorefrontService storefrontService;
    private final AuthService authService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final RevendedorRepository revendedorRepository;
    private final ClienteRevendedorRepository clienteRevendedorRepository;
    private final AsesorVentaRepository asesorVentaRepository;
    private final RolRepository rolRepository;
    private final StorefrontCredencialRepository storefrontCredencialRepository;

    @Transactional(readOnly = true)
    public StorefrontAuthResponse login(StorefrontLoginRequest request) {
        StorefrontResponse storefront = resolverStorefront(request.getStorefrontHost());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Usuario administradorCentral = autenticarAdministradorCentral(email, request.getPassword());
        if (administradorCentral != null) {
            return emitirSesion(administradorCentral, storefront, false);
        }
        StorefrontCredencial credencial = storefrontCredencialRepository
                .findByRevendedor_IdAndEmailLoginIgnoreCase(storefront.getRevendedorId(), email)
                .orElse(null);
        Usuario usuario;
        if (credencial != null) {
            if (!credencial.isActivo()
                    || !credencial.getUsuario().isActivo()
                    || !passwordEncoder.matches(request.getPassword(), credencial.getPasswordHash())) {
                throw credencialesInvalidas();
            }
            usuario = credencial.getUsuario();
        } else {
            // Compatibilidad con las cuentas existentes previas al login por tienda.
            usuario = authService.autenticarUsuario(email, request.getPassword());
        }
        return emitirSesion(usuario, storefront, false);
    }

    private Usuario autenticarAdministradorCentral(String email, String password) {
        Usuario candidato = usuarioRepository.findByEmailIgnoreCase(email)
                .filter(Usuario::isActivo)
                .orElse(null);
        if (candidato == null || !authService.resolverRoles(candidato).contains("ADMIN_MD")) {
            return null;
        }
        if (!StringUtils.hasText(candidato.getPasswordHash())
                || !passwordEncoder.matches(password, candidato.getPasswordHash())) {
            return null;
        }
        return candidato;
    }

    @Transactional(readOnly=true)
    public StorefrontAuthResponse refresh(StorefrontRefreshRequest request) {
        try {
            JwtService.ClaimsJwt claims = this.jwtService.validarRefreshToken(request.getRefreshToken());
            Usuario usuario = this.usuarioRepository.findById(claims.usuarioId()).filter(Usuario::isActivo).orElseThrow(this::credencialesInvalidas);
            if (!usuario.getEmail().equalsIgnoreCase(claims.email())) {
                throw this.credencialesInvalidas();
            }
            StorefrontResponse storefront = this.resolverStorefront(request.getStorefrontHost());
            if (ROLES_SESION_CLIENTE.equals(claims.roles())) {
                return this.emitirSesionCliente(usuario, storefront);
            }
            return this.emitirSesion(usuario, storefront, false);
        }
        catch (IllegalArgumentException ex) {
            throw this.credencialesInvalidas();
        }
    }

    @Transactional
    public StorefrontAuthResponse registrarCliente(StorefrontClienteRegistroRequest request) {
        StorefrontResponse storefront = resolverStorefront(request.getStorefrontHost());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        Usuario existente = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);

        if (existente != null) {
            return loginClienteExistente(existente, request, storefront);
        }

        Usuario cliente = new Usuario();
        cliente.setNombre(request.getNombre().trim());
        cliente.setEmail(email);
        cliente.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        cliente.setTipoUsuario(TipoUsuario.CLIENTE);
        cliente.setActivo(true);
        aplicarDireccion(cliente, request);
        cliente.setRoles(new HashSet<>(java.util.Set.of(resolveRol("CLIENTE"))));
        Usuario guardado = usuarioRepository.save(cliente);
        asegurarClienteRevendedor(guardado, storefront.getRevendedorId());
        asegurarCredencial(guardado, storefront.getRevendedorId(), "CLIENTE");
        return emitirSesion(guardado, storefront, true);
    }

    private StorefrontAuthResponse loginClienteExistente(Usuario existente, StorefrontClienteRegistroRequest request, StorefrontResponse storefront) {
        boolean esCliente;
        boolean cuentaClienteSinClave;
        List<String> roles = this.authService.resolverRoles(existente);
        boolean bl = cuentaClienteSinClave = existente.isActivo() && TipoUsuario.CLIENTE.equals((Object)existente.getTipoUsuario()) && (existente.getPasswordHash() == null || existente.getPasswordHash().isBlank()) && roles.stream().noneMatch(rol -> rol.equals("REVENDEDOR") || rol.equals("PROVEEDOR") || rol.equals("ADMIN_MD") || rol.equals("LOGISTICA_MD"));
        if (cuentaClienteSinClave) {
            throw new OperacionEnConflictoException("Ese email ya tiene compras como invitado. Usa el acceso verificado para recuperarlas");
        }
        if (!existente.isActivo() || existente.getPasswordHash() == null || existente.getPasswordHash().isBlank() || !this.passwordEncoder.matches(request.getPassword(), existente.getPasswordHash())) {
            throw new OperacionEnConflictoException("Ya existe una cuenta con ese email");
        }
        boolean bl2 = esCliente = roles.contains("CLIENTE") && !roles.stream().anyMatch(rol -> rol.equals("REVENDEDOR") || rol.equals("PROVEEDOR") || rol.equals("ADMIN_MD") || rol.equals("LOGISTICA_MD"));
        if (!esCliente) {
            throw new OperacionEnConflictoException("Ya existe una cuenta con ese email");
        }
        this.asegurarClienteRevendedor(existente, storefront.getRevendedorId());
        this.asegurarCredencial(existente, storefront.getRevendedorId(), "CLIENTE");
        return this.emitirSesion(existente, storefront, true);
    }

    private StorefrontAuthResponse emitirSesion(Usuario usuario, StorefrontResponse storefront, boolean forzarCliente) {
        UUID storeRevendedorId = storefront.getRevendedorId();
        if (storeRevendedorId == null) {
            throw credencialesInvalidas();
        }

        List<String> roles = authService.resolverRoles(usuario);
        if (forzarCliente && !roles.contains("CLIENTE")) {
            throw credencialesInvalidas();
        }

        boolean esRevendedor = roles.contains("REVENDEDOR");
        boolean esAdminMd = roles.contains("ADMIN_MD");
        boolean esAsesor = roles.contains("ASESOR_VENTA");
        boolean esCliente = roles.contains("CLIENTE");
        boolean esOperativo = roles.stream().anyMatch(rol ->
                rol.equals("PROVEEDOR") || rol.equals("ADMIN_MD") || rol.equals("LOGISTICA_MD"));

        if (esOperativo && !esAdminMd && !esRevendedor && !esAsesor && !esCliente) {
            throw credencialesInvalidas();
        }

        String revendedorId = null;
        String clienteId = null;
        String asesorId = null;

        if (esAdminMd) {
            // El acceso administrativo queda contextualizado en la tienda desde la que se inicio sesion.
            revendedorId = storeRevendedorId.toString();
        } else if (esRevendedor) {
            String propio = authService.resolveRevendedorId(usuario.getId());
            if (propio == null || !storeRevendedorId.toString().equals(propio)) {
                throw credencialesInvalidas();
            }
            revendedorId = propio;
        } else if (esAsesor) {
            AsesorVenta asesor = asesorVentaRepository.findByUsuario_IdAndRevendedor_Id(usuario.getId(), storeRevendedorId)
                    .filter(AsesorVenta::isActivo)
                    .orElse(null);
            if (asesor == null) {
                throw credencialesInvalidas();
            }
            revendedorId = storeRevendedorId.toString();
            asesorId = asesor.getId().toString();
        } else if (esCliente) {
            ClienteRevendedor relacion = clienteRevendedorRepository
                    .findByUsuario_IdAndRevendedor_Id(usuario.getId(), storeRevendedorId)
                    .orElseThrow(this::credencialesInvalidas);
            if (!relacion.isActivo()) {
                throw credencialesInvalidas();
            }
            revendedorId = storeRevendedorId.toString();
            clienteId = usuario.getId().toString();
        } else {
            throw credencialesInvalidas();
        }

        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail(), roles);
        String refreshToken = jwtService.generarRefreshToken(usuario.getId(), usuario.getEmail());
        String nombre = usuario.getNombre() == null || usuario.getNombre().isBlank()
                ? usuario.getEmail()
                : usuario.getNombre();

        return StorefrontAuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(usuario.getEmail())
                .nombre(nombre)
                .issuedAt(LocalDateTime.now().format(ISSUED_AT))
                .revendedorId(revendedorId)
                .clienteId(clienteId)
                .asesorId(asesorId)
                .roles(roles)
                .build();
    }

    private StorefrontResponse resolverStorefront(String host) {
        try {
            return storefrontService.resolverPorHost(host);
        } catch (ResourceNotFoundException ex) {
            throw credencialesInvalidas();
        }
    }

    private void asegurarClienteRevendedor(Usuario cliente, UUID revendedorId) {
        if (revendedorId == null) {
            throw credencialesInvalidas();
        }
        Revendedor revendedor = revendedorRepository.findById(revendedorId)
                .orElseThrow(this::credencialesInvalidas);

        ClienteRevendedor relacion = clienteRevendedorRepository
                .findByUsuario_IdAndRevendedor_Id(cliente.getId(), revendedor.getId())
                .orElseGet(() -> {
                    ClienteRevendedor nueva = new ClienteRevendedor();
                    nueva.setUsuario(cliente);
                    nueva.setRevendedor(revendedor);
                    nueva.setFechaAlta(LocalDateTime.now());
                    nueva.setActivo(true);
                    nueva.setOrigenAlta(ORIGEN_ALTA_REGISTRO);
                    return clienteRevendedorRepository.save(nueva);
                });

        if (!relacion.isActivo()) {
            relacion.setActivo(true);
            clienteRevendedorRepository.save(relacion);
        }
    }

    private Rol resolveRol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setId(UUID.randomUUID());
                    rol.setNombre(nombre);
                    return rolRepository.save(rol);
                });
    }

    private void aplicarDireccion(Usuario cliente, StorefrontClienteRegistroRequest request) {
        cliente.setTelefonoWhatsapp(request.getTelefonoWhatsapp().trim());
        cliente.setProvincia(request.getProvincia().trim());
        cliente.setLocalidad(request.getLocalidad().trim());
        cliente.setCodigoPostal(request.getCodigoPostal().trim());
        cliente.setCalle(request.getCalle().trim());
        cliente.setNumero(request.getNumero().trim());
        cliente.setPisoDepto(normalizarOpcional(request.getPisoDepto()));
        cliente.setObservacionesEnvio(normalizarOpcional(request.getObservacionesEnvio()));
    }

    private void asegurarCredencial(Usuario usuario, UUID revendedorId, String tipoAcceso) {
        Revendedor revendedor = revendedorRepository.findById(revendedorId)
                .orElseThrow(this::credencialesInvalidas);
        StorefrontCredencial credencial = storefrontCredencialRepository
                .findByRevendedor_IdAndUsuario_Id(revendedorId, usuario.getId())
                .orElseGet(StorefrontCredencial::new);
        credencial.setRevendedor(revendedor);
        credencial.setUsuario(usuario);
        credencial.setTipoAcceso(tipoAcceso);
        credencial.setEmailLogin(usuario.getEmail().toLowerCase(Locale.ROOT));
        credencial.setPasswordHash(usuario.getPasswordHash());
        credencial.setActivo(usuario.isActivo());
        storefrontCredencialRepository.save(credencial);
    }

    private String normalizarOpcional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
    }

    private static final List<String> ROLES_SESION_CLIENTE = List.of("CLIENTE");

    @Transactional
    public StorefrontAuthResponse autenticarClienteGoogle(String storefrontHost, String emailGoogle, String nombreGoogle) {
        StorefrontResponse storefront = this.resolverStorefront(storefrontHost);
        String email = emailGoogle.trim().toLowerCase(Locale.ROOT);
        Usuario cliente = this.usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
        if (cliente == null) {
            cliente = new Usuario();
            cliente.setNombre(StringUtils.hasText(nombreGoogle) ? nombreGoogle.trim() : email);
            cliente.setEmail(email);
            cliente.setTipoUsuario(TipoUsuario.CLIENTE);
            cliente.setActivo(true);
            cliente.setRoles(new HashSet<Rol>(Set.of(this.resolveRol("CLIENTE"))));
            cliente = this.usuarioRepository.save(cliente);
        } else {
            List<String> roles = this.authService.resolverRoles(cliente);
            boolean esCuentaOperativa = roles.stream().anyMatch(rol -> rol.equals("REVENDEDOR") || rol.equals("PROVEEDOR") || rol.equals("ADMIN_MD") || rol.equals("LOGISTICA_MD") || rol.equals("ASESOR_VENTA"));
            if (!cliente.isActivo()) {
                throw new OperacionEnConflictoException("La cuenta asociada a ese correo esta inactiva");
            }
            if (!esCuentaOperativa && !TipoUsuario.CLIENTE.equals((Object)cliente.getTipoUsuario())) {
                throw new OperacionEnConflictoException("Ese correo no puede asociarse a una cuenta de cliente");
            }
            if (!esCuentaOperativa && !roles.contains("CLIENTE")) {
                cliente.setRoles(new HashSet<Rol>(Set.of(this.resolveRol("CLIENTE"))));
            }
            if (!esCuentaOperativa && !StringUtils.hasText(cliente.getNombre()) && StringUtils.hasText(nombreGoogle)) {
                cliente.setNombre(nombreGoogle.trim());
            }
            if (!esCuentaOperativa) {
                cliente = this.usuarioRepository.save(cliente);
            }
        }
        this.asegurarClienteRevendedor(cliente, storefront.getRevendedorId());
        return this.emitirSesionCliente(cliente, storefront);
    }

    private StorefrontAuthResponse emitirSesionCliente(Usuario usuario, StorefrontResponse storefront) {
        UUID revendedorId = storefront.getRevendedorId();
        if (revendedorId == null || !usuario.isActivo()) {
            throw this.credencialesInvalidas();
        }
        ClienteRevendedor relacion = this.clienteRevendedorRepository.findByUsuario_IdAndRevendedor_Id(usuario.getId(), revendedorId).filter(ClienteRevendedor::isActivo).orElseThrow(this::credencialesInvalidas);
        String token = this.jwtService.generarToken(usuario.getId(), usuario.getEmail(), ROLES_SESION_CLIENTE);
        String refreshToken = this.jwtService.generarRefreshToken(usuario.getId(), usuario.getEmail(), ROLES_SESION_CLIENTE);
        String nombre = usuario.getNombre() == null || usuario.getNombre().isBlank() ? usuario.getEmail() : usuario.getNombre();
        return StorefrontAuthResponse.builder().token(token).refreshToken(refreshToken).email(usuario.getEmail()).nombre(nombre).issuedAt(LocalDateTime.now().format(ISSUED_AT)).revendedorId(relacion.getRevendedor().getId().toString()).clienteId(usuario.getId().toString()).roles(ROLES_SESION_CLIENTE).build();
    }
}
