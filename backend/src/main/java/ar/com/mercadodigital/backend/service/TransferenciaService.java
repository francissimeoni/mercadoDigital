package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.service.NotificationService;
import ar.com.mercadodigital.backend.service.postcompra.EventoComprobanteTransferenciaCargado;
import ar.com.mercadodigital.backend.service.postcompra.EventoTransferenciaSolicitada;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileAttribute;

import java.io.InputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import ar.com.mercadodigital.backend.dto.response.TransferenciaComprobanteResponse;
import ar.com.mercadodigital.backend.entity.Pago;
import ar.com.mercadodigital.backend.entity.PagoEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoEstado;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.repository.PagoRepository;
import ar.com.mercadodigital.backend.repository.PedidoRepository;
import ar.com.mercadodigital.backend.repository.RevendedorStorefrontRepository;
import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompra;
import ar.com.mercadodigital.backend.service.postcompra.BrandingPostCompraResolver;
import ar.com.mercadodigital.backend.service.postcompra.EmailSender;
import ar.com.mercadodigital.backend.service.postcompra.EventoPagoConfirmado;
import ar.com.mercadodigital.backend.service.postcompra.MensajeEmail;
import lombok.Getter;

@Service
public class TransferenciaService {
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp", "application/pdf");
    private static final long MAX_BYTES = 8L * 1024L * 1024L;

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;
    private final RevendedorStorefrontRepository storefrontRepository;
    private final BrandingPostCompraResolver brandingResolver;
    private final EmailSender emailSender;
    private final ApplicationEventPublisher eventPublisher;
    @Getter
    private final Path baseDir;

    @Transactional
    public String solicitar(UUID pedidoId) {
        Pedido pedido = this.pedidoRepository.findByIdForUpdate(pedidoId).orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (pedido.getEstadoPedido() != PedidoEstado.CREADO || pedido.getPago() == null
                || pedido.getPago().getEstadoPago() != PagoEstado.PENDIENTE) {
            throw new OperacionEnConflictoException("El pedido ya no admite solicitar una transferencia");
        }
        RevendedorStorefront storefront = this.storefrontRepository.findByRevendedor_Id(pedido.getRevendedor().getId()).orElseThrow(() -> new ResourceNotFoundException("Storefront no encontrado"));
        if (!storefront.isTransferenciaHabilitada() || !StringUtils.hasText(storefront.getTransferenciaAlias())) {
            throw new BadRequestException("La tienda no tiene habilitado el pago por transferencia");
        }
        Pago pago = pedido.getPago();
        pago.setMetodo("TRANSFERENCIA");
        pago.setEstadoPago(PagoEstado.PENDIENTE);
        pago.setTransferenciaToken(UUID.randomUUID().toString());
        pago.setTransferenciaAliasSnapshot(storefront.getTransferenciaAlias().trim());
        pago.setTransferenciaSolicitadaAt(LocalDateTime.now());
        this.pedidoRepository.save(pedido);
        this.eventPublisher.publishEvent(new EventoTransferenciaSolicitada(pedido.getId(), pedido.getRevendedor().getId(), pedido.getCliente().getEmail(), pedido.getCliente().getNombre(), pedido.getTrackingToken(), pedido.getTotalFinal(), pago.getTransferenciaAliasSnapshot(), storefront.getTransferenciaTitular(), pago.getTransferenciaToken()));
        return pago.getTransferenciaToken();
    }

