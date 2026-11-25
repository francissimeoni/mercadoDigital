package ar.com.mercadodigital.backend.service.postcompra;

import ar.com.mercadodigital.backend.service.postcompra.EventoComprobanteTransferenciaCargado;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioDespachado;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioEntregado;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioListoParaRetiro;
import ar.com.mercadodigital.backend.service.postcompra.EventoEnvioRetirado;
import ar.com.mercadodigital.backend.service.postcompra.EventoPagoConfirmado;
import ar.com.mercadodigital.backend.service.postcompra.EventoPagoInternoConfirmado;
import ar.com.mercadodigital.backend.service.postcompra.EventoPedidoCreado;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaCerrado;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaEnvioSolicitado;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaReposicionEntregada;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaReposicionReenviada;
import ar.com.mercadodigital.backend.service.postcompra.EventoRmaSolicitado;
import ar.com.mercadodigital.backend.service.postcompra.EventoTransferenciaSolicitada;
import ar.com.mercadodigital.backend.service.postcompra.NotificacionInternaEmailService;
import ar.com.mercadodigital.backend.service.postcompra.NotificacionPostCompraService;
import org.springframework.scheduling.annotation.Async;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificacionPostCompraListener {

    private final NotificacionPostCompraService notificacionPostCompraService;

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onPedidoCreado(EventoPedidoCreado evento) {
        this.notificacionPostCompraService.notificarPedidoCreado(evento);
        this.notificacionInternaEmailService.notificarPedidoCreado(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onPagoConfirmado(EventoPagoConfirmado evento) {
        this.notificacionPostCompraService.notificarPagoConfirmado(evento);
        this.notificacionInternaEmailService.notificarPagoConfirmado(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onPagoInternoConfirmado(EventoPagoInternoConfirmado evento) {
        this.notificacionInternaEmailService.notificarPagoInternoConfirmado(evento.pedidoId());
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onEnvioDespachado(EventoEnvioDespachado evento) {
        this.notificacionPostCompraService.notificarEnvioDespachado(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onEnvioEntregado(EventoEnvioEntregado evento) {
        this.notificacionPostCompraService.notificarEnvioEntregado(evento);
        this.notificacionInternaEmailService.notificarEntregaConfirmada(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onEnvioListoParaRetiro(EventoEnvioListoParaRetiro evento) {
        this.notificacionPostCompraService.notificarEnvioListoParaRetiro(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onEnvioRetirado(EventoEnvioRetirado evento) {
        this.notificacionPostCompraService.notificarEnvioRetirado(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onRmaSolicitado(EventoRmaSolicitado evento) {
        this.notificacionPostCompraService.notificarRmaSolicitado(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onRmaEnvioSolicitado(EventoRmaEnvioSolicitado evento) {
        this.notificacionPostCompraService.notificarRmaEnvioSolicitado(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onRmaCerrado(EventoRmaCerrado evento) {
        this.notificacionPostCompraService.notificarRmaCerrado(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onRmaReposicionReenviada(EventoRmaReposicionReenviada evento) {
        this.notificacionPostCompraService.notificarRmaReposicionReenviada(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onRmaReposicionEntregada(EventoRmaReposicionEntregada evento) {
        this.notificacionPostCompraService.notificarRmaReposicionEntregada(evento);
    }

    private final NotificacionInternaEmailService notificacionInternaEmailService;

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onTransferenciaSolicitada(EventoTransferenciaSolicitada evento) {
        this.notificacionPostCompraService.notificarTransferenciaSolicitada(evento);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Async(value="postCompraTaskExecutor")
    public void onComprobanteTransferenciaCargado(EventoComprobanteTransferenciaCargado evento) {
        this.notificacionInternaEmailService.notificarComprobanteTransferencia(evento);
    }
}
