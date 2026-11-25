package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;
import ar.com.mercadodigital.backend.exception.BadRequestException;

class CatalogoAssetTransactionTest {
    @TempDir Path dir;
    @AfterEach void cleanup() { if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization(); }
    MockMultipartFile png() { return new MockMultipartFile("imagenes", "foto.exe", "image/png", new byte[]{(byte)137,80,78,71,13,10,26,10,0}); }
    @Test void rollbackEliminaSoloLaImagenNueva() throws Exception {
        Path previo = Files.writeString(dir.resolve("previo.txt"), "conservar");
        TransactionSynchronizationManager.initSynchronization();
        var asset = new StorefrontAssetStorageService(dir.toString()).storeTransactional(png(), "http://local.invalid/api");
        Path nuevo = dir.resolve(asset.getFilename()); assertTrue(Files.exists(nuevo)); assertTrue(asset.getFilename().endsWith(".png"));
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertFalse(Files.exists(nuevo)); assertTrue(Files.exists(previo));
    }
    @Test void commitConservaArchivo() {
        TransactionSynchronizationManager.initSynchronization();
        var asset = new StorefrontAssetStorageService(dir.toString()).storeTransactional(png(), "http://local.invalid/api");
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        assertTrue(Files.exists(dir.resolve(asset.getFilename())));
    }
    @Test void contenidoFalsoNoCreaArchivos() throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        assertThrows(BadRequestException.class, () -> new StorefrontAssetStorageService(dir.toString()).storeTransactional(
                new MockMultipartFile("imagenes", "foto.png", "image/png", "no-es-imagen".getBytes()), "http://local.invalid"));
        try (var files = Files.list(dir)) { assertEquals(0, files.count()); }
    }
    @Test void exigeTransaccionParaPoderLimpiar() {
        assertThrows(IllegalStateException.class, () -> new StorefrontAssetStorageService(dir.toString()).storeTransactional(png(), "http://local.invalid"));
    }
}
