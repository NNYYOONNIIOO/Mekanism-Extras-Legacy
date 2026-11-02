package com.jerry.mekextras.mixin;

import com.jerry.mekextras.common.util.ExtraEnergyNetworkCapacity;
import mekanism.api.transmitters.DynamicNetwork;
import mekanism.api.transmitters.IGridTransmitter;
import mekanism.common.transmitters.grid.EnergyNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = DynamicNetwork.class, remap = false)
public abstract class MixinDynamicNetwork {

    @Shadow protected Set<IGridTransmitter<?, ?, ?>> transmitters;
    @Shadow protected double doubleCapacity;
    @Shadow protected abstract void updateMeanCapacity();

    @Inject(method = "updateCapacity", at = @At("TAIL"))
    private void extras$updateEnergyCapacity(CallbackInfo callback) {
        if (!((Object) this instanceof EnergyNetwork)) return;
        double additionalCapacity = 0;
        for (IGridTransmitter<?, ?, ?> transmitter : transmitters) {
            additionalCapacity = ExtraEnergyNetworkCapacity.addCapacities(additionalCapacity,
                  ExtraEnergyNetworkCapacity.extraCapacity(transmitter));
        }
        if (additionalCapacity > 0) {
            doubleCapacity = ExtraEnergyNetworkCapacity.addCapacities(doubleCapacity, additionalCapacity);
            updateMeanCapacity();
        }
    }
}
