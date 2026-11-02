package com.jerry.mekextras.client.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Adds the generated config resource pack as the lowest-priority pack after vanilla's default pack. The pack is
 * created before the first model reload, so no manual reload is required.
 */
@SideOnly(Side.CLIENT)
public final class TierResourcePackInjector {

    private TierResourcePackInjector() {
    }

    @SuppressWarnings("unchecked")
    public static void install() {
        try {
            Field field;
            try {
                field = Minecraft.class.getDeclaredField("field_110449_ao");
            } catch (NoSuchFieldException exception) {
                field = Minecraft.class.getDeclaredField("defaultResourcePacks");
            }
            field.setAccessible(true);
            List<IResourcePack> packs = (List<IResourcePack>) field.get(Minecraft.getMinecraft());
            packs.removeIf(pack -> pack instanceof TierResourcePack);
            packs.add(new TierResourcePack());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            com.jerry.mekextras.MekanismExtras.logger.warn(
                  "Configurable tier resource pack is unavailable; built-in tier assets remain active", exception);
        }
    }
}
