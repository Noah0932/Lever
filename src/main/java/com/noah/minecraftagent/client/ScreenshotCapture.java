package com.noah.minecraftagent.client;

import com.noah.minecraftagent.common.cache.CacheManager;
import com.noah.minecraftagent.common.config.AgentConfigStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;

public final class ScreenshotCapture {
    public CapturedScreenshot capture() throws IOException {
        MinecraftClient client = MinecraftClient.getInstance();
        NativeImage nativeImage = ScreenshotRecorder.takeScreenshot(client.getFramebuffer());
        try {
            BufferedImage source = new BufferedImage(nativeImage.getWidth(), nativeImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < nativeImage.getHeight(); y++) {
                for (int x = 0; x < nativeImage.getWidth(); x++) {
                    source.setRGB(x, y, nativeImage.getColor(x, y));
                }
            }
            int maxWidth = AgentConfigStore.getInstance().config().screenshotMaxWidth;
            BufferedImage resized = resize(source, maxWidth);
            byte[] jpeg = encodeJpeg(resized, AgentConfigStore.getInstance().config().screenshotJpegQuality / 100F);
            String base64 = Base64.getEncoder().encodeToString(jpeg);
            return new CapturedScreenshot(base64, CacheManager.sha256(base64), resized.getWidth(), resized.getHeight());
        } finally {
            nativeImage.close();
        }
    }

    private BufferedImage resize(BufferedImage source, int maxWidth) {
        if (source.getWidth() <= maxWidth) {
            return source;
        }
        int width = maxWidth;
        int height = Math.max(1, source.getHeight() * width / source.getWidth());
        Image scaled = source.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();
        return resized;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(Math.max(0.1F, Math.min(1.0F, quality)));
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    public record CapturedScreenshot(String base64Jpeg, String hash, int width, int height) {
    }
}
