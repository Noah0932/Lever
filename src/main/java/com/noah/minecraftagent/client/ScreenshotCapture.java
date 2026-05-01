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
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Base64;
import java.util.Iterator;

public final class ScreenshotCapture {

    public NativeImage takeScreenshotOnMain() {
        return ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer());
    }

    public CapturedScreenshot processAsync(NativeImage nativeImage) {
        try {
            int w = nativeImage.getWidth();
            int h = nativeImage.getHeight();
            int[] pixels = convertAbgrToArgbBulk(nativeImage, w, h);

            BufferedImage source = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            source.setRGB(0, 0, w, h, pixels, 0, w);

            int maxWidth = AgentConfigStore.getInstance().config().screenshotMaxWidth;
            float quality = AgentConfigStore.getInstance().config().screenshotJpegQuality / 100F;
            BufferedImage resized = resize(source, maxWidth);
            byte[] jpeg = encodeJpeg(resized, quality);
            String base64 = Base64.getEncoder().encodeToString(jpeg);
            return new CapturedScreenshot(base64, CacheManager.sha256(base64), resized.getWidth(), resized.getHeight());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (nativeImage != null) {
                nativeImage.close();
            }
        }
    }

    static int[] convertAbgrToArgbBulk(NativeImage nativeImage, int width, int height) throws IOException {
        byte[] rawBytes = nativeImage.getBytes();
        IntBuffer intBuf = ByteBuffer.wrap(rawBytes).order(ByteOrder.nativeOrder()).asIntBuffer();
        int pixelCount = width * height;
        int[] pixels = new int[pixelCount];
        for (int i = 0; i < pixelCount; i++) {
            int abgr = intBuf.get(i);
            pixels[i] = (abgr & 0xFF00FF00) | ((abgr & 0x00FF0000) >>> 16) | ((abgr & 0x000000FF) << 16);
        }
        return pixels;
    }

    public CapturedScreenshot capture() throws IOException {
        NativeImage nativeImage = takeScreenshotOnMain();
        return processAsync(nativeImage);
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
