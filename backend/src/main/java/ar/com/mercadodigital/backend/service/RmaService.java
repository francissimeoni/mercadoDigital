package ar.com.mercadodigital.backend.service;

import java.math.BigDecimal;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.util.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.mercadodigital.backend.dto.request.AgregarRmaMensajeRequest;
import ar.com.mercadodigital.backend.dto.request.ActualizarReposicionManualRequest;
import ar.com.mercadodigital.backend.dto.request.PublicSolicitarRmaRequest;
import ar.com.mercadodigital.backend.dto.request.ResolverRmaRequest;
import ar.com.mercadodigital.backend.dto.request.SolicitarRmaRequest;
import ar.com.mercadodigital.backend.dto.response.PublicRmaResponse;
import ar.com.mercadodigital.backend.dto.response.ReposicionBitacoraPasoResponse;
import ar.com.mercadodigital.backend.dto.response.RmaAdjuntoResponse;
import ar.com.mercadodigital.backend.dto.response.RmaMensajeResponse;
import ar.com.mercadodigital.backend.dto.response.RmaResponse;
import ar.com.mercadodigital.backend.entity.AuditoriaEvento;
import ar.com.mercadodigital.backend.entity.EnvioEstado;
import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.PedidoItem;
import ar.com.mercadodigital.backend.entity.RMA;
import ar.com.mercadodigital.backend.entity.RmaAdjunto;
import ar.com.mercadodigital.backend.entity.RmaAutorTipo;
import ar.com.mercadodigital.backend.entity.RmaEstado;
import ar.com.mercadodigital.backend.entity.RmaMensaje;
import ar.com.mercadodigital.backend.entity.Usuario;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.exception.OperacionEnConflictoException;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.repository.AuditoriaEventoRepository;
import ar.com.mercadodigital.backend.repository.PedidoItemRepository;
import ar.com.mercadodigital.backend.repository.RMARepository;
import ar.com.mercadodigital.backend.repository.RmaAdjuntoRepository;
import ar.com.mercadodigital.backend.repository.RmaMensajeRepository;
import ar.com.mercadodigital.backend.entity.Transportista;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaCerrado;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaEnvioSolicitado;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaReposicionEntregada;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaReposicionReenviada;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaSolicitado;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RmaService {

    private static final List<RmaEstado> ESTADOS_RMA_ABIERTOS = List.of(
            RmaEstado.SOLICITADO,
            RmaEstado.APROBADO,
            RmaEstado.EN_TRANSITO_DEVOLUCION,
            RmaEstado.RECIBIDO_EN_DEPOSITO);
    private static final Set<String> CONTENT_TYPES_IMAGEN_PERMITIDOS = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final RMARepository rmaRepository;
    private final PedidoItemRepository pedidoItemRepository;
    private final AuditoriaEventoRepository auditoriaEventoRepository;
    private final NotificationService notificationService;
    private final RmaMensajeRepository rmaMensajeRepository;
    private final RmaAdjuntoRepository rmaAdjuntoRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransportistaService transportistaService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RmaResponse solicitar(SolicitarRmaRequest request) {
        PedidoItem pedidoItem = getPedidoItem(request.getPedidoItemId());
        RMA saved = crearCasoRma(pedidoItem, pedidoItem.getPedido().getCliente(), request.getMotivo(), request.getDescripcion(), List.of());
        return toResponse(saved);
    }

    @Transactional
    public PublicRmaResponse solicitarPublico(Pedido pedido, PublicSolicitarRmaRequest request, List<MultipartFile> archivos) {
        PedidoItem pedidoItem = getPedidoItem(request.getPedidoItemId());
        if (!pedido.getId().equals(pedidoItem.getPedido().getId())) {
            throw new ResourceNotFoundException("Pedido item no corresponde al tracking actual: " + request.getPedidoItemId());
        }

        RMA saved = crearCasoRma(pedidoItem, pedido.getCliente(), request.getMotivo(), request.getMensajeInicial(), archivos);
        return toPublicResponse(saved);
    }

    @Transactional
    public RmaResponse aprobar(UUID id) {
        RMA rma = getRma(id);
        validarTransicion(rma.getEstadoRma(), RmaEstado.SOLICITADO, RmaEstado.APROBADO);
        rma.setEstadoRma(RmaEstado.APROBADO);
        RMA saved = rmaRepository.save(rma);
        registrarAuditoria("RMA_APROBADO", saved.getId(), "{\"rmaId\":\"" + saved.getId() + "\"}");
        return toResponse(saved);
    }

    @Transactional
    public RmaResponse solicitarEnvio(UUID id) {
        RMA rma = getRma(id);
        validarTransicion(rma.getEstadoRma(), RmaEstado.APROBADO, RmaEstado.EN_TRANSITO_DEVOLUCION);
        rma.setEstadoRma(RmaEstado.EN_TRANSITO_DEVOLUCION);
        RMA saved = rmaRepository.save(rma);
        registrarAuditoria("RMA_ENVIO_SOLICITADO", saved.getId(), "{\"rmaId\":\"" + saved.getId() + "\"}");
        publicarEventoRmaEnvioSolicitado(saved);
        return toResponse(saved);
    }

    @Transactional
    public RmaResponse recibirEnDeposito(UUID id) {
        RMA rma = getRma(id);
        if (!(RmaEstado.APROBADO.equals(rma.getEstadoRma()) || RmaEstado.EN_TRANSITO_DEVOLUCION.equals(rma.getEstadoRma()))) {
            throw new OperacionEnConflictoException(
                    "Transicion RMA invalida. Estado actual: " + rma.getEstadoRma() + ", esperado: APROBADO o EN_TRANSITO_DEVOLUCION");
        }
        rma.setEstadoRma(RmaEstado.RECIBIDO_EN_DEPOSITO);
        RMA saved = rmaRepository.save(rma);
        registrarAuditoria("RMA_RECIBIDO_EN_DEPOSITO", saved.getId(), "{\"rmaId\":\"" + saved.getId() + "\"}");
        return toResponse(saved);
    }

    @Transactional
    public RmaResponse solicitarReposicionProveedor(UUID id, ActualizarReposicionManualRequest request) {
        RMA rma = getRma(id);
        validarCircuitoReposicion(rma);
        if (auditoriaEventoRepository.existsByTipoEventoAndReferenciaId("RMA_REPOSICION_SOLICITADA_PROVEEDOR", id)) {
            throw new OperacionEnConflictoException("La reposicion ya fue solicitada al proveedor para este caso.");
        }
        registrarAuditoria("RMA_REPOSICION_SOLICITADA_PROVEEDOR", id, toJson(buildPayloadReposicion(request, null, null)));
        return toResponse(rmaRepository.findById(id).orElseThrow());
    }

    @Transactional
    public RmaResponse recibirReposicionMd(UUID id, ActualizarReposicionManualRequest request) {
        RMA rma = getRma(id);
        validarCircuitoReposicion(rma);
        requireEvento("RMA_REPOSICION_SOLICITADA_PROVEEDOR", id, "Primero debes solicitar la reposicion al proveedor.");
        if (auditoriaEventoRepository.existsByTipoEventoAndReferenciaId("RMA_REPOSICION_RECIBIDA_MD", id)) {
            throw new OperacionEnConflictoException("La reposicion ya fue recibida por MD para este caso.");
        }
        registrarAuditoria("RMA_REPOSICION_RECIBIDA_MD", id, toJson(buildPayloadReposicion(request, null, null)));
        return toResponse(rmaRepository.findById(id).orElseThrow());
    }

    @Transactional
    public RmaResponse reenviarReposicionCliente(UUID id, ActualizarReposicionManualRequest request) {
        RMA rma = getRma(id);
        validarCircuitoReposicion(rma);
        requireEvento("RMA_REPOSICION_RECIBIDA_MD", id, "Primero debes registrar que MD recibio la reposicion.");
        if (request == null || (!StringUtils.hasText(request.getTransportistaNombre()) && request.getTransportistaId() == null)
                || !StringUtils.hasText(request.getTracking())) {
            throw new BadRequestException("Debes indicar transportista y tracking para reenviar la reposicion al cliente.");
        }
        if (auditoriaEventoRepository.existsByTipoEventoAndReferenciaId("RMA_REPOSICION_REENVIADA_CLIENTE", id)) {
            throw new OperacionEnConflictoException("La reposicion ya fue reenviada al cliente para este caso.");
        }
        Transportista transportista = transportistaService.resolveOrCreate(
                request.getTransportistaId(),
                request.getTransportistaNombre());
        registrarAuditoria("RMA_REPOSICION_REENVIADA_CLIENTE", id, toJson(buildPayloadReposicion(request, transportista.getId(), transportista.getNombre(), request.getTracking().trim())));
        publicarEventoRmaReposicionReenviada(rma, request.getTracking().trim(), transportista.getNombre());
        return toResponse(rmaRepository.findById(id).orElseThrow());
    }

    @Transactional
    public RmaResponse entregarReposicionCliente(UUID id, ActualizarReposicionManualRequest request) {
        RMA rma = getRma(id);
        validarCircuitoReposicion(rma);
        requireEvento("RMA_REPOSICION_REENVIADA_CLIENTE", id, "Primero debes reenviar la reposicion al cliente.");
        if (auditoriaEventoRepository.existsByTipoEventoAndReferenciaId("RMA_REPOSICION_ENTREGADA_CLIENTE", id)) {
            throw new OperacionEnConflictoException("La reposicion ya fue marcada como entregada para este caso.");
        }
        ReposicionPayload reenvio = parsePayload(
                auditoriaEventoRepository.findTopByTipoEventoAndReferenciaIdOrderByFechaEventoDesc("RMA_REPOSICION_REENVIADA_CLIENTE", id)
                        .map(AuditoriaEvento::getPayloadJson)
                        .orElse(null));
        registrarAuditoria("RMA_REPOSICION_ENTREGADA_CLIENTE", id, toJson(buildPayloadReposicion(
                request,
                reenvio.transportistaId(),
                reenvio.transportistaNombre(),
                reenvio.tracking())));
        publicarEventoRmaReposicionEntregada(rma, reenvio.tracking(), reenvio.transportistaNombre());
        return toResponse(rmaRepository.findById(id).orElseThrow());
    }

    @Transactional
    public RmaResponse resolver(UUID id, ResolverRmaRequest request, List<MultipartFile> archivos) {
        RMA rma = getRma(id);
        if (!RmaEstado.RECIBIDO_EN_DEPOSITO.equals(rma.getEstadoRma())) {
            throw new OperacionEnConflictoException(
                    "Transicion RMA invalida. Estado actual: " + rma.getEstadoRma() + ", esperado: RECIBIDO_EN_DEPOSITO");
        }

        rma.setResolucionFinal(request.getResolucionFinal());
        rma.setObservacionesInspeccion(request.getObservacionesInspeccion());
        rma.setResponsableProbable(request.getResponsableProbable());
        rma.setMontoCompensacion(request.getMontoCompensacion());
        rma.setReferenciaCompensacion(request.getReferenciaCompensacion());

        if ("RECHAZADO".equalsIgnoreCase(request.getResolucionFinal())) {
            rma.setEstadoRma(RmaEstado.RECHAZADO);
            RMA saved = rmaRepository.save(rma);
            registrarCierreInterno(saved, request, archivos);
            registrarAuditoria("RMA_RECHAZADO", saved.getId(), "{\"rmaId\":\"" + saved.getId() + "\"}");
            publicarEventoRmaCerrado(saved);
            return toResponse(saved);
        }

        if ("REEMBOLSADO".equalsIgnoreCase(request.getResolucionFinal())) {
            rma.setEstadoRma(RmaEstado.REEMBOLSADO);
            RMA saved = rmaRepository.save(rma);
            registrarCierreInterno(saved, request, archivos);
            registrarAuditoria("RMA_REEMBOLSADO", saved.getId(), "{\"rmaId\":\"" + saved.getId() + "\"}");
            publicarEventoRmaCerrado(saved);
            return toResponse(saved);
        }

        rma.setEstadoRma(RmaEstado.RESUELTO);
        RMA saved = rmaRepository.save(rma);
        registrarCierreInterno(saved, request, archivos);
        registrarAuditoria("RMA_RESUELTO", saved.getId(), "{\"rmaId\":\"" + saved.getId() + "\"}");
        publicarEventoRmaCerrado(saved);
        return toResponse(saved);
    }

    @Transactional
    public RmaResponse agregarMensajeAdmin(UUID rmaId, AgregarRmaMensajeRequest request, List<MultipartFile> archivos) {
        RMA rma = getRma(rmaId);
        guardarMensaje(rma, RmaAutorTipo.ADMIN_MD, "Admin MD", request.getMensaje(), request.isInterno(), archivos);
        registrarAuditoria("RMA_MENSAJE_ADMIN", rma.getId(), "{\"rmaId\":\"" + rma.getId() + "\",\"interno\":" + request.isInterno() + "}");
        return toResponse(rmaRepository.findById(rmaId).orElseThrow());
    }

    @Transactional
    public PublicRmaResponse agregarMensajeCliente(RMA rma, String mensaje, List<MultipartFile> archivos) {
        guardarMensaje(rma, RmaAutorTipo.CLIENTE, rma.getCreadoPor() != null ? rma.getCreadoPor().getNombre() : "Cliente", mensaje, false, archivos);
        registrarAuditoria("RMA_MENSAJE_CLIENTE", rma.getId(), "{\"rmaId\":\"" + rma.getId() + "\"}");
        notificationService.createNotificationsForRole(
                "ADMIN_MD",
                "RMA_MENSAJE_CLIENTE",
                "Nuevo mensaje de postventa",
                "El caso " + rma.getId() + " recibio un nuevo mensaje del cliente.");
        return toPublicResponse(rmaRepository.findById(rma.getId()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<RmaResponse> getVisibles(String email, Collection<String> roles, UUID pedidoId) {
        return obtenerRmasVisibles(email, roles, pedidoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RmaResponse getDetalleVisible(UUID id, String email, Collection<String> roles) {
        RMA rma = esAdmin(roles)
                ? getRma(id)
                : rmaRepository.findByIdAndCreadoPor_EmailIgnoreCase(id, email)
                        .orElseThrow(() -> new ResourceNotFoundException("RMA no encontrado: " + id));
        return toResponse(rma);
    }

    @Transactional(readOnly = true)
    public List<PublicRmaResponse> getPublicByPedido(UUID pedidoId) {
        return rmaRepository.findByPedidoItem_Pedido_IdOrderByCreatedAtDesc(pedidoId).stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<RMA> getVisiblePublico(UUID rmaId, UUID pedidoId) {
        return rmaRepository.findById(rmaId)
                .filter(rma -> rma.getPedidoItem() != null
                        && rma.getPedidoItem().getPedido() != null
                        && pedidoId.equals(rma.getPedidoItem().getPedido().getId()));
    }

    @Transactional
    public void registrarDescargaRotuloDevolucion(UUID rmaId) {
        if (!auditoriaEventoRepository.existsByTipoEventoAndReferenciaId("RMA_ROTULO_DEVOLUCION_DESCARGADO", rmaId)) {
            registrarAuditoria("RMA_ROTULO_DEVOLUCION_DESCARGADO", rmaId, "{\"rmaId\":\"" + rmaId + "\"}");
        }
    }

    @Transactional
    public void registrarGeneracionRotuloReposicion(UUID rmaId) {
        if (!auditoriaEventoRepository.existsByTipoEventoAndReferenciaId("RMA_ROTULO_REPOSICION_GENERADO", rmaId)) {
            registrarAuditoria("RMA_ROTULO_REPOSICION_GENERADO", rmaId, "{\"rmaId\":\"" + rmaId + "\"}");
        }
    }

    private RMA crearCasoRma(PedidoItem pedidoItem, Usuario cliente, String motivo, String mensajeInicial, List<MultipartFile> archivos) {
        validarPedidoItemElegible(pedidoItem);
        if (rmaRepository.existsByPedidoItem_IdAndEstadoRmaIn(pedidoItem.getId(), ESTADOS_RMA_ABIERTOS)) {
            throw new OperacionEnConflictoException("Ya existe un caso de postventa abierto para este item.");
        }

        RMA rma = new RMA();
        rma.setPedidoItem(pedidoItem);
        rma.setMotivo(motivo);
        rma.setDescripcion(mensajeInicial);
        rma.setEstadoRma(RmaEstado.SOLICITADO);
        rma.setTieneSeguro(pedidoItem.isTieneSeguro());
        rma.setCreadoPor(cliente);

        RMA saved = rmaRepository.save(rma);
        guardarMensaje(saved, RmaAutorTipo.CLIENTE, cliente != null ? cliente.getNombre() : "Cliente", mensajeInicial, false, archivos);
        registrarAuditoria("RMA_SOLICITADO", saved.getId(), "{\"rmaId\":\"" + saved.getId() + "\"}");
        registrarAuditoria("NOTIFICACIONES_RMA_SOLICITADO_ADMIN", saved.getId(), "{\"rmaId\":\"" + saved.getId() + "\",\"origen\":\"ADMIN_MD\"}");
        notificationService.createNotificationsForRole(
                "ADMIN_MD",
                "RMA_SOLICITADO",
                "Nuevo caso de postventa",
                "Ingreso un nuevo caso de postventa para revisar. Caso " + saved.getId() + ".");
        publicarEventoRmaSolicitado(saved);
        return rmaRepository.findById(saved.getId()).orElseThrow();
    }

    private void registrarCierreInterno(RMA rma, ResolverRmaRequest request, List<MultipartFile> archivos) {
        guardarMensaje(
                rma,
                RmaAutorTipo.ADMIN_MD,
                "Admin MD",
                buildMensajeCierreInterno(rma, request),
                true,
                archivos != null ? archivos : List.of());
    }

    private PedidoItem getPedidoItem(UUID pedidoItemId) {
        return pedidoItemRepository.findById(pedidoItemId)
                .orElseThrow(() -> new ResourceNotFoundException("PedidoItem no encontrado: " + pedidoItemId));
    }

    private void validarPedidoItemElegible(PedidoItem pedidoItem) {
        EnvioEstado estadoEnvio = pedidoItem.getPedido().getEnvio() != null
                ? pedidoItem.getPedido().getEnvio().getEstadoEnvio()
                : null;
        if (!(EnvioEstado.DESPACHADO.equals(estadoEnvio) || EnvioEstado.ENTREGADO.equals(estadoEnvio))) {
            throw new OperacionEnConflictoException(
                    "Solo se puede solicitar RMA para pedidos con envio DESPACHADO o ENTREGADO");
        }
    }

    private RMA getRma(UUID id) {
        return rmaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RMA no encontrado: " + id));
    }

    private List<RMA> obtenerRmasVisibles(String email, Collection<String> roles, UUID pedidoId) {
        if (esAdmin(roles)) {
            return pedidoId != null
                    ? rmaRepository.findByPedidoItem_Pedido_IdOrderByCreatedAtDesc(pedidoId)
                    : rmaRepository.findAllByOrderByCreatedAtDesc();
        }
        return pedidoId != null
                ? rmaRepository.findByCreadoPor_EmailIgnoreCaseAndPedidoItem_Pedido_IdOrderByCreatedAtDesc(email, pedidoId)
                : rmaRepository.findByCreadoPor_EmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    private boolean esAdmin(Collection<String> roles) {
        return roles != null && roles.stream().anyMatch("ADMIN_MD"::equals);
    }

    private void validarTransicion(RmaEstado actual, RmaEstado esperadoActual, RmaEstado siguiente) {
        if (!esperadoActual.equals(actual)) {
            throw new OperacionEnConflictoException(
                    "Transicion RMA invalida. Estado actual: " + actual + ", esperado: " + esperadoActual
                            + ", destino: " + siguiente);
        }
    }

    private void registrarAuditoria(String tipoEvento, UUID referenciaId, String payloadJson) {
        AuditoriaEvento evento = new AuditoriaEvento();
        evento.setTipoEvento(tipoEvento);
        evento.setReferenciaId(referenciaId);
        evento.setPayloadJson(payloadJson);
        auditoriaEventoRepository.save(evento);
    }

    private void guardarMensaje(RMA rma, RmaAutorTipo autorTipo, String autorNombre, String mensaje, boolean interno, List<MultipartFile> archivos) {
        RmaMensaje entity = new RmaMensaje();
        entity.setRma(rma);
        entity.setAutorTipo(autorTipo);
        entity.setAutorNombre(autorNombre);
        entity.setMensaje(mensaje);
        entity.setInterno(interno);
        RmaMensaje savedMessage = rmaMensajeRepository.save(entity);
        guardarAdjuntos(rma, savedMessage, archivos);
    }

    private void guardarAdjuntos(RMA rma, RmaMensaje mensaje, List<MultipartFile> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            return;
        }

        for (MultipartFile archivo : archivos) {
            if (archivo == null || archivo.isEmpty()) {
                continue;
            }

            validarAdjunto(archivo);
            try {
                RmaAdjunto entity = new RmaAdjunto();
                entity.setRma(rma);
                entity.setRmaMensaje(mensaje);
                entity.setNombreArchivo(resolveNombreArchivo(archivo));
                entity.setContentType(normalizeContentType(archivo.getContentType()));
                entity.setTamanoBytes(archivo.getSize());
                entity.setArchivo(archivo.getBytes());
                rmaAdjuntoRepository.save(entity);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer el adjunto del caso de postventa", e);
            }
        }
    }

    private void validarAdjunto(MultipartFile archivo) {
        if (archivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Cada imagen del caso puede pesar hasta 5 MB.");
        }
        String contentType = normalizeContentType(archivo.getContentType());
        if (!CONTENT_TYPES_IMAGEN_PERMITIDOS.contains(contentType)) {
            throw new BadRequestException("Solo se permiten imagenes PNG, JPG, WEBP o GIF en postventa.");
        }
    }

    private String resolveNombreArchivo(MultipartFile archivo) {
        String originalFilename = archivo.getOriginalFilename();
        return originalFilename != null && !originalFilename.isBlank() ? originalFilename : "adjunto-rma";
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase();
    }

    private PublicRmaResponse toPublicResponse(RMA rma) {
        HitosRma hitos = buildHitos(rma.getId());
        String estadoReposicion = buildEstadoReposicion(rma, hitos);
        return PublicRmaResponse.builder()
                .id(rma.getId())
                .pedidoId(rma.getPedidoItem().getPedido().getId())
                .pedidoItemId(rma.getPedidoItem().getId())
                .productoNombre(resolverNombreProducto(rma.getPedidoItem()))
                .motivo(rma.getMotivo())
                .estadoRma(rma.getEstadoRma().name())
                .estadoCliente(mapEstadoCliente(rma.getEstadoRma()))
                .resolucionFinal(rma.getResolucionFinal())
                .montoCompensacion(rma.getMontoCompensacion())
                .referenciaCompensacion(rma.getReferenciaCompensacion())
                .trackingLogistica(rma.getPedidoItem().getPedido().getEnvio() != null
                        ? rma.getPedidoItem().getPedido().getEnvio().getTracking()
                        : null)
                .rotuloDevolucionDescargado(rotuloDevolucionDescargado(rma))
                .rotuloReposicionGenerado(hitos.rotuloReposicionGenerado())
                .permiteDescargarRotuloDevolucion(permiteDescargarRotuloDevolucion(rma))
                .fechaAprobacion(hitos.fechaAprobacion())
                .fechaEnvioSolicitado(hitos.fechaEnvioSolicitado())
                .fechaRotuloDevolucionDescargado(hitos.fechaRotuloDevolucionDescargado())
                .fechaRecibidoDeposito(hitos.fechaRecibidoDeposito())
                .fechaCierre(hitos.fechaCierre())
                .fechaRotuloReposicionGenerado(hitos.fechaRotuloReposicionGenerado())
                .fechaReposicionSolicitadaProveedor(hitos.fechaReposicionSolicitadaProveedor())
                .fechaReposicionRecibidaMd(hitos.fechaReposicionRecibidaMd())
                .fechaReposicionReenviadaCliente(hitos.fechaReposicionReenviadaCliente())
                .fechaReposicionEntregadaCliente(hitos.fechaReposicionEntregadaCliente())
                .estadoReposicion(estadoReposicion)
                .resumenReposicion(buildResumenReposicion(rma, hitos, estadoReposicion))
                .reposicionTracking(hitos.reposicionTracking())
                .reposicionTransportistaNombre(hitos.reposicionTransportistaNombre())
                .instruccionesSiguientePaso(buildInstruccionesSiguientePaso(rma, hitos))
                .reposicionBitacora(buildReposicionBitacora(rma))
                .mensajes(rmaMensajeRepository.findByRma_IdOrderByCreatedAtAsc(rma.getId()).stream()
                        .filter(mensaje -> !mensaje.isInterno())
                        .map(this::toMensajeResponse)
                        .toList())
                .createdAt(rma.getCreatedAt())
                .updatedAt(rma.getUpdatedAt())
                .build();
    }

    private RmaResponse toResponse(RMA rma) {
        Pedido pedido = rma.getPedidoItem().getPedido();
        HitosRma hitos = buildHitos(rma.getId());
        boolean reposicionPath = esCircuitoReposicion(rma);
        String estadoReposicion = buildEstadoReposicion(rma, hitos);
        return RmaResponse.builder()
                .id(rma.getId())
                .pedidoItemId(rma.getPedidoItem().getId())
                .motivo(rma.getMotivo())
                .descripcion(rma.getDescripcion())
                .estadoRma(rma.getEstadoRma().name())
                .resolucionFinal(rma.getResolucionFinal())
                .observacionesInspeccion(rma.getObservacionesInspeccion())
                .responsableProbable(rma.getResponsableProbable())
                .montoCompensacion(rma.getMontoCompensacion())
                .referenciaCompensacion(rma.getReferenciaCompensacion())
                .pedidoId(pedido.getId())
                .trackingToken(pedido.getTrackingToken())
                .estadoPedido(pedido.getEstadoPedido().name())
                .estadoEnvio(pedido.getEnvio() != null ? pedido.getEnvio().getEstadoEnvio().name() : null)
                .trackingLogistica(pedido.getEnvio() != null ? pedido.getEnvio().getTracking() : null)
                .transportistaNombre(pedido.getEnvio() != null && pedido.getEnvio().getTransportista() != null
                        ? pedido.getEnvio().getTransportista().getNombre()
                        : null)
                .clienteNombre(pedido.getCliente() != null ? pedido.getCliente().getNombre() : null)
                .clienteEmail(pedido.getCliente() != null ? pedido.getCliente().getEmail() : null)
                .revendedorNombre(pedido.getRevendedor() != null ? pedido.getRevendedor().getNombre() : null)
                .storefrontNombre(pedido.getRevendedor() != null && pedido.getRevendedor().getStorefront() != null
                        ? pedido.getRevendedor().getStorefront().getNombreComercial()
                        : null)
                .storefrontDominio(pedido.getRevendedor() != null ? pedido.getRevendedor().getDominio() : null)
                .proveedorNombre(rma.getPedidoItem().getProveedor() != null ? rma.getPedidoItem().getProveedor().getNombre() : null)
                .proveedorEmail(rma.getPedidoItem().getProveedor() != null && rma.getPedidoItem().getProveedor().getUsuario() != null
                        ? rma.getPedidoItem().getProveedor().getUsuario().getEmail()
                        : null)
                .productoNombre(resolverNombreProducto(rma.getPedidoItem()))
                .cantidad(rma.getPedidoItem().getCantidad())
                .precioItemFinal(nvl(rma.getPedidoItem().getPrecioItemFinal()))
                .precioCosto(nvl(rma.getPedidoItem().getPrecioCosto()))
                .tieneSeguro(rma.getPedidoItem().isTieneSeguro())
                .montoSeguro(nvl(rma.getPedidoItem().getMontoSeguro()))
                .totalEnvioPedido(nvl(pedido.getTotalEnvio()))
                .direccionEnvio(pedido.getEnvio() != null ? pedido.getEnvio().getDireccionEnvio() : null)
                .rotuloDevolucionDescargado(rotuloDevolucionDescargado(rma))
                .rotuloReposicionGenerado(hitos.rotuloReposicionGenerado())
                .permiteSolicitarEnvio(RmaEstado.APROBADO.equals(rma.getEstadoRma()))
                .permiteDescargarRotuloDevolucion(permiteDescargarRotuloDevolucion(rma))
                .permiteDescargarRotuloReposicion(permiteDescargarRotuloReposicion(rma))
                .fechaAprobacion(hitos.fechaAprobacion())
                .fechaEnvioSolicitado(hitos.fechaEnvioSolicitado())
                .fechaRotuloDevolucionDescargado(hitos.fechaRotuloDevolucionDescargado())
                .fechaRecibidoDeposito(hitos.fechaRecibidoDeposito())
                .fechaCierre(hitos.fechaCierre())
                .fechaRotuloReposicionGenerado(hitos.fechaRotuloReposicionGenerado())
                .fechaReposicionSolicitadaProveedor(hitos.fechaReposicionSolicitadaProveedor())
                .fechaReposicionRecibidaMd(hitos.fechaReposicionRecibidaMd())
                .fechaReposicionReenviadaCliente(hitos.fechaReposicionReenviadaCliente())
                .fechaReposicionEntregadaCliente(hitos.fechaReposicionEntregadaCliente())
                .estadoReposicion(estadoReposicion)
                .resumenReposicion(buildResumenReposicion(rma, hitos, estadoReposicion))
                .reposicionTracking(hitos.reposicionTracking())
                .reposicionTransportistaNombre(hitos.reposicionTransportistaNombre())
                .permiteSolicitarReposicionProveedor(reposicionPath && hitos.fechaReposicionSolicitadaProveedor() == null)
                .permiteRegistrarRecepcionReposicionMd(reposicionPath
                        && hitos.fechaReposicionSolicitadaProveedor() != null
                        && hitos.fechaReposicionRecibidaMd() == null)
                .permiteReenviarReposicionCliente(reposicionPath
                        && hitos.fechaReposicionRecibidaMd() != null
                        && hitos.fechaReposicionReenviadaCliente() == null)
                .permiteEntregarReposicionCliente(reposicionPath
                        && hitos.fechaReposicionReenviadaCliente() != null
                        && hitos.fechaReposicionEntregadaCliente() == null)
                .instruccionesSiguientePaso(buildInstruccionesSiguientePaso(rma, hitos))
                .reposicionBitacora(buildReposicionBitacora(rma))
                .mensajes(rmaMensajeRepository.findByRma_IdOrderByCreatedAtAsc(rma.getId()).stream()
                        .map(this::toMensajeResponse)
                        .toList())
                .createdAt(rma.getCreatedAt())
                .updatedAt(rma.getUpdatedAt())
                .build();
    }

    private RmaMensajeResponse toMensajeResponse(RmaMensaje mensaje) {
        return RmaMensajeResponse.builder()
                .id(mensaje.getId())
                .autorTipo(mensaje.getAutorTipo().name())
                .autorNombre(mensaje.getAutorNombre())
                .mensaje(mensaje.getMensaje())
                .interno(mensaje.isInterno())
                .adjuntos(rmaAdjuntoRepository.findByRmaMensaje_IdOrderByCreatedAtAsc(mensaje.getId()).stream()
                        .map(this::toAdjuntoResponse)
                        .toList())
                .createdAt(mensaje.getCreatedAt())
                .build();
    }

    private RmaAdjuntoResponse toAdjuntoResponse(RmaAdjunto adjunto) {
        return RmaAdjuntoResponse.builder()
                .id(adjunto.getId())
                .nombreArchivo(adjunto.getNombreArchivo())
                .contentType(adjunto.getContentType())
                .tamanoBytes(adjunto.getTamanoBytes())
                .dataUrl("data:" + adjunto.getContentType() + ";base64," + Base64.getEncoder().encodeToString(adjunto.getArchivo()))
                .createdAt(adjunto.getCreatedAt())
                .build();
    }

    private String resolverNombreProducto(PedidoItem pedidoItem) {
        if (pedidoItem.getProducto() != null && pedidoItem.getProducto().getProductoCanonico() != null) {
            return pedidoItem.getProducto().getProductoCanonico().getNombrePublico();
        }
        return pedidoItem.getProducto() != null ? pedidoItem.getProducto().getNombre() : "Producto";
    }

    private String mapEstadoCliente(RmaEstado estado) {
        return switch (estado) {
            case SOLICITADO -> "Recibido";
            case APROBADO, EN_TRANSITO_DEVOLUCION -> "Esperando tu envio";
            case RECIBIDO_EN_DEPOSITO -> "En revision";
            case REEMBOLSADO -> "Reembolsado";
            case RESUELTO -> "Resuelto";
            case RECHAZADO -> "Cerrado";
        };
    }

    private boolean permiteDescargarRotuloDevolucion(RMA rma) {
        return List.of(RmaEstado.EN_TRANSITO_DEVOLUCION, RmaEstado.RECIBIDO_EN_DEPOSITO, RmaEstado.RESUELTO)
                .contains(rma.getEstadoRma());
    }

    private boolean permiteDescargarRotuloReposicion(RMA rma) {
        return RmaEstado.RESUELTO.equals(rma.getEstadoRma())
                && !"RECHAZADO".equalsIgnoreCase(rma.getResolucionFinal());
    }

    private String buildInstruccionesSiguientePaso(RMA rma, HitosRma hitos) {
        return switch (rma.getEstadoRma()) {
            case SOLICITADO -> "Recibimos tu caso. Nuestro equipo ya esta revisando la informacion enviada.";
            case APROBADO -> "Caso aprobado. Desde admin ya puedes solicitar el envio de devolucion y compartir el rotulo.";
            case EN_TRANSITO_DEVOLUCION -> "El cliente ya puede descargar el rotulo de devolucion y despachar el producto.";
            case RECIBIDO_EN_DEPOSITO -> "El producto ya fue recibido en deposito y quedo listo para evaluacion.";
            case REEMBOLSADO -> "El reembolso ya fue registrado. El caso queda trazado en este mismo seguimiento.";
            case RESUELTO -> buildInstruccionesReposicion(hitos);
            case RECHAZADO -> "El caso fue cerrado sin reposicion. Mantener trazabilidad de la resolucion informada al cliente.";
        };
    }

    private String buildInstruccionesReposicion(HitosRma hitos) {
        if (hitos.fechaReposicionSolicitadaProveedor() == null) {
            return "El caso fue resuelto con reposicion. Siguiente paso: solicitar la reposicion al proveedor.";
        }
        if (hitos.fechaReposicionRecibidaMd() == null) {
            return "La reposicion ya fue solicitada al proveedor. Siguiente paso: registrar cuando el reemplazo ingrese a MD.";
        }
        if (hitos.fechaReposicionReenviadaCliente() == null) {
            return "El reemplazo ya ingreso a MD. Siguiente paso: registrar el reenvio al cliente con transportista y tracking.";
        }
        if (hitos.fechaReposicionEntregadaCliente() == null) {
            return "El reemplazo ya fue reenviado. Siguiente paso: confirmar la entrega final al cliente.";
        }
        return "La reposicion ya fue entregada al cliente y el circuito de reemplazo queda cerrado.";
    }

    private String buildEstadoReposicion(RMA rma, HitosRma hitos) {
        if (!esCircuitoReposicion(rma)) {
            return "NO_APLICA";
        }
        if (hitos.fechaReposicionEntregadaCliente() != null) {
            return "ENTREGADA_CLIENTE";
        }
        if (hitos.fechaReposicionReenviadaCliente() != null) {
            return "EN_CAMINO_AL_CLIENTE";
        }
        if (hitos.fechaReposicionRecibidaMd() != null) {
            return "LISTA_PARA_REENVIO";
        }
        if (hitos.fechaReposicionSolicitadaProveedor() != null) {
            return "ESPERANDO_INGRESO_MD";
        }
        return "PENDIENTE_SOLICITUD_PROVEEDOR";
    }

    private String buildResumenReposicion(RMA rma, HitosRma hitos, String estadoReposicion) {
        return switch (estadoReposicion) {
            case "PENDIENTE_SOLICITUD_PROVEEDOR" ->
                    "El caso ya quedo resuelto con reposicion, pero todavia falta pedir formalmente el reemplazo al proveedor.";
            case "ESPERANDO_INGRESO_MD" ->
                    "La reposicion ya fue solicitada al proveedor y estamos esperando el ingreso fisico del reemplazo en MD.";
            case "LISTA_PARA_REENVIO" ->
                    "MD ya recibio el producto de reemplazo. Falta registrar el reenvio al cliente.";
            case "EN_CAMINO_AL_CLIENTE" ->
                    "El reemplazo ya fue reenviado al cliente"
                            + (StringUtils.hasText(hitos.reposicionTracking()) ? " con tracking " + hitos.reposicionTracking() : "")
                            + (StringUtils.hasText(hitos.reposicionTransportistaNombre()) ? " por " + hitos.reposicionTransportistaNombre() : "")
                            + ".";
            case "ENTREGADA_CLIENTE" ->
                    "La reposicion ya fue entregada al cliente y el caso queda completamente cerrado.";
            default -> {
                if ("REEMBOLSADO".equalsIgnoreCase(rma.getResolucionFinal()) || RmaEstado.REEMBOLSADO.equals(rma.getEstadoRma())) {
                    yield "Este caso cerro con devolucion de dinero, sin circuito de reposicion.";
                }
                if ("RECHAZADO".equalsIgnoreCase(rma.getResolucionFinal()) || RmaEstado.RECHAZADO.equals(rma.getEstadoRma())) {
                    yield "Este caso fue cerrado sin reposicion.";
                }
                yield "Este caso no requiere circuito de reposicion.";
            }
        };
    }

    private boolean rotuloDevolucionDescargado(RMA rma) {
        return auditoriaEventoRepository.existsByTipoEventoAndReferenciaId("RMA_ROTULO_DEVOLUCION_DESCARGADO", rma.getId());
    }

    private List<ReposicionBitacoraPasoResponse> buildReposicionBitacora(RMA rma) {
        if (!esCircuitoReposicion(rma)) {
            return List.of();
        }

        List<ReposicionBitacoraPasoResponse> pasos = new ArrayList<>();
        pasos.add(buildReposicionBitacoraPaso(
                "solicitud-proveedor",
                "Reposicion solicitada al proveedor",
                "RMA_REPOSICION_SOLICITADA_PROVEEDOR",
                rma.getId()));
        pasos.add(buildReposicionBitacoraPaso(
                "recepcion-md",
                "Reposicion recibida en MD",
                "RMA_REPOSICION_RECIBIDA_MD",
                rma.getId()));
        pasos.add(buildReposicionBitacoraPaso(
                "reenvio-cliente",
                "Reposicion reenviada al cliente",
                "RMA_REPOSICION_REENVIADA_CLIENTE",
                rma.getId()));
        pasos.add(buildReposicionBitacoraPaso(
                "entrega-cliente",
                "Reposicion entregada al cliente",
                "RMA_REPOSICION_ENTREGADA_CLIENTE",
                rma.getId()));

        return pasos.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private ReposicionBitacoraPasoResponse buildReposicionBitacoraPaso(String id, String titulo, String tipoEvento, UUID rmaId) {
        Optional<AuditoriaEvento> eventoOpt = auditoriaEventoRepository.findTopByTipoEventoAndReferenciaIdOrderByFechaEventoAsc(tipoEvento, rmaId);
        if (eventoOpt.isEmpty()) {
            return null;
        }
        AuditoriaEvento evento = eventoOpt.get();
        ReposicionPayload payload = parsePayload(evento.getPayloadJson());
        return ReposicionBitacoraPasoResponse.builder()
                .id(id)
                .titulo(titulo)
                .fecha(evento.getFechaEvento())
                .observacion(payload.observacion())
                .tracking(payload.tracking())
                .transportistaNombre(payload.transportistaNombre())
                .build();
    }

    private HitosRma buildHitos(UUID rmaId) {
        ReposicionPayload reenvioPayload = parsePayload(
                auditoriaEventoRepository.findTopByTipoEventoAndReferenciaIdOrderByFechaEventoDesc("RMA_REPOSICION_REENVIADA_CLIENTE", rmaId)
                        .map(AuditoriaEvento::getPayloadJson)
                        .orElse(null));
        return new HitosRma(
                fechaEvento("RMA_APROBADO", rmaId),
                fechaEvento("RMA_ENVIO_SOLICITADO", rmaId),
                fechaEvento("RMA_ROTULO_DEVOLUCION_DESCARGADO", rmaId),
                fechaEvento("RMA_RECIBIDO_EN_DEPOSITO", rmaId),
                fechaPrimerCierre(rmaId),
                fechaEvento("RMA_ROTULO_REPOSICION_GENERADO", rmaId),
                auditoriaEventoRepository.existsByTipoEventoAndReferenciaId("RMA_ROTULO_REPOSICION_GENERADO", rmaId),
                fechaEvento("RMA_REPOSICION_SOLICITADA_PROVEEDOR", rmaId),
                fechaEvento("RMA_REPOSICION_RECIBIDA_MD", rmaId),
                fechaEvento("RMA_REPOSICION_REENVIADA_CLIENTE", rmaId),
                fechaEvento("RMA_REPOSICION_ENTREGADA_CLIENTE", rmaId),
                reenvioPayload.tracking(),
                reenvioPayload.transportistaNombre());
    }

    private LocalDateTime fechaEvento(String tipoEvento, UUID rmaId) {
        return auditoriaEventoRepository.findTopByTipoEventoAndReferenciaIdOrderByFechaEventoAsc(tipoEvento, rmaId)
                .map(AuditoriaEvento::getFechaEvento)
                .orElse(null);
    }

    private LocalDateTime fechaPrimerCierre(UUID rmaId) {
        return List.of("RMA_REEMBOLSADO", "RMA_RESUELTO", "RMA_RECHAZADO").stream()
                .map(tipoEvento -> fechaEvento(tipoEvento, rmaId))
                .filter(java.util.Objects::nonNull)
                .sorted()
                .findFirst()
                .orElse(null);
    }

    private String buildMensajeCierreInterno(RMA rma, ResolverRmaRequest request) {
        StringBuilder mensaje = new StringBuilder("Cierre del caso asentado desde admin.");
        mensaje.append("\nResolucion: ").append(request.getResolucionFinal());
        if (request.getObservacionesInspeccion() != null && !request.getObservacionesInspeccion().isBlank()) {
            mensaje.append("\nObservaciones: ").append(request.getObservacionesInspeccion().trim());
        }
        if (request.getResponsableProbable() != null && !request.getResponsableProbable().isBlank()) {
            mensaje.append("\nResponsable probable: ").append(request.getResponsableProbable().trim());
        }
        if (request.getMontoCompensacion() != null) {
            mensaje.append("\nCompensacion: ").append(request.getMontoCompensacion());
        }
        if (request.getReferenciaCompensacion() != null && !request.getReferenciaCompensacion().isBlank()) {
            mensaje.append("\nReferencia / comprobante: ").append(request.getReferenciaCompensacion().trim());
        }
        mensaje.append("\nEstado backend resultante: ").append(rma.getEstadoRma().name());
        return mensaje.toString();
    }

    private void validarCircuitoReposicion(RMA rma) {
        if (!esCircuitoReposicion(rma)) {
            throw new OperacionEnConflictoException("La reposicion manual solo aplica a casos resueltos con reemplazo.");
        }
    }

    private boolean esCircuitoReposicion(RMA rma) {
        return RmaEstado.RESUELTO.equals(rma.getEstadoRma())
                && !"REEMBOLSADO".equalsIgnoreCase(rma.getResolucionFinal())
                && !"RECHAZADO".equalsIgnoreCase(rma.getResolucionFinal());
    }

    private void requireEvento(String tipoEvento, UUID rmaId, String mensaje) {
        if (!auditoriaEventoRepository.existsByTipoEventoAndReferenciaId(tipoEvento, rmaId)) {
            throw new OperacionEnConflictoException(mensaje);
        }
    }

    private Map<String, Object> buildPayloadReposicion(ActualizarReposicionManualRequest request, UUID transportistaId, String transportistaNombre) {
        return buildPayloadReposicion(request, transportistaId, transportistaNombre, request != null ? request.getTracking() : null);
    }

    private Map<String, Object> buildPayloadReposicion(ActualizarReposicionManualRequest request, UUID transportistaId, String transportistaNombre, String tracking) {
        Map<String, Object> payload = new HashMap<>();
        if (request != null && StringUtils.hasText(request.getObservacion())) {
            payload.put("observacion", request.getObservacion().trim());
        }
        if (transportistaId != null) {
            payload.put("transportistaId", transportistaId.toString());
        }
        if (StringUtils.hasText(transportistaNombre)) {
            payload.put("transportistaNombre", transportistaNombre);
        }
        if (StringUtils.hasText(tracking)) {
            payload.put("tracking", tracking.trim());
        }
        return payload;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private ReposicionPayload parsePayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return new ReposicionPayload(null, null, null, null);
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
            });
            String transportistaId = payload.get("transportistaId") instanceof String value ? value : null;
            return new ReposicionPayload(
                    transportistaId != null ? UUID.fromString(transportistaId) : null,
                    payload.get("transportistaNombre") instanceof String value ? value : null,
                    payload.get("tracking") instanceof String value ? value : null,
                    payload.get("observacion") instanceof String value ? value : null);
        } catch (Exception ex) {
            return new ReposicionPayload(null, null, null, null);
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void publicarEventoRmaSolicitado(RMA rma) {
        Pedido pedido = rma.getPedidoItem().getPedido();
        if (pedido == null || pedido.getRevendedor() == null || pedido.getTrackingToken() == null || pedido.getCliente() == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new EventoRmaSolicitado(
                rma.getId(),
                pedido.getId(),
                pedido.getRevendedor().getId(),
                pedido.getCliente().getEmail(),
                pedido.getCliente().getNombre(),
                pedido.getTrackingToken().toString(),
                rma.getEstadoRma().name(),
                resolverNombreProducto(rma.getPedidoItem()),
                rma.getMotivo(),
                rma.getCreatedAt()));
    }

    private void publicarEventoRmaEnvioSolicitado(RMA rma) {
        Pedido pedido = rma.getPedidoItem().getPedido();
        if (pedido == null || pedido.getRevendedor() == null || pedido.getTrackingToken() == null || pedido.getCliente() == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new EventoRmaEnvioSolicitado(
                rma.getId(),
                pedido.getId(),
                pedido.getRevendedor().getId(),
                pedido.getCliente().getEmail(),
                pedido.getCliente().getNombre(),
                pedido.getTrackingToken().toString(),
                rma.getEstadoRma().name(),
                resolverNombreProducto(rma.getPedidoItem()),
                rma.getUpdatedAt() != null ? rma.getUpdatedAt() : rma.getCreatedAt()));
    }

    private void publicarEventoRmaCerrado(RMA rma) {
        Pedido pedido = rma.getPedidoItem().getPedido();
        if (pedido == null || pedido.getRevendedor() == null || pedido.getTrackingToken() == null || pedido.getCliente() == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new EventoRmaCerrado(
                rma.getId(),
                pedido.getId(),
                pedido.getRevendedor().getId(),
                pedido.getCliente().getEmail(),
                pedido.getCliente().getNombre(),
                pedido.getTrackingToken().toString(),
                rma.getEstadoRma().name(),
                resolverNombreProducto(rma.getPedidoItem()),
                rma.getResolucionFinal(),
                rma.getMontoCompensacion(),
                rma.getReferenciaCompensacion(),
                rma.getUpdatedAt() != null ? rma.getUpdatedAt() : rma.getCreatedAt()));
    }

    private void publicarEventoRmaReposicionReenviada(RMA rma, String trackingReposicion, String transportistaNombre) {
        Pedido pedido = rma.getPedidoItem().getPedido();
        if (pedido == null || pedido.getRevendedor() == null || pedido.getTrackingToken() == null || pedido.getCliente() == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new EventoRmaReposicionReenviada(
                rma.getId(),
                pedido.getId(),
                pedido.getRevendedor().getId(),
                pedido.getCliente().getEmail(),
                pedido.getCliente().getNombre(),
                pedido.getTrackingToken().toString(),
                rma.getEstadoRma().name(),
                resolverNombreProducto(rma.getPedidoItem()),
                trackingReposicion,
                transportistaNombre,
                rma.getUpdatedAt() != null ? rma.getUpdatedAt() : LocalDateTime.now()));
    }

    private void publicarEventoRmaReposicionEntregada(RMA rma, String trackingReposicion, String transportistaNombre) {
        Pedido pedido = rma.getPedidoItem().getPedido();
        if (pedido == null || pedido.getRevendedor() == null || pedido.getTrackingToken() == null || pedido.getCliente() == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new EventoRmaReposicionEntregada(
                rma.getId(),
                pedido.getId(),
                pedido.getRevendedor().getId(),
                pedido.getCliente().getEmail(),
                pedido.getCliente().getNombre(),
                pedido.getTrackingToken().toString(),
                rma.getEstadoRma().name(),
                resolverNombreProducto(rma.getPedidoItem()),
                trackingReposicion,
                transportistaNombre,
                rma.getUpdatedAt() != null ? rma.getUpdatedAt() : LocalDateTime.now()));
    }

    private record HitosRma(
            LocalDateTime fechaAprobacion,
            LocalDateTime fechaEnvioSolicitado,
            LocalDateTime fechaRotuloDevolucionDescargado,
            LocalDateTime fechaRecibidoDeposito,
            LocalDateTime fechaCierre,
            LocalDateTime fechaRotuloReposicionGenerado,
            boolean rotuloReposicionGenerado,
            LocalDateTime fechaReposicionSolicitadaProveedor,
            LocalDateTime fechaReposicionRecibidaMd,
            LocalDateTime fechaReposicionReenviadaCliente,
            LocalDateTime fechaReposicionEntregadaCliente,
            String reposicionTracking,
            String reposicionTransportistaNombre) {
    }

    private record ReposicionPayload(
            UUID transportistaId,
            String transportistaNombre,
            String tracking,
            String observacion) {
    }
}
