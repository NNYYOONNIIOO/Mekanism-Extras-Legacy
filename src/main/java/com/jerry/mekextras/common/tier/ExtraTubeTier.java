package com.jerry.mekextras.common.tier;

import com.jerry.mekextras.api.tier.AdvancedTier;
import com.jerry.mekextras.api.tier.IAdvancedTier;
import com.jerry.mekextras.common.config.ExtraConfig;
import mekanism.common.tier.BaseTier;

/**
 * Pressurized tube tiers above Mekanism's own {@link mekanism.common.tier.TubeTier#ULTIMATE}.
 */
public enum ExtraTubeTier implements IAdvancedTier {

    ABSOLUTE(AdvancedTier.ABSOLUTE, 65_536, 16_384),
    SUPREME(AdvancedTier.SUPREME, 262_144, 65_536),
    COSMIC(AdvancedTier.COSMIC, 1_048_576, 262_144),
    INFINITE(AdvancedTier.INFINITE, 4_194_304, 1_048_576),
    LUNA(AdvancedTier.LUNA, 16_777_216, 4_194_304);

    private final AdvancedTier advancedTier;
    private final int capacity;
    private final int pull;

    ExtraTubeTier(AdvancedTier advancedTier, int capacity, int pull) {
        this.advancedTier = advancedTier;
        this.capacity = capacity;
        this.pull = pull;
    }

    public int getTubeCapacity() {
        return ExtraConfig.getInt(advancedTier, "pressurizedTubeCapacity", capacity);
    }

    public int getTubePullAmount() {
        return ExtraConfig.getInt(advancedTier, "pressurizedTubePull", pull);
    }

    public int getBaseCapacity() {
        return getTubeCapacity();
    }

    public int getBasePull() {
        return getTubePullAmount();
    }

    @Override
    public AdvancedTier getAdvanceTier() {
        return advancedTier;
    }

    @Override
    public BaseTier getBaseTier() {
        return advancedTier.getBaseTier();
    }

    public static ExtraTubeTier byIndex(int index) {
        ExtraTubeTier[] values = values();
        return index >= 0 && index < values.length ? values[index] : ABSOLUTE;
    }

    public static ExtraTubeTier get(BaseTier tier) {
        if (tier == null) {
            return ABSOLUTE;
        }
        ExtraTubeTier[] values = values();
        int index = tier.ordinal() - BaseTier.ULTIMATE.ordinal();
        return index >= 0 && index < values.length ? values[index] : ABSOLUTE;
    }
}
