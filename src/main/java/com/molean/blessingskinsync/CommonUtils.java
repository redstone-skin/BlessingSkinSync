package com.molean.blessingskinsync;

import com.google.common.collect.Iterables;
import com.google.gson.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class CommonUtils {

    public static final Logger LOGGER = Logger.getLogger("BlessingSkinSync");

    public static MineSkin.Data.Texture getSkinTextureFromProfile(String textureUrl, SimpleSkin skin, String player) {
        MineSkin.Data.Texture texture = SkinCache.INSTANCE.get(skin.getTexture());
        if (texture != null) {
            return texture;
        } else {
            textureUrl = textureUrl.replace("textures/%texture%", "skin/" + player + ".png");
            String variant = "classic";
            if (!skin.getModel().equalsIgnoreCase("default")) {
                variant = "slim";
            }
            String request = "{"
                    + "\"variant\":\"" + variant + "\","
                    + "\"name\":\"" + player + "\","
                    + "\"visibility\":\"public\","
                    + "\"url\":\"" + textureUrl + "\""
                    + "}";
            int responseCode;
            try {
                URL url = new URL("https://api.mineskin.org/v2/queue");
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                request(http);
                try (OutputStream os = http.getOutputStream()) {
                    byte[] input = request.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                responseCode = http.getResponseCode();
                if (responseCode != 200) {
                    HttpURLConnection http2 = (HttpURLConnection) url.openConnection();
                    request(http2);
                    try (OutputStream os = http2.getOutputStream()) {
                        byte[] input = request.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                    if (http2.getResponseCode() != 200) {
                        LOGGER.warning(responseCode + ":" + new String(readInputStream(http2.getErrorStream())));
                        return null;
                    } else {
                        InputStream inputStream = http2.getInputStream();
                        byte[] bytes = readInputStream(inputStream);
                        inputStream.read(bytes);
                        String s = new String(bytes);
                        LOGGER.info(s);
                        JsonParser parser = new JsonParser();
                        JsonObject root = parser.parse(s).getAsJsonObject();
                        JsonObject textureDataJson = root.getAsJsonObject("skin")
                                .getAsJsonObject("texture")
                                .getAsJsonObject("data");
                        Gson gson = new Gson();
                        texture = gson.fromJson(textureDataJson, MineSkin.Data.Texture.class);
                        SkinCache.INSTANCE.set(skin.getTexture(), texture);
                        SkinCache.INSTANCE.persist();
                        return texture;
                    }
                } else {
                    InputStream inputStream = http.getInputStream();
                    byte[] bytes = readInputStream(inputStream);
                    inputStream.read(bytes);
                    String s = new String(bytes);
                    JsonParser parser = new JsonParser();
                    JsonObject root = parser.parse(s).getAsJsonObject();
                    JsonObject textureDataJson = root.getAsJsonObject("skin")
                            .getAsJsonObject("texture")
                            .getAsJsonObject("data");
                    Gson gson = new Gson();
                    texture = gson.fromJson(textureDataJson, MineSkin.Data.Texture.class);
                    SkinCache.INSTANCE.set(skin.getTexture(), texture);
                    SkinCache.INSTANCE.persist();
                    return texture;
                }
            } catch (Exception e) {
                LOGGER.warning(Arrays.toString(e.getStackTrace()));
                return null;
            }
        }
    }

    public static void request(HttpURLConnection http) throws ProtocolException {
        http.setRequestMethod("POST");
        http.setDoOutput(true);
        http.setDoInput(true);
        http.setRequestProperty("Content-Type", "application/json");
        http.setRequestProperty("Accept", "application/json");
        http.setRequestProperty("User-Agent", "Chrome/120.0.0.0");
        http.setRequestProperty("Authorization", "Bearer msk_cQXSLKi3_ASU7SQIAZjHCZy255sHEjKKPPD3H-4RPx1WO-03qlvJRb2Vzel8ze89QIXpONK71");
        http.setConnectTimeout(20000);
        http.setReadTimeout(20000);
    }

    public static MineSkin.Data.Texture getSkinTextureFromPlayer(String rawProfileUrl, String rawTextureUrl, String player) {
        SimpleSkin skin = getSkinProfileFromPlayer(rawProfileUrl, player);
        if (skin == null) {
            LOGGER.warning("从皮肤站读取玩家" + player + "的profile失败!");
            String profileUrl = rawProfileUrl.replace("%playername%", player);
            LOGGER.warning("请检查<" + profileUrl + ">是否能够正常访问!");
            return null;
        } else {
            MineSkin.Data.Texture texture = getSkinTextureFromProfile(rawTextureUrl, skin, player);
            if (texture == null) {
                LOGGER.warning("无法从MineSkinAPI获取皮肤, 请服务器检查与api.mineskin.org的连通性!");
            }
            return texture;
        }
    }

    public static byte[] readInputStream(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int len = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        while((len = inputStream.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }

        bos.close();
        return bos.toByteArray();
    }

    public static SimpleSkin getSkinProfileFromPlayer(String profileUrl, String name) {
        try {
            profileUrl = profileUrl.replace("%playername%", name);
            URL url = new URL(profileUrl);
            InputStream inputStream = url.openStream();
            byte[] bytes = readInputStream(inputStream);
            inputStream.read(bytes);
            JsonParser jsonParser = new JsonParser();
            JsonElement parse = jsonParser.parse(new String(bytes));
            JsonObject jsonObject = parse.getAsJsonObject();
            JsonObject skins = jsonObject.getAsJsonObject("skins");
            Set<Map.Entry<String, JsonElement>> entries = skins.entrySet();
            Map.Entry<String, JsonElement> first = Iterables.getFirst(entries, null);
            if (first == null) {
                return null;
            } else {
                JsonElement value = first.getValue();
                String key = first.getKey();
                return new SimpleSkin(value.getAsString(), key);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }
}
