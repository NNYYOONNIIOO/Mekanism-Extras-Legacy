package com.jerry.mekextras.client.resource;

import com.google.gson.JsonObject;
import com.jerry.mekextras.common.config.ExtraConfig;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;

/**
 * A virtual, lowest-priority resource pack. The generated pack.mcmeta intentionally uses pack_format 1 so the pack
 * remains compatible with all 1.12.2 resource-manager variants. Existing assets always win, making tiers.json a safe
 * fallback layer.
 */
@SideOnly(Side.CLIENT)
public final class TierResourcePack implements IResourcePack {

    private final File packRoot;

    public TierResourcePack() {
        File configDirectory = ExtraConfig.getConfigDirectory();
        this.packRoot = configDirectory == null ? null : new File(configDirectory, "resourcepack");
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (location.getPath().equals("pack.mcmeta")) {
            String metadata = "{\"pack\":{\"pack_format\":1,\"description\":\"Mekanism Extras Legacy configured tiers\"}}";
            return new ByteArrayInputStream(metadata.getBytes(StandardCharsets.UTF_8));
        }
        File file = resolve(location);
        if (file == null || !file.isFile()) {
            throw new IOException("Resource does not exist: " + location);
        }
        return Files.newInputStream(file.toPath());
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (location.getPath().equals("pack.mcmeta")) return true;
        File file = resolve(location);
        return file != null && file.isFile();
    }

    @Override
    public Set<String> getResourceDomains() {
        return Collections.singleton("mekanism_extras");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) throws IOException {
        if (!section.equals("pack")) return null;
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 1);
        pack.addProperty("description", "Mekanism Extras Legacy configurable tier assets");
        root.add("pack", pack);
        JsonObject language = new JsonObject();
        JsonObject zh = new JsonObject();
        zh.addProperty("name", "简体中文");
        zh.addProperty("region", "CN");
        zh.addProperty("bidirectional", false);
        language.add("zh_cn", zh);
        root.add("language", language);
        return (T) serializer.parseMetadataSection(section, root);
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        return image;
    }

    @Override
    public String getPackName() {
        return "Mekanism Extras Legacy configured tiers";
    }

    @Nullable
    private File resolve(ResourceLocation location) {
        if (packRoot == null || !"mekanism_extras".equals(location.getNamespace())) return null;
        File file = new File(packRoot, "assets/" + location.getNamespace() + "/" + location.getPath());
        try {
            String root = packRoot.getCanonicalPath() + File.separator;
            if (!file.getCanonicalPath().startsWith(root)) return null;
        } catch (IOException exception) {
            return null;
        }
        return file;
    }
}
