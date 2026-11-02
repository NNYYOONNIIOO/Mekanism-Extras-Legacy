package com.jerry.mekextras.common.tier;

import com.jerry.mekextras.api.tier.AdvancedTier;
import com.jerry.mekextras.api.tier.IAdvancedTier;
import com.jerry.mekextras.common.config.ExtraConfig;
import mekanism.common.tier.BaseTier;
import net.minecraft.util.IStringSerializable;

import java.util.Locale;

public enum ExtraFactoryTier implements IAdvancedTier, IStringSerializable {

    ABSOLUTE(AdvancedTier.ABSOLUTE, 11),
    SUPREME(AdvancedTier.SUPREME, 13),
    COSMIC(AdvancedTier.COSMIC, 15),
    INFINITE(AdvancedTier.INFINITE, 17),
    LUNA(AdvancedTier.LUNA, 21);

    public final int processes;
    private final AdvancedTier advancedTier;

    ExtraFactoryTier(AdvancedTier advancedTier, int processes) {
        this.advancedTier = advancedTier;
        this.processes = processes;
    }

    public int getProcesses() {
        return ExtraConfig.getInt(advancedTier, "extraFactoryProcesses", processes);
    }

    @Override
    public AdvancedTier getAdvanceTier() {
        return advancedTier;
    }

    @Override
    public BaseTier getBaseTier() {
        return advancedTier.getBaseTier();
    }

    @Override
    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ExtraFactoryTier byIndex(int index) {
        ExtraFactoryTier[] values = values();
        return index >= 0 && index < values.length ? values[index] : ABSOLUTE;
    }
}
