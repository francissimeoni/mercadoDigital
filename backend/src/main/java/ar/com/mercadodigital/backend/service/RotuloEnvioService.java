package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.entity.RevendedorStorefront;
import ar.com.mercadodigital.backend.service.StorefrontAssetStorageService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import ar.com.mercadodigital.backend.entity.Envio;
import ar.com.mercadodigital.backend.exception.ResourceNotFoundException;
import ar.com.mercadodigital.backend.exception.RotuloEnvioException;
import ar.com.mercadodigital.backend.repository.EnvioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RotuloEnvioService {

    private final EnvioRepository envioRepository;

    @Transactional(readOnly = true)
    public byte[] generarRotuloPdf(UUID pedidoId) {
        Envio envio = envioRepository.findByPedido_Id(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Envio no encontrado para pedido: " + pedidoId));

        return generarPdf(envio);
    }

    @Transactional(readOnly = true)
    public byte[] generarRotuloPdfRevendedor(UUID revendedorId, UUID pedidoId) {
        Envio envio = envioRepository.findByPedido_IdAndPedido_Revendedor_Id(pedidoId, revendedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Envio no encontrado para pedido: " + pedidoId));

        return generarPdf(envio);
    }

    private byte[] generarPdf(Envio envio) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 24.0f, 24.0f, 24.0f, 24.0f);
            PdfWriter.getInstance(document, output);
            document.open();
            RevendedorStorefront storefront = this.getStorefront(envio);
            Color colorMarca = this.parseColor(storefront != null ? storefront.getColorPrimario() : null);
            String nombreTienda = this.getNombreTienda(envio, storefront);
            PdfPTable etiqueta = new PdfPTable(1);
            etiqueta.setWidthPercentage(49.0f);
            etiqueta.setHorizontalAlignment(0);
            etiqueta.setKeepTogether(true);
            PdfPCell contenido = new PdfPCell();
            contenido.setBorderColor(COLOR_BORDE);
            contenido.setBorderWidth(1.0f);
            contenido.setPadding(10.0f);
            this.agregarEncabezado(contenido, storefront, nombreTienda, colorMarca);
            this.agregarDestino(contenido, envio);
            this.agregarConfirmacionEntrega(contenido, envio, colorMarca);
            this.agregarPie(contenido, storefront, nombreTienda, this.getPedidoCorto(envio));
            etiqueta.addCell(contenido);
            document.add(etiqueta);
            document.close();
            return output.toByteArray();
        }
        catch (DocumentException ex) {
            throw new RotuloEnvioException("No se pudo generar el PDF del rotulo", ex);
        }
        catch (Exception ex) {
            throw new RotuloEnvioException("Error inesperado al generar el PDF del rotulo", ex);
        }
    }

    private String getTelefono(Envio envio) {
        if (envio.getPedido().getCliente() == null || envio.getPedido().getCliente().getTelefonoWhatsapp() == null) {
            return "";
        }
        return envio.getPedido().getCliente().getTelefonoWhatsapp().trim();
    }

    private String getDestinatario(Envio envio) {
        if (envio.getPedido().getCliente() == null) {
            return "Destinatario no informado";
        }
        if (envio.getPedido().getCliente().getNombre() != null && !envio.getPedido().getCliente().getNombre().isBlank()) {
            return envio.getPedido().getCliente().getNombre();
        }
        if (envio.getPedido().getCliente().getEmail() != null && !envio.getPedido().getCliente().getEmail().isBlank()) {
            return envio.getPedido().getCliente().getEmail();
        }
        return "Destinatario no informado";
    }

    private static final Color COLOR_TEXTO = new Color(15, 23, 42);
    private static final Color COLOR_SECUNDARIO = new Color(71, 85, 105);
    private static final Color COLOR_BORDE = new Color(203, 213, 225);
    private final StorefrontAssetStorageService storefrontAssetStorageService;

    private void agregarEncabezado(PdfPCell container, RevendedorStorefront storefront, String nombreTienda, Color colorMarca) throws Exception {
        PdfPTable header = new PdfPTable(new float[]{1.0f, 2.4f});
        header.setWidthPercentage(100.0f);
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(0);
        logoCell.setVerticalAlignment(5);
        logoCell.setPadding(0.0f);
        Image logo = this.cargarLogo(storefront);
        if (logo != null) {
            logoCell.addElement(logo);
        }
        header.addCell(logoCell);
        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(0);
        brandCell.setPadding(0.0f);
        Paragraph nombre = new Paragraph(nombreTienda, FontFactory.getFont("Helvetica-Bold", 13.0f, COLOR_TEXTO));
        nombre.setAlignment(2);
        brandCell.addElement(nombre);
        String contacto = this.getContactoTienda(storefront);
        if (!contacto.isBlank()) {
            Paragraph detalle = new Paragraph(contacto, FontFactory.getFont("Helvetica", 7.5f, COLOR_SECUNDARIO));
            detalle.setAlignment(2);
            brandCell.addElement(detalle);
        }
        header.addCell(brandCell);
        container.addElement(header);
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100.0f);
        line.setSpacingBefore(6.0f);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setFixedHeight(4.0f);
        lineCell.setBorder(0);
        lineCell.setBackgroundColor(colorMarca);
        line.addCell(lineCell);
        container.addElement(line);
    }

    private void agregarDestino(PdfPCell container, Envio envio) throws DocumentException {
        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(100.0f);
        box.setSpacingBefore(10.0f);
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(COLOR_BORDE);
        cell.setBorderWidth(1.0f);
        cell.setPadding(12.0f);
        cell.addElement(this.label("DESTINATARIO"));
        Paragraph destinatario = new Paragraph(this.getDestinatario(envio), FontFactory.getFont("Helvetica-Bold", 13.0f, COLOR_TEXTO));
        destinatario.setSpacingAfter(10.0f);
        cell.addElement(destinatario);
        cell.addElement(this.label("DIRECCION DE ENTREGA"));
        Paragraph direccion = new Paragraph(this.valor(envio.getDireccionEnvio()), FontFactory.getFont("Helvetica-Bold", 12.0f, COLOR_TEXTO));
        direccion.setLeading(15.0f);
        direccion.setSpacingAfter(10.0f);
        cell.addElement(direccion);
        String telefono = this.getTelefono(envio);
        if (!telefono.isBlank()) {
            cell.addElement(this.label("TELEFONO DE CONTACTO"));
            cell.addElement(new Paragraph(telefono, FontFactory.getFont("Helvetica", 10.0f, COLOR_TEXTO)));
        }
        box.addCell(cell);
        container.addElement(box);
    }

    private void agregarPie(PdfPCell container, RevendedorStorefront storefront, String nombreTienda, String pedidoCorto) throws DocumentException {
        Paragraph remitente = new Paragraph("PEDIDO #" + pedidoCorto + "  |  ENVIADO POR " + nombreTienda.toUpperCase(), FontFactory.getFont("Helvetica-Bold", 8.0f, COLOR_SECUNDARIO));
        remitente.setAlignment(1);
        remitente.setSpacingBefore(14.0f);
        container.addElement(remitente);
        String detalle = this.getDetalleRemitente(storefront);
        if (!detalle.isBlank()) {
            Paragraph contacto = new Paragraph(detalle, FontFactory.getFont("Helvetica", 7.5f, COLOR_SECUNDARIO));
            contacto.setAlignment(1);
            contacto.setSpacingBefore(3.0f);
            container.addElement(contacto);
        }
    }

    private void agregarConfirmacionEntrega(PdfPCell container, Envio envio, Color colorMarca) throws Exception {
        String url = this.getUrlConfirmacionEntrega(envio);
        if (url == null) {
            return;
        }
        PdfPTable bloque = new PdfPTable(new float[]{1.0f, 1.7f});
        bloque.setWidthPercentage(100.0f);
        bloque.setSpacingBefore(10.0f);
        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(0);
        qrCell.setPadding(3.0f);
        qrCell.setVerticalAlignment(5);
        Image qr = this.generarQr(url);
        qr.scaleToFit(78.0f, 78.0f);
        qr.setAlignment(1);
        qrCell.addElement(qr);
        bloque.addCell(qrCell);
        PdfPCell textoCell = new PdfPCell();
        textoCell.setBorder(0);
        textoCell.setPadding(5.0f);
        textoCell.setVerticalAlignment(5);
        Paragraph titulo = new Paragraph("CONFIRMACION DE RECEPCION", FontFactory.getFont("Helvetica-Bold", 8.0f, colorMarca));
        titulo.setSpacingAfter(4.0f);
        textoCell.addElement(titulo);
        Paragraph indicacion = new Paragraph("Luego de revisar el contenido, escanea este codigo para confirmar que recibiste el pedido correctamente.", FontFactory.getFont("Helvetica", 7.5f, COLOR_SECUNDARIO));
        indicacion.setLeading(10.0f);
        textoCell.addElement(indicacion);
        bloque.addCell(textoCell);
        container.addElement(bloque);
    }

    private Image generarQr(String value) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 300, 300);
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), 12);
        for (int y = 0; y < matrix.getHeight(); ++y) {
            for (int x = 0; x < matrix.getWidth(); ++x) {
                image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        try (ByteArrayOutputStream png = new ByteArrayOutputStream();){
            ImageIO.write((RenderedImage)image, "png", png);
            Image image2 = Image.getInstance(png.toByteArray());
            return image2;
        }
    }

    private String getUrlConfirmacionEntrega(Envio envio) {
        if (envio.getPedido() == null || envio.getPedido().getRevendedor() == null || envio.getPedido().getTrackingToken() == null || envio.getPedido().getRevendedor().getDominio() == null) {
            return null;
        }
        String host = envio.getPedido().getRevendedor().getDominio().trim().replaceFirst("^https?://", "").replaceFirst("/.*$", "");
        if (host.isBlank()) {
            return null;
        }
        return "https://" + host + "/pedido/" + envio.getPedido().getTrackingToken() + "?confirmarEntrega=1";
    }

    private Paragraph label(String text) {
        Paragraph label = new Paragraph(text, FontFactory.getFont("Helvetica-Bold", 7.5f, COLOR_SECUNDARIO));
        label.setSpacingAfter(3.0f);
        return label;
    }

    private Image cargarLogo(RevendedorStorefront storefront) {
        if (storefront == null || storefront.getLogoUrl() == null || storefront.getLogoUrl().isBlank()) {
            return null;
        }
        try {
            String path = storefront.getLogoUrl().replace('\\', '/');
            String filename = path.substring(path.lastIndexOf(47) + 1).split("\\?")[0];
            Path baseDir = this.storefrontAssetStorageService.getBaseDir().toAbsolutePath().normalize();
            Path logoPath = baseDir.resolve(filename).normalize();
            if (!logoPath.startsWith(baseDir) || !Files.isRegularFile(logoPath, new LinkOption[0])) {
                return null;
            }
            Image logo = Image.getInstance(Files.readAllBytes(logoPath));
            logo.scaleToFit(72.0f, 34.0f);
            logo.setAlignment(0);
            return logo;
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private RevendedorStorefront getStorefront(Envio envio) {
        return envio.getPedido().getRevendedor() != null ? envio.getPedido().getRevendedor().getStorefront() : null;
    }

    private String getNombreTienda(Envio envio, RevendedorStorefront storefront) {
        if (storefront != null && storefront.getNombreComercial() != null && !storefront.getNombreComercial().isBlank()) {
            return storefront.getNombreComercial();
        }
        if (envio.getPedido().getRevendedor() != null && envio.getPedido().getRevendedor().getNombre() != null && !envio.getPedido().getRevendedor().getNombre().isBlank()) {
            return envio.getPedido().getRevendedor().getNombre();
        }
        return "Tienda";
    }

    private String getContactoTienda(RevendedorStorefront storefront) {
        if (storefront == null) {
            return "";
        }
        return this.unir(storefront.getDominioPublico(), storefront.getTelefonoComercial());
    }

    private String getDetalleRemitente(RevendedorStorefront storefront) {
        if (storefront == null) {
            return "";
        }
        return this.unir(storefront.getDireccionComercial(), storefront.getEmailComercial(), storefront.getTelefonoComercial());
    }

    private String getPedidoCorto(Envio envio) {
        return envio.getPedido().getId().toString().substring(0, 8).toUpperCase();
    }

    private Color parseColor(String value) {
        if (value != null && value.trim().matches("#?[0-9a-fA-F]{6}")) {
            return Color.decode((String)(value.trim().startsWith("#") ? value.trim() : "#" + value.trim()));
        }
        return COLOR_TEXTO;
    }

    private String valor(String value) {
        return value != null && !value.isBlank() ? value.trim() : "Direccion no informada";
    }

    private String unir(String ... values) {
        return Arrays.stream(values).filter(value -> value != null && !value.isBlank()).map(String::trim).distinct().collect(Collectors.joining("  |  "));
    }
}
