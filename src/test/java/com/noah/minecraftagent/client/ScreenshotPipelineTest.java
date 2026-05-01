package com.noah.minecraftagent.client;

import com.noah.minecraftagent.common.cache.CacheManager;
import net.minecraft.client.texture.NativeImage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD: Screenshot processing pipeline — CPU-intensive work off main thread.
 */
class ScreenshotPipelineTest {

    @Test
    void shouldProduceValidBase64Jpeg() throws IOException {
        BufferedImage img = createTestImage(64, 64);
        byte[] jpeg = encodeJpeg(img, 0.75f);
        String base64 = Base64.getEncoder().encodeToString(jpeg);
        assertNotNull(base64);
        assertTrue(base64.length() > 10);
        assertTrue(base64.matches("^[A-Za-z0-9+/=]+$"), "Must be valid Base64");
    }

    @Test
    void shouldResizeImageProportionally() {
        BufferedImage source = createTestImage(1024, 768);
        BufferedImage resized = resize(source, 512);
        assertEquals(512, resized.getWidth());
        assertEquals(384, resized.getHeight()); // 1024:768 = 512:384
    }

    @Test
    void shouldNotResizeSmallImage() {
        BufferedImage source = createTestImage(200, 100);
        BufferedImage resized = resize(source, 512);
        assertSame(source, resized, "Images smaller than maxWidth must not be resized");
    }

    @Test
    void shouldProduceConsistentHash() {
        String data1 = "test-screenshot-data-1";
        String hash1 = CacheManager.sha256(data1);
        String hash2 = CacheManager.sha256(data1);
        assertEquals(hash1, hash2, "SHA-256 must be deterministic");
        assertEquals(64, hash1.length(), "SHA-256 hex must be 64 chars");
    }

    @Test
    void shouldEncodeJpegAtVariousQualities() throws IOException {
        BufferedImage img = createTestImage(128, 128);

        byte[] highQ = encodeJpeg(img, 0.9f);
        byte[] lowQ = encodeJpeg(img, 0.1f);

        assertTrue(highQ.length > 0);
        assertTrue(lowQ.length > 0);
        assertTrue(highQ.length > lowQ.length,
                "Higher quality JPEG must produce larger output");
    }

    private BufferedImage createTestImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, (x * 3 + y * 5) % 0xFFFFFF);
            }
        }
        return img;
    }

    private BufferedImage resize(BufferedImage source, int maxWidth) {
        if (source.getWidth() <= maxWidth) return source;
        int width = maxWidth;
        int height = Math.max(1, source.getHeight() * width / source.getWidth());
        java.awt.Image scaled = source.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return resized;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName("jpg");
        var writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (var imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            var params = writer.getDefaultWriteParam();
            params.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(Math.max(0.1f, Math.min(1.0f, quality)));
            writer.write(null, new javax.imageio.IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }
}
