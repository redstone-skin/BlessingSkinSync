package com.molean.blessingskinsync;

import com.google.common.collect.Iterables;
import com.google.gson.*;
import net.skinsrestorer.api.SkinVariant;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.SkinsRestorerAPIShared;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class CommonUtils {

    public static final Logger LOGGER = Logger.getLogger("BlessingSkinSync");

    public static MineSkin.Data.Texture getSkinTextureFromProfile(String textureUrl, SimpleSkin skin) {
        MineSkin.Data.Texture texture = SkinCache.INSTANCE.get(skin.getTexture());
        if (texture != null) {
            return texture;
        } else {
            try {
                textureUrl = textureUrl.replace("%texture%", skin.getTexture());
                SkinVariant variant = SkinVariant.CLASSIC;
                if (!skin.getModel().equalsIgnoreCase("default")) {
                    variant = SkinVariant.SLIM;
                }
                IProperty property = SkinsRestorerAPIShared.getApi().genSkinUrl(textureUrl, variant);
                if (property != null) {
                    texture = new MineSkin.Data.Texture();
                    texture.setValue(property.getValue());
                    texture.setSignature(property.getSignature());
                    SkinCache.INSTANCE.set(skin.getTexture(), texture);
                    SkinCache.INSTANCE.persist();
                    return texture;
                } else {
                    return null;
                }
            } catch (SkinRequestException e){
                LOGGER.warning(e.getMessage());
                return null;
            }
        }
    }

    public static MineSkin.Data.Texture getSkinTextureFromPlayer(String rawProfileUrl, String rawTextureUrl, String player) {
        SimpleSkin skin = getSkinProfileFromPlayer(rawProfileUrl, player);
        if (skin == null) {
            LOGGER.warning("从皮肤站读取玩家" + player + "的profile失败!");
            String profileUrl = rawProfileUrl.replace("%playername%", player);
            LOGGER.warning("请检查<" + profileUrl + ">是否能够正常访问!");
            return null;
        } else {
            MineSkin.Data.Texture texture = getSkinTextureFromProfile(rawTextureUrl, skin);
            if (texture == null) {
                LOGGER.warning("无法从MineSkinAPI获取皮肤, 请服务器检查与mineskin.org的连通性!");
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
