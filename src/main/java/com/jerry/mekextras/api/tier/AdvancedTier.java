package com.jerry.mekextras.api.tier;

import com.jerry.mekextras.common.config.ExtraConfig;
import mekanism.api.EnumColor;
import mekanism.common.tier.BaseTier;

import java.util.Collection;
import java.util.Locale;

public enum AdvancedTier implements IAdvancedTier {

    ABSOLUTE("Absolute", 0xE8E983, EnumColor.BRIGHT_GREEN),
    SUPREME("Supreme", 0xE65249, EnumColor.DARK_RED),
    COSMIC("Cosmic", 0xFACEF7, EnumColor.AQUA),
    INFINITE("Infinite", 0xDFD9AB, EnumColor.PURPLE),
    LUNA("Luna", 0xFFFFFF, EnumColor.WHITE);

    private static final AdvancedTier[] TIERS = values();

    private final String name;
    private final int rgb;
    private final EnumColor color;

    AdvancedTier(String name, int rgb, EnumColor color) {
        this.name = name;
        this.rgb = rgb;
        this.color = color;
    }

    public String getSimpleName() {
        return ExtraConfig.getTierName(this, name);
    }

    public String getLowerName() {
        return ExtraConfig.getTierId(this, name.toLowerCase(Locale.ROOT));
    }

    /**
     * @return the RGB tint used for this tier's blocks, items and textures
     */
    public int getRgb() {
        return ExtraConfig.getTierRgb(this, rgb);
    }

    /** Client display colour, including the configured animated Cosmic and Infinite gradients. */
    public int getDisplayColor(float ticks) {
        Collection<Integer> configured = ExtraConfig.getTierPalette(this);
        if (configured.size() > 1) {
            int[] palette = configured.stream().mapToInt(Integer::intValue).toArray();
            int[] order = ExtraConfig.getTierPaletteOrder(this).stream().mapToInt(Integer::intValue).toArray();
            if (order.length < 2) return rgb;
            return animate(ticks, palette, order, ExtraConfig.getTierFrameTicks(this, 7.0F));
        }
        return rgb;
    }

    private static int animate(float ticks, int[] palette, int[] order, float frameTicks) {
        float position = Math.max(0, ticks) / frameTicks;
        int frame = (int) Math.floor(position) % order.length;
        int nextFrame = (frame + 1) % order.length;
        float progress = position - (float) Math.floor(position);
        return lerpColor(palette[order[frame]], palette[order[nextFrame]], progress);
    }

    private static int lerpColor(int from, int to, float progress) {
        int red = Math.round(((from >> 16) & 0xFF) + ((((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * progress));
        int green = Math.round(((from >> 8) & 0xFF) + ((((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * progress));
        int blue = Math.round((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * progress));
        return red << 16 | green << 8 | blue;
    }

    public EnumColor getColor() {
        return ExtraConfig.getTierColor(this, color);
    }

    public boolean isObtainable() {
        return ExtraConfig.isTierObtainable(this);
    }

    @Override
    public AdvancedTier getAdvanceTier() {
        return this;
    }

    @Override
    public BaseTier getBaseTier() {
        return this == LUNA ? BaseTier.ULTIMATE : BaseTier.values()[ordinal()];
    }

    public static AdvancedTier byIndex(int index) {
        return index >= 0 && index < TIERS.length ? TIERS[index] : ABSOLUTE;
    }

    public static AdvancedTier byName(String name) {
        for (AdvancedTier tier : TIERS) {
            if (tier.getSimpleName().equalsIgnoreCase(name) || tier.getLowerName().equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return ABSOLUTE;
    }

    public static String getName(int ordinal) {
        return byIndex(ordinal).getSimpleName();
    }

    public static String getLowerName(int ordinal) {
        return byIndex(ordinal).getLowerName();
    }

    public static int getRgb(int ordinal) {
        return byIndex(ordinal).getRgb();
    }

    public static int getDisplayColor(int ordinal, float ticks) {
        return byIndex(ordinal).getDisplayColor(ticks);
    }
}

