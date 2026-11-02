package com.jerry.mekextras.mixin;

import com.jerry.mekextras.common.config.ExtraConfig;
import com.jerry.mekextras.common.util.ExtraEnergyNetworkCapacity;
import mekanism.api.energy.EnergyStack;
import mekanism.api.transmitters.IGridTransmitter;
import mekanism.common.base.EnergyAcceptorWrapper;
import mekanism.common.transmitters.grid.BufferedNetwork;
import mekanism.common.transmitters.grid.EnergyNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EnergyNetwork.class, remap = false)
public abstract class MixinEnergyNetwork extends BufferedNetwork<EnergyAcceptorWrapper, EnergyNetwork, EnergyStack> {

    @Unique private long extras$tierConfigRevision = -1;

    @Inject(method = "absorbBuffer", at = @At("HEAD"))
    private void extras$admitFullCableCapacity(IGridTransmitter<EnergyAcceptorWrapper, EnergyNetwork, EnergyStack> transmitter,
                                                CallbackInfo callback) {
        if (ExtraEnergyNetworkCapacity.isExtraCable(transmitter)) {
            doubleCapacity = ExtraEnergyNetworkCapacity.addCapacities(doubleCapacity,
                  ExtraEnergyNetworkCapacity.extraCapacity(transmitter));
            capacity = (int) Math.min(Integer.MAX_VALUE, doubleCapacity);
        }
    }

    @Inject(method = "preTick", at = @At("HEAD"))
    private void extras$refreshChangedTierCapacity(CallbackInfo callback) {
        long revision = ExtraConfig.getTierConfigRevision();
        if (extras$tierConfigRevision != revision) {
            extras$tierConfigRevision = revision;
            updateCapacity();
            clampBuffer();
        }
    }
}
