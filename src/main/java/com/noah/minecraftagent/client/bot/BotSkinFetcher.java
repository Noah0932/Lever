package com.noah.minecraftagent.client.bot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.noah.minecraftagent.common.util.SecureLog;

public final class BotSkinFetcher {
    private static final Gson GSON = new Gson();

    private BotSkinFetcher() {
    }

    public static CompletableFuture<Identifier> fetch(String playerName) {
        return CompletableFuture
                .supplyAsync(() -> downloadAndDecode(playerName))
                .thenApplyAsync(nativeImage -> {
                    if (nativeImage == null) return null;
                    Identifier id = Identifier.of("minecraftagent", "bot_skin_" + System.nanoTime());
                    MinecraftClient.getInstance().getTextureManager()
                            .registerTexture(id, new NativeImageBackedTexture(nativeImage));
                    return id;
                }, MinecraftClient.getInstance()::execute);
    }

    private static NativeImage downloadAndDecode(String playerName) {
        try {
            String uuid = resolveUuid(playerName);
            if (uuid == null) return null;
            Map<String, Object> profile = fetchProfile(uuid);
            if (profile == null) return null;

            var properties = (java.util.List<Map<String, Object>>) profile.get("properties");
            if (properties == null) return null;

            for (var prop : properties) {
                if ("textures".equals(prop.get("name"))) {
                    String encoded = (String) prop.get("value");
                    String decoded = new String(Base64.getDecoder().decode(encoded));
                    Map<String, Object> textures = GSON.fromJson(decoded, new TypeToken<Map<String, Object>>() {
                    }.getType());
                    var textureMap = (Map<String, Object>) textures.get("textures");
                    if (textureMap == null) return null;
                    var skinMap = (Map<String, Object>) textureMap.get("SKIN");
                    if (skinMap == null) return null;
                    String url = (String) skinMap.get("url");
                    if (url == null) return null;
                    return downloadImage(url);
                }
            }
        } catch (Exception exception) {
            SecureLog.error("Failed to download and decode skin for player: " + playerName, exception);
        }
        return null;
    }

    private static NativeImage downloadImage(String url) throws Exception {
        URL imgUrl = URI.create(url).toURL();
        HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        try (InputStream in = conn.getInputStream()) {
            BufferedImage buffered = ImageIO.read(in);
            if (buffered == null) return null;
            NativeImage nativeImage = new NativeImage(buffered.getWidth(), buffered.getHeight(), true);
            for (int y = 0; y < buffered.getHeight(); y++) {
                for (int x = 0; x < buffered.getWidth(); x++) {
                    nativeImage.setColor(x, y, buffered.getRGB(x, y));
                }
            }
            return nativeImage;
        }
    }

    private static String resolveUuid(String playerName) throws Exception {
        URL url = URI.create("https://api.mojang.com/users/profiles/minecraft/" + playerName).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        if (conn.getResponseCode() != 200) return null;
        String json;
        try (InputStream in = conn.getInputStream()) {
            json = new String(in.readAllBytes());
        }
        Map<String, Object> data = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
        String id = (String) data.get("id");
        if (id == null) return null;
        return id.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
    }

    private static Map<String, Object> fetchProfile(String uuid) throws Exception {
        URL url = URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        if (conn.getResponseCode() != 200) return null;
        String json;
        try (InputStream in = conn.getInputStream()) {
            json = new String(in.readAllBytes());
        }
        return GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
    }
}
