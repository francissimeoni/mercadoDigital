package ar.com.mercadodigital.backend.controller;

import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import javax.imageio.ImageIO;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.com.mercadodigital.backend.dto.response.PublicProductoDetalleResponse;
import ar.com.mercadodigital.backend.dto.response.StorefrontResponse;
import ar.com.mercadodigital.backend.exception.BadRequestException;
import ar.com.mercadodigital.backend.service.PublicCatalogoService;
import ar.com.mercadodigital.backend.service.StorefrontService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/share")
public class PublicShareController {
    private static final int OG_IMAGE_WIDTH = 1200;
    private static final int OG_IMAGE_HEIGHT = 630;
    private static final int MAX_SOURCE_IMAGE_BYTES = 12 * 1024 * 1024;

    private final StorefrontService storefrontService;
    private final PublicCatalogoService publicCatalogoService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @GetMapping(value = "/producto/{productoCanonicoId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareProducto(
            @PathVariable UUID productoCanonicoId,
            @RequestParam @NotBlank String storefrontHost,
            @RequestParam(required = false) String asesorId,
            @RequestParam(required = false) String asesor,
            @RequestParam(required = false) String v) {
        StorefrontResponse storefront = storefrontService.resolverPorHost(storefrontHost);
        PublicProductoDetalleResponse producto = resolveProducto(storefront, productoCanonicoId);

        String productUrl = buildProductUrl(storefront.getDominio(), productoCanonicoId, asesorId, asesor);
        String title = nonBlank(producto.getNombrePublico(), "Producto");
        String description = buildDescription(producto);
        String imageUrl = buildOwnOgImageUrl(storefront.getDominio(), productoCanonicoId, storefrontHost, v);

        String html = "<!doctype html><html lang=\"es\"><head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + escapeHtml(title) + "</title>"
                + "<meta property=\"og:type\" content=\"product\">"
                + "<meta property=\"og:title\" content=\"" + escapeHtml(title) + "\">"
                + "<meta property=\"og:description\" content=\"" + escapeHtml(description) + "\">"
                + "<meta property=\"og:url\" content=\"" + escapeHtml(productUrl) + "\">"
                + (imageUrl.isBlank() ? "" : "<meta property=\"og:image\" content=\"" + escapeHtml(imageUrl) + "\">")
                + (imageUrl.isBlank() ? "" : "<meta property=\"og:image:secure_url\" content=\"" + escapeHtml(imageUrl) + "\">")
                + (imageUrl.isBlank() ? "" : "<meta property=\"og:image:type\" content=\"image/jpeg\">")
                + (imageUrl.isBlank() ? "" : "<meta property=\"og:image:width\" content=\"" + OG_IMAGE_WIDTH + "\">")
                + (imageUrl.isBlank() ? "" : "<meta property=\"og:image:height\" content=\"" + OG_IMAGE_HEIGHT + "\">")
                + "<meta name=\"twitter:card\" content=\"summary_large_image\">"
                + "<meta name=\"twitter:title\" content=\"" + escapeHtml(title) + "\">"
                + "<meta name=\"twitter:description\" content=\"" + escapeHtml(description) + "\">"
                + (imageUrl.isBlank() ? "" : "<meta name=\"twitter:image\" content=\"" + escapeHtml(imageUrl) + "\">")
                + "</head><body>"
                + "<p><a href=\"" + escapeHtml(productUrl) + "\">Ver detalles</a></p>"
                + "<script>setTimeout(function(){window.location.replace(" + toJsString(productUrl) + ");}, 350);</script>"
                + "</body></html>";

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @GetMapping(value = "/lista", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareLista(
            @RequestParam @NotBlank String storefrontHost,
            @RequestParam @NotBlank String lista,
            @RequestParam(required = false) String asesorId,
            @RequestParam(required = false) String asesor,
            @RequestParam(required = false) String v) {
        StorefrontResponse storefront = storefrontService.resolverPorHost(storefrontHost);
        List<UUID> productoIds = parseLista(lista);
        if (productoIds.isEmpty()) {
            throw new BadRequestException("lista invalida");
        }

        PublicProductoDetalleResponse first = resolveProducto(storefront, productoIds.get(0));
        String listUrl = buildListUrl(storefront.getDominio(), lista, asesorId, asesor);
        String title = "Seleccion de productos - " + nonBlank(storefront.getNombreComercial(), storefront.getDominio());
        String description = "Incluye " + productoIds.size() + " producto(s). " + (first.isModoCatalogo() ? "Consultá con la tienda por WhatsApp." : "Ver lista y comprar online.");
        String imageUrl = buildOwnOgImageUrl(storefront.getDominio(), productoIds.get(0), storefrontHost, v);

        String html = "<!doctype html><html lang=\"es\"><head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + escapeHtml(title) + "</title>"
                + "<meta property=\"og:type\" content=\"website\">"
                + "<meta property=\"og:title\" content=\"" + escapeHtml(title) + "\">"
                + "<meta property=\"og:description\" content=\"" + escapeHtml(description) + "\">"
                + "<meta property=\"og:url\" content=\"" + escapeHtml(listUrl) + "\">"
                + (imageUrl.isBlank() ? "" : "<meta property=\"og:image\" content=\"" + escapeHtml(imageUrl) + "\">")
                + (imageUrl.isBlank() ? "" : "<meta property=\"og:image:secure_url\" content=\"" + escapeHtml(imageUrl) + "\">")
                + (imageUrl.isBlank() ? "" : "<meta property=\"og:image:type\" content=\"image/jpeg\">")
                + (imageUrl.isBlank() ? "" : "<meta property=\"og:image:width\" content=\"" + OG_IMAGE_WIDTH + "\">")
                + (imageUrl.isBlank() ? "" : "<meta property=\"og:image:height\" content=\"" + OG_IMAGE_HEIGHT + "\">")
                + "<meta name=\"twitter:card\" content=\"summary_large_image\">"
                + "<meta name=\"twitter:title\" content=\"" + escapeHtml(title) + "\">"
                + "<meta name=\"twitter:description\" content=\"" + escapeHtml(description) + "\">"
                + (imageUrl.isBlank() ? "" : "<meta name=\"twitter:image\" content=\"" + escapeHtml(imageUrl) + "\">")
                + "</head><body>"
                + "<p><a href=\"" + escapeHtml(listUrl) + "\">Ver lista</a></p>"
                + "<script>setTimeout(function(){window.location.replace(" + toJsString(listUrl) + ");}, 350);</script>"
                + "</body></html>";

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(15)).cachePublic())
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @GetMapping(value = "/producto/{productoCanonicoId}/image")
    public ResponseEntity<byte[]> shareProductoImage(
            @PathVariable UUID productoCanonicoId,
            @RequestParam @NotBlank String storefrontHost) {
        StorefrontResponse storefront = storefrontService.resolverPorHost(storefrontHost);
        PublicProductoDetalleResponse producto = resolveProducto(storefront, productoCanonicoId);
        for (String rawImageUrl : imageCandidates(producto, storefront)) {
            byte[] jpegBody = downloadAndConvert(rawImageUrl);
            if (jpegBody != null && jpegBody.length > 0) {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePublic())
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(jpegBody);
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private String buildProductUrl(String storefrontHost, UUID productoCanonicoId, String asesorId, String asesor) {
        StringBuilder builder = new StringBuilder("https://")
                .append(storefrontHost)
                .append("/producto/")
                .append(productoCanonicoId);

        boolean hasQuery = false;
        if (nonBlank(asesorId, "").length() > 0) {
            builder.append(hasQuery ? '&' : '?')
                    .append("asesorId=")
                    .append(urlEncode(asesorId));
            hasQuery = true;
        }
        if (nonBlank(asesor, "").length() > 0) {
            builder.append(hasQuery ? '&' : '?')
                    .append("asesor=")
                    .append(urlEncode(asesor));
        }
        return builder.toString();
    }

    private PublicProductoDetalleResponse resolveProducto(StorefrontResponse storefront, UUID productoId) {
        if (storefront.getRevendedorId() != null) {
            return publicCatalogoService.getProductoDetalle(productoId, storefront.getRevendedorId());
        }
        if (storefront.getProveedorId() != null) {
            return publicCatalogoService.getProductoDetalleProveedor(productoId, storefront.getProveedorId());
        }
        throw new BadRequestException("storefront sin catalogo asociado");
    }

    private String buildListUrl(String storefrontHost, String lista, String asesorId, String asesor) {
        StringBuilder builder = new StringBuilder("https://")
                .append(storefrontHost)
                .append("/catalogo?lista=")
                .append(urlEncode(lista));

        if (nonBlank(asesorId, "").length() > 0) {
            builder.append("&asesorId=").append(urlEncode(asesorId));
        }
        if (nonBlank(asesor, "").length() > 0) {
            builder.append("&asesor=").append(urlEncode(asesor));
        }
        return builder.toString();
    }

    private List<UUID> parseLista(String lista) {
        List<UUID> ids = new ArrayList<>();
        for (String raw : lista.split(",")) {
            String trimmed = raw.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            try {
                ids.add(UUID.fromString(trimmed));
            } catch (IllegalArgumentException ignored) {
                // Ignorar tokens invalidos y validar al final.
            }
        }
        return ids;
    }

    private String buildDescription(PublicProductoDetalleResponse producto) {
        String precio = producto.getMejorPrecioFinalEstimado() != null ? "$ " + producto.getMejorPrecioFinalEstimado().toPlainString() : "Precio a consultar";
        if (producto.isModoCatalogo()) {
            java.text.NumberFormat ars = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-AR"));
            return "Precio en pesos argentinos: " + ars.format(producto.getMejorPrecioFinalEstimado())
                    + (StringUtils.hasText(producto.getCondicionesEntrega()) ? " | " + producto.getCondicionesEntrega() : "");
        }
        int demora = producto.getDemoraEstimadaDias() != null ? producto.getDemoraEstimadaDias() : 0;
        int desde = Math.max(demora - 2, 1);
        int hasta = demora + 2;
        return "Precio: " + precio + " | Entrega estimada: " + desde + " a " + hasta + " dias";
    }

    private String nonBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String toJsString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String buildOwnOgImageUrl(
            String storefrontHost,
            UUID productoCanonicoId,
            String storefrontHostParam,
            String version) {
        String url = "https://" + storefrontHost
                + "/api/public/share/producto/"
                + productoCanonicoId
                + "/image?storefrontHost="
                + urlEncode(storefrontHostParam);
        return StringUtils.hasText(version) ? url + "&v=" + urlEncode(version) : url;
    }

    private List<String> imageCandidates(
            PublicProductoDetalleResponse producto,
            StorefrontResponse storefront) {
        List<String> rawCandidates = new ArrayList<>();
        rawCandidates.add(producto.getImagenPrincipal());
        if (producto.getImagenes() != null) {
            rawCandidates.addAll(producto.getImagenes());
        }
        rawCandidates.add(storefront.getHeroImagenUrl());
        rawCandidates.add(storefront.getLogoUrl());
        return rawCandidates.stream()
                .filter(StringUtils::hasText)
                .map(url -> toAbsoluteUrl(url, storefront.getDominio()))
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private byte[] downloadAndConvert(String rawImageUrl) {
        try {
            URI uri = URI.create(rawImageUrl);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "Mozilla/5.0 (compatible; MercadoDigitalBot/1.0)")
                    .header("Accept", "image/jpeg,image/png,image/webp,image/gif,image/*;q=0.8,*/*;q=0.5")
                    .header("Referer", uri.getScheme() + "://" + uri.getHost() + "/")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300
                    || body == null || body.length == 0 || body.length > MAX_SOURCE_IMAGE_BYTES) {
                return null;
            }
            return toJpegOgImage(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    private byte[] toJpegOgImage(byte[] rawImage) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(rawImage));
            if (source == null) {
                return null;
            }

            BufferedImage canvas = new BufferedImage(OG_IMAGE_WIDTH, OG_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = canvas.createGraphics();
            try {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, OG_IMAGE_WIDTH, OG_IMAGE_HEIGHT);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                double ratio = Math.min((double) OG_IMAGE_WIDTH / source.getWidth(), (double) OG_IMAGE_HEIGHT / source.getHeight());
                int drawW = Math.max(1, (int) Math.round(source.getWidth() * ratio));
                int drawH = Math.max(1, (int) Math.round(source.getHeight() * ratio));
                int x = (OG_IMAGE_WIDTH - drawW) / 2;
                int y = (OG_IMAGE_HEIGHT - drawH) / 2;
                g.drawImage(source, x, y, drawW, drawH, null);
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(canvas, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String toAbsoluteUrl(String rawUrl, String storefrontHost) {
        String value = nonBlank(rawUrl, "");
        if (value.isBlank()) {
            return "";
        }
        String lower = value.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("//")) {
            return "https:" + value;
        }
        String normalizedHost = nonBlank(storefrontHost, "");
        if (normalizedHost.isBlank()) {
            return value;
        }
        return value.startsWith("/")
                ? "https://" + normalizedHost + value
                : "https://" + normalizedHost + "/" + value;
    }
}
