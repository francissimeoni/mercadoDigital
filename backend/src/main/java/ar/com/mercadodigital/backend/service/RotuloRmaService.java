package ar.com.mercadodigital.backend.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import ar.com.mercadodigital.backend.entity.Pedido;
import ar.com.mercadodigital.backend.entity.RMA;
import ar.com.mercadodigital.backend.entity.RmaEstado;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.exception.RotuloEnvioException;
import ar.com.mercadodigital.backend.repository.RMARepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RotuloRmaService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RMARepository rmaRepository;

    @Transactional(readOnly = true)
    public byte[] generarRotuloDevolucionPdf(UUID rmaId) {
        RMA rma = getRma(rmaId);
        if (!ListasRmaConRotulo.permiteRotuloDevolucion(rma.getEstadoRma())) {
            throw new RotuloEnvioException("El rotulo de devolucion todavia no esta disponible para este caso.", null);
        }
        return generarPdf(rma, "Rotulo de devolucion", "DEVOLUCION");
    }

    @Transactional(readOnly = true)
    public byte[] generarRotuloReposicionPdf(UUID rmaId) {
        RMA rma = getRma(rmaId);
        if (!RmaEstado.RESUELTO.equals(rma.getEstadoRma()) || "RECHAZADO".equalsIgnoreCase(rma.getResolucionFinal())) {
            throw new RotuloEnvioException("La etiqueta de reposicion solo esta disponible para casos resueltos con reposicion.", null);
        }
        return generarPdf(rma, "Etiqueta de reposicion", "REPOSICION");
    }

    private byte[] generarPdf(RMA rma, String titulo, String tipoRotulo) {
        Pedido pedido = rma.getPedidoItem().getPedido();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font textoFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph(titulo, tituloFont));
            document.add(new Paragraph(" ", textoFont));
            document.add(new Paragraph("casoRmaId: " + rma.getId(), textoFont));
            document.add(new Paragraph("tipo: " + tipoRotulo, textoFont));
            document.add(new Paragraph("pedidoId: " + pedido.getId(), textoFont));
            document.add(new Paragraph("storefront: " + getStorefront(pedido), textoFont));
            document.add(new Paragraph("cliente: " + getCliente(pedido), textoFont));
            document.add(new Paragraph("direccion: " + getDireccion(pedido), textoFont));
            document.add(new Paragraph("producto: " + getProducto(rma), textoFont));
            document.add(new Paragraph("motivo: " + nvl(rma.getMotivo()), textoFont));
            document.add(new Paragraph("estadoRma: " + rma.getEstadoRma().name(), textoFont));
            document.add(new Paragraph("trackingPublico: " + (pedido.getTrackingToken() != null ? pedido.getTrackingToken() : "-"), textoFont));
            document.add(new Paragraph("fechaCaso: " + (rma.getCreatedAt() != null ? rma.getCreatedAt().format(FORMATO_FECHA) : "-"), textoFont));
            document.add(new Paragraph(" ", textoFont));
            document.add(new Paragraph("Referencia de impresion: " + titulo + " / " + rma.getId(), textoFont));

            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new RotuloEnvioException("No se pudo generar el PDF del rotulo de RMA", ex);
        } catch (Exception ex) {
            throw new RotuloEnvioException("Error inesperado al generar el PDF del rotulo de RMA", ex);
        }
    }

    private RMA getRma(UUID rmaId) {
        return rmaRepository.findById(rmaId)
                .orElseThrow(() -> new ResourceNotFoundException("RMA no encontrado: " + rmaId));
    }

    private String getStorefront(Pedido pedido) {
        if (pedido.getRevendedor() != null && pedido.getRevendedor().getStorefront() != null
                && pedido.getRevendedor().getStorefront().getNombreComercial() != null) {
            return pedido.getRevendedor().getStorefront().getNombreComercial();
        }
        return pedido.getRevendedor() != null ? nvl(pedido.getRevendedor().getNombre()) : "-";
    }

    private String getCliente(Pedido pedido) {
        if (pedido.getCliente() == null) {
            return pedido.getId().toString();
        }
        if (pedido.getCliente().getNombre() != null && !pedido.getCliente().getNombre().isBlank()) {
            return pedido.getCliente().getNombre();
        }
        return nvl(pedido.getCliente().getEmail());
    }

    private String getDireccion(Pedido pedido) {
        return pedido.getEnvio() != null ? nvl(pedido.getEnvio().getDireccionEnvio()) : "-";
    }

    private String getProducto(RMA rma) {
        if (rma.getPedidoItem().getProducto() != null && rma.getPedidoItem().getProducto().getProductoCanonico() != null) {
            return nvl(rma.getPedidoItem().getProducto().getProductoCanonico().getNombrePublico());
        }
        return rma.getPedidoItem().getProducto() != null ? nvl(rma.getPedidoItem().getProducto().getNombre()) : "-";
    }

    private String nvl(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }

    private static final class ListasRmaConRotulo {
        private ListasRmaConRotulo() {
        }

        private static boolean permiteRotuloDevolucion(RmaEstado estado) {
            return estado == RmaEstado.EN_TRANSITO_DEVOLUCION
                    || estado == RmaEstado.RECIBIDO_EN_DEPOSITO
                    || estado == RmaEstado.RESUELTO;
        }
    }
}