    @Transactional
    public TransferenciaComprobanteResponse cargarComprobante(String token, MultipartFile archivo) {
        Pago referencia = this.pagoRepository.findByTransferenciaToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de transferencia no encontrada"));
        Pedido pedido = this.pedidoRepository.findByIdForUpdate(referencia.getPedido().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de transferencia no encontrada"));
        Pago pago = pedido.getPago();
        if (pago == null || !token.equals(pago.getTransferenciaToken())) {
            throw new ResourceNotFoundException("Solicitud de transferencia no encontrada");
        }
        if (pago.getPedido().getEstadoPedido() != PedidoEstado.CREADO || pago.getEstadoPago() != PagoEstado.PENDIENTE
                || !"TRANSFERENCIA".equalsIgnoreCase(pago.getMetodo())) {
            throw new OperacionEnConflictoException("El pedido ya no admite comprobantes de transferencia");
        }
        if (StringUtils.hasText(pago.getComprobanteArchivo())) {
            throw new OperacionEnConflictoException("El comprobante ya fue cargado; consulta el estado del pedido");
        }
        this.validarArchivo(archivo);
        String extension = this.extension(archivo);
        String filename = String.valueOf(pago.getPedido().getId()) + "-" + String.valueOf(UUID.randomUUID()) + extension;
        try {
            Files.createDirectories(this.baseDir, new FileAttribute[0]);
            try (InputStream input = archivo.getInputStream();){
                Files.copy(input, this.baseDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            try { Files.deleteIfExists(baseDir.resolve(filename)); } catch (IOException ignored) { }
                        }
                    }
                });
            }
        }
        catch (Exception exception) {
            throw new IllegalStateException("No se pudo guardar el comprobante", exception);
        }
        pago.setComprobanteArchivo(filename);
        pago.setComprobanteNombreOriginal(this.limitar(archivo.getOriginalFilename(), 255));
        pago.setComprobanteContentType(archivo.getContentType());
        pago.setComprobanteCargadoAt(LocalDateTime.now());
        this.pagoRepository.save(pago);
        this.notificarComprobanteAlRevendedor(pago.getPedido());
        this.eventPublisher.publishEvent(new EventoComprobanteTransferenciaCargado(pago.getPedido().getId(), pago.getPedido().getRevendedor() != null ? pago.getPedido().getRevendedor().getId() : null, this.resolverEmailComercial(pago.getPedido()), pago.getPedido().getRevendedor() != null ? pago.getPedido().getRevendedor().getDominio() : null));
        RevendedorStorefront storefront = this.storefrontRepository.findByRevendedor_Id(pago.getPedido().getRevendedor().getId()).orElse(null);
        return TransferenciaComprobanteResponse.builder().estado("EN_REVISION").mensaje("Gracias por tu compra. El pedido esta en proceso y el comprobante sera revisado.").trackingToken(pago.getPedido().getTrackingToken()).tiendaNombre(storefront != null ? storefront.getNombreComercial() : pago.getPedido().getRevendedor().getNombre()).telefono(storefront != null ? storefront.getTelefonoComercial() : null).email(storefront != null ? storefront.getEmailComercial() : null).build();
    }

    @Transactional
    public void confirmar(UUID revendedorId, UUID pedidoId) {
        Pedido pedido = pedidoRepository.findByIdForUpdate(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (!pedido.getRevendedor().getId().equals(revendedorId)) throw new ResourceNotFoundException("Pedido no encontrado");
        Pago pago = pedido.getPago();
        if (pago == null || !"TRANSFERENCIA".equals(pago.getMetodo()) || !StringUtils.hasText(pago.getComprobanteArchivo())) {
            throw new BadRequestException("El pedido no tiene un comprobante de transferencia cargado");
        }
        if (pedido.getEstadoPedido() == PedidoEstado.PAGADO && pago.getEstadoPago() == PagoEstado.CONFIRMADO) {
            // Una reparación de envío heredado no vuelve a emitir la confirmación de pago.
            if (pedido.getEnvio() != null && pedido.getEnvio().getEstadoEnvio() == EnvioEstado.PENDIENTE_PAGO) {
                pedido.getEnvio().setEstadoEnvio(EnvioEstado.PENDIENTE_PREPARACION);
                pedidoRepository.save(pedido);
            }
            return;
        }
        if (pedido.getEstadoPedido() != PedidoEstado.CREADO || pago.getEstadoPago() != PagoEstado.PENDIENTE) {
            throw new OperacionEnConflictoException("El pedido ya no admite confirmar la transferencia");
        }
        pago.setEstadoPago(PagoEstado.CONFIRMADO);
        pedido.setEstadoPedido(PedidoEstado.PAGADO);
        if (pedido.getEnvio() != null && EnvioEstado.PENDIENTE_PAGO.equals(pedido.getEnvio().getEstadoEnvio())) {
            pedido.getEnvio().setEstadoEnvio(EnvioEstado.PENDIENTE_PREPARACION);
        }
        pedidoRepository.save(pedido);
        eventPublisher.publishEvent(new EventoPagoConfirmado(pedido.getId(), pago.getId(), revendedorId,
                pedido.getCliente().getEmail(), pedido.getCliente().getNombre(), pedido.getTrackingToken(),
                pedido.getEstadoPedido().name(), pago.getMonto(), "ARS", LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public ComprobanteArchivo getComprobante(UUID revendedorId, UUID pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (!pedido.getRevendedor().getId().equals(revendedorId) || pedido.getPago() == null
                || pedido.getPago().getComprobanteArchivo() == null) throw new ResourceNotFoundException("Comprobante no encontrado");
        try {
            Path path = baseDir.resolve(pedido.getPago().getComprobanteArchivo()).toRealPath();
            if (!path.startsWith(baseDir.toRealPath()) || !Files.isRegularFile(path)) {
                throw new ResourceNotFoundException("Comprobante no encontrado");
            }
            return new ComprobanteArchivo(path, pedido.getPago().getComprobanteContentType(), pedido.getPago().getComprobanteNombreOriginal());
        } catch (IOException ex) {
            throw new ResourceNotFoundException("Comprobante no encontrado");
        }
    }

    public record ComprobanteArchivo(Path path, String contentType, String nombreOriginal) { }

    private void validarArchivo(MultipartFile archivo) {
        boolean pdfPorNombre;
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("El comprobante es obligatorio");
        }
        if (archivo.getSize() > 0x800000L) {
            throw new BadRequestException("El comprobante supera los 8 MB");
        }
        String contentType = archivo.getContentType();
        boolean tipoPermitido = StringUtils.hasText(contentType) && TIPOS_PERMITIDOS.contains(contentType);
        boolean bl = pdfPorNombre = archivo.getOriginalFilename() != null && archivo.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".pdf");
        if (!tipoPermitido && (!pdfPorNombre || StringUtils.hasText(contentType) && !"application/octet-stream".equals(contentType))) {
            throw new BadRequestException("Formato no permitido. Usa JPG, PNG, WEBP o PDF");
        }
        try (InputStream input = archivo.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean pdf = header.length >= 5 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F' && header[4] == '-';
            boolean png = header.length >= 8 && header[0] == (byte) 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                    && header[4] == 13 && header[5] == 10 && header[6] == 26 && header[7] == 10;
            boolean jpeg = header.length >= 3 && header[0] == (byte) 0xff && header[1] == (byte) 0xd8 && header[2] == (byte) 0xff;
            boolean webp = header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            if (!(pdf && ("application/pdf".equals(contentType) || pdfPorNombre && (contentType == null || "application/octet-stream".equals(contentType)))
                    || png && "image/png".equals(contentType)
                    || jpeg && "image/jpeg".equals(contentType)
                    || webp && "image/webp".equals(contentType))) {
                throw new BadRequestException("El archivo no coincide con un formato permitido");
            }
        } catch (IOException ex) {
            throw new BadRequestException("No se pudo leer el comprobante");
        }
    }

    private String extension(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        if ((contentType == null || "application/octet-stream".equals(contentType)) && archivo.getOriginalFilename() != null && archivo.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return ".pdf";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            default -> ".jpg";
        };
    }
    private String limitar(String value, int max) { return value == null ? "comprobante" : value.substring(0, Math.min(max, value.length())); }
    private String esc(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }

    private final NotificationService notificationService;

    public TransferenciaService(PagoRepository pagoRepository, PedidoRepository pedidoRepository, RevendedorStorefrontRepository storefrontRepository, BrandingPostCompraResolver brandingResolver, EmailSender emailSender, ApplicationEventPublisher eventPublisher, NotificationService notificationService, @Value(value="${app.transferencias.base-dir:storage/comprobantes-transferencia}") String baseDir) {
        this.pagoRepository = pagoRepository;
        this.pedidoRepository = pedidoRepository;
        this.storefrontRepository = storefrontRepository;
        this.brandingResolver = brandingResolver;
        this.emailSender = emailSender;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.baseDir = Paths.get(baseDir, new String[0]).toAbsolutePath().normalize();
    }

    private void notificarComprobanteAlRevendedor(Pedido pedido) {
        if (pedido.getRevendedor() == null || pedido.getRevendedor().getUsuario() == null) {
            return;
        }
        this.notificationService.createNotificationForUser(pedido.getRevendedor().getUsuario(), "COMPROBANTE_TRANSFERENCIA_CARGADO", "Nuevo comprobante para revisar", "El cliente cargo el comprobante del pedido " + String.valueOf(pedido.getId()) + ".");
    }

    private String resolverEmailComercial(Pedido pedido) {
        if (pedido.getRevendedor() == null) {
            return null;
        }
        RevendedorStorefront storefront = this.storefrontRepository.findByRevendedor_Id(pedido.getRevendedor().getId()).orElse(null);
        if (storefront != null && StringUtils.hasText(storefront.getEmailComercial())) {
            return storefront.getEmailComercial().trim();
        }
        return pedido.getRevendedor().getUsuario() != null ? pedido.getRevendedor().getUsuario().getEmail() : null;
    }
}
