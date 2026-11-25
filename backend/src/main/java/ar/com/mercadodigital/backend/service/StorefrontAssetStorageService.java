package ar.com.mercadodigital.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ar.com.mercadodigital.backend.dto.response.AdminStorefrontAssetResponse;
import ar.com.mercadodigital.backend.exception.BadRequestException;

@Service
public class StorefrontAssetStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/svg+xml",
            "image/gif");

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final Path baseDir;

    public StorefrontAssetStorageService(
            @Value("${app.storefront-assets.base-dir:storage/storefront-assets}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    public AdminStorefrontAssetResponse store(MultipartFile archivo, String tipo, String baseUrl) {
        validateArchivo(archivo);
        String tipoNormalizado = normalizeTipo(tipo);
        String extension = resolveExtension(archivo);
        String filename = tipoNormalizado + "-" + UUID.randomUUID() + extension;
        Path destino = baseDir.resolve(filename);

        try {
            Files.createDirectories(baseDir);
            try (InputStream inputStream = archivo.getInputStream()) {
                Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo de storefront", e);
        }

        return AdminStorefrontAssetResponse.builder()
                .url(buildPublicUrl(baseUrl, filename))
                .filename(filename)
                .contentType(archivo.getContentType())
                .size(archivo.getSize())
                .build();
    }

    public Path getBaseDir() {
        return baseDir;
    }

    /** Avatar acotado, decodificado y recodificado; propiedad identificable y limpieza ante rollback. */
    public AdminStorefrontAssetResponse storeAdvisorTransactional(MultipartFile archivo, UUID tienda, String baseUrl) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())
            throw new IllegalStateException("La foto debe guardarse dentro de una transacción.");
        validateArchivo(archivo);
        if (!Set.of("image/png", "image/jpeg", "image/webp").contains(archivo.getContentType()))
            throw new BadRequestException("Usá una foto PNG, JPEG o WEBP de hasta 5 MB.");
        String filename = "asesor-" + tienda + "-" + UUID.randomUUID() + ".jpg";
        Path destino = baseDir.resolve(filename);
        try (var input = javax.imageio.ImageIO.createImageInputStream(archivo.getInputStream())) {
            var readers = javax.imageio.ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BadRequestException("No se pudo leer la foto.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 20_000_000)
                    throw new BadRequestException("La foto debe tener hasta 20 megapíxeles.");
                var source = reader.read(0);
                int crop = Math.min(width, height), size = Math.min(600, crop);
                var thumbnail = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics = thumbnail.createGraphics();
                try {
                    graphics.setColor(java.awt.Color.WHITE); graphics.fillRect(0, 0, size, size);
                    graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    int x = (width - crop) / 2, y = (height - crop) / 2;
                    graphics.drawImage(source, 0, 0, size, size, x, y, x + crop, y + crop, null);
                } finally { graphics.dispose(); }
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) try { Files.deleteIfExists(destino); }
                            catch (IOException ex) { org.slf4j.LoggerFactory.getLogger(StorefrontAssetStorageService.class).error("No se pudo limpiar avatar {}", filename, ex); }
                        }
                    });
                Files.createDirectories(baseDir);
                if (!javax.imageio.ImageIO.write(thumbnail, "jpeg", destino.toFile())) throw new IOException("JPEG no disponible");
            } finally { reader.dispose(); }
            return AdminStorefrontAssetResponse.builder().url(buildPublicUrl(baseUrl, filename)).filename(filename)
                    .contentType("image/jpeg").size(Files.size(destino)).build();
        } catch (IOException | IllegalArgumentException ex) { throw new BadRequestException("No se pudo procesar la foto. Elegí otra imagen."); }
    }

    /** Archivos nuevos del guardado unificado: se eliminan si la transacción revierte. */
    public AdminStorefrontAssetResponse storeTransactional(MultipartFile archivo, String baseUrl) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive())
            throw new IllegalStateException("La imagen debe guardarse dentro de una transacción.");
        validateArchivo(archivo);
        try {
            byte[] bytes = archivo.getBytes();
            String type = String.valueOf(archivo.getContentType()).toLowerCase(Locale.ROOT);
            boolean valid = switch (type) {
                case "image/png" -> bytes.length > 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
                case "image/jpeg" -> bytes.length > 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff;
                case "image/webp" -> bytes.length > 12 && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                        && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
                default -> false;
            };
            if (!valid) throw new BadRequestException("Usá una imagen PNG, JPEG o WEBP válida.");
        } catch (IOException ex) { throw new BadRequestException("No se pudo leer la imagen."); }
        // La extensión proviene del tipo validado, nunca del nombre elegido por el usuario.
        String extension = switch (archivo.getContentType().toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png"; case "image/jpeg" -> ".jpg"; default -> ".webp";
        };
        String filename = "producto-" + UUID.randomUUID() + extension;
        Path destino = baseDir.resolve(filename);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try { Files.deleteIfExists(destino); }
                            catch (IOException ex) { org.slf4j.LoggerFactory.getLogger(StorefrontAssetStorageService.class)
                                    .error("No se pudo limpiar la imagen revertida {}", filename, ex); }
                        }
                    }
                });
        try {
            Files.createDirectories(baseDir);
            try (InputStream stream = archivo.getInputStream()) { Files.copy(stream, destino); }
        } catch (IOException ex) { throw new IllegalStateException("No se pudo guardar la imagen", ex); }
        return AdminStorefrontAssetResponse.builder().url(buildPublicUrl(baseUrl, filename)).filename(filename)
                .contentType(archivo.getContentType()).size(archivo.getSize()).build();
    }

    private void validateArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("archivo es obligatorio");
        }

        if (archivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("El archivo supera el maximo permitido de 5 MB");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Formato de imagen no soportado");
        }
    }

    private String normalizeTipo(String tipo) {
        String value = tipo == null ? "" : tipo.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return "storefront";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        return normalized.isBlank() ? "storefront" : normalized;
    }

    private String resolveExtension(MultipartFile archivo) {
        String originalName = archivo.getOriginalFilename();
        if (originalName != null) {
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                String extension = originalName.substring(dotIndex).toLowerCase(Locale.ROOT);
                if (extension.matches("\\.[a-z0-9]{2,5}")) {
                    return extension;
                }
            }
        }

        return switch (String.valueOf(archivo.getContentType()).toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            case "image/gif" -> ".gif";
            default -> ".img";
        };
    }

    private String buildPublicUrl(String baseUrl, String filename) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBase + "/public/assets/storefront/" + filename;
    }
}
