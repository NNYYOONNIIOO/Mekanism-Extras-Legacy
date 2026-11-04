package com.jerry.mekextras.common.integration;

import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraUniversalCable;
import mekanism.common.integration.forgeenergy.ForgeEnergyIntegration;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.IEnergyStorage;

public class ExtraForgeEnergyCableIntegration implements IEnergyStorage {

    private final TileEntityExtraUniversalCable cable;
    private final EnumFacing side;

    public ExtraForgeEnergyCableIntegration(TileEntityExtraUniversalCable cable, EnumFacing side) {
        this.cable = cable;
        this.side = side;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return ForgeEnergyIntegration.toForge(cable.acceptEnergy(side, ForgeEnergyIntegration.fromForge(maxReceive), simulate));
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return ForgeEnergyIntegration.toForge(cable.pullEnergy(side, ForgeEnergyIntegration.fromForge(maxExtract), simulate));
    }

    @Override
    public int getEnergyStored() {
        return ForgeEnergyIntegration.toForge(cable.getEnergy());
    }

    @Override
    public int getMaxEnergyStored() {
        return ForgeEnergyIntegration.toForge(cable.getMaxEnergy());
    }

    @Override
    public boolean canExtract() {
        return cable.canOutputEnergy(side);
    }

    @Override
    public boolean canReceive() {
        return cable.canReceiveEnergy(side);
    }
}
