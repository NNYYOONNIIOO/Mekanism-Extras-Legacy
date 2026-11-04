package com.jerry.mekextras.common.integration;

import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraUniversalCable;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.tesla.TeslaIntegration;
import net.darkhax.tesla.api.ITeslaConsumer;
import net.darkhax.tesla.api.ITeslaHolder;
import net.darkhax.tesla.api.ITeslaProducer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Optional;

@Optional.InterfaceList({
      @Optional.Interface(iface = "net.darkhax.tesla.api.ITeslaConsumer", modid = MekanismHooks.TESLA_MOD_ID),
      @Optional.Interface(iface = "net.darkhax.tesla.api.ITeslaProducer", modid = MekanismHooks.TESLA_MOD_ID),
      @Optional.Interface(iface = "net.darkhax.tesla.api.ITeslaHolder", modid = MekanismHooks.TESLA_MOD_ID)
})
public class ExtraTeslaCableIntegration implements ITeslaConsumer, ITeslaProducer, ITeslaHolder {

    private final TileEntityExtraUniversalCable cable;
    private final EnumFacing side;

    public ExtraTeslaCableIntegration(TileEntityExtraUniversalCable cable, EnumFacing side) {
        this.cable = cable;
        this.side = side;
    }

    @Override
    @Optional.Method(modid = MekanismHooks.TESLA_MOD_ID)
    public long givePower(long power, boolean simulate) {
        return TeslaIntegration.toTesla(cable.acceptEnergy(side, TeslaIntegration.fromTesla(power), simulate));
    }

    @Override
    @Optional.Method(modid = MekanismHooks.TESLA_MOD_ID)
    public long takePower(long power, boolean simulate) {
        return TeslaIntegration.toTesla(cable.pullEnergy(side, TeslaIntegration.fromTesla(power), simulate));
    }

    @Override
    @Optional.Method(modid = MekanismHooks.TESLA_MOD_ID)
    public long getStoredPower() {
        return TeslaIntegration.toTesla(cable.getEnergy());
    }

    @Override
    @Optional.Method(modid = MekanismHooks.TESLA_MOD_ID)
    public long getCapacity() {
        return TeslaIntegration.toTesla(cable.getMaxEnergy());
    }
}
