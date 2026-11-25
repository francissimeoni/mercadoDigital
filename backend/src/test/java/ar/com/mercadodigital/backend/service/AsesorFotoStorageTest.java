package ar.com.mercadodigital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ar.com.mercadodigital.backend.exception.BadRequestException;

class AsesorFotoStorageTest {
    @TempDir Path folder;
    @Test void recodificaCuadradaConPropiedadYLimpiaSiRevierte() throws Exception {
        var storage=new StorefrontAssetStorageService(folder.toString());var bytes=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1000,800,BufferedImage.TYPE_INT_RGB),"png",bytes);
        var file=new MockMultipartFile("foto","../../archivo.svg","image/png",bytes.toByteArray());
        UUID tienda=UUID.randomUUID();TransactionSynchronizationManager.initSynchronization();
        try {
            var saved=storage.storeAdvisorTransactional(file,tienda,"http://local.test.invalid");
            assertTrue(saved.getFilename().startsWith("asesor-"+tienda+"-"));assertTrue(saved.getFilename().endsWith(".jpg"));
            var image=ImageIO.read(folder.resolve(saved.getFilename()).toFile());assertEquals(600,image.getWidth());assertEquals(600,image.getHeight());
            for(var sync:TransactionSynchronizationManager.getSynchronizations())sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            assertFalse(Files.exists(folder.resolve(saved.getFilename())));
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void rechazaContenidoFalsoYFormatosActivosSinEscribir() throws Exception {
        var storage=new StorefrontAssetStorageService(folder.toString());TransactionSynchronizationManager.initSynchronization();
        try {
            assertThrows(BadRequestException.class,()->storage.storeAdvisorTransactional(new MockMultipartFile("foto","a.png","image/png","not an image".getBytes()),UUID.randomUUID(),"http://local.test.invalid"));
            assertThrows(BadRequestException.class,()->storage.storeAdvisorTransactional(new MockMultipartFile("foto","a.svg","image/svg+xml","<svg/>".getBytes()),UUID.randomUUID(),"http://local.test.invalid"));
            try(var files=Files.list(folder)){assertEquals(0,files.count());}
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
}
