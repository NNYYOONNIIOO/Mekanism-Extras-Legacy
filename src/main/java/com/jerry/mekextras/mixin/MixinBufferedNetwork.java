package com.jerry.mekextras.mixin;

import com.jerry.mekextras.common.util.ExtraEnergyNetworkCapacity;
import mekanism.api.energy.EnergyStack;
import mekanism.api.transmitters.DynamicNetwork;
import mekanism.api.transmitters.IGridTransmitter;
import mekanism.common.base.EnergyAcceptorWrapper;
import mekanism.common.transmitters.grid.BufferedNetwork;
import mekanism.common.transmitters.grid.EnergyNetwork;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = BufferedNetwork.class, remap = false)
public abstract class MixinBufferedNetwork extends DynamicNetwork<EnergyAcceptorWrapper, EnergyNetwork, EnergyStack> {

    @Shadow @Final private Map<IGridTransmitter<EnergyAcceptorWrapper, EnergyNetwork, EnergyStack>, Double> saveShares;
    @Shadow private boolean sharesDirty;
    @Shadow private double savedAmount;
    @Shadow protected abstract double getSaveAmount();

    @Inject(method = "rebuildSaveShares", at = @At("HEAD"), cancellable = true)
    private void extras$saveFullEnergyShares(CallbackInfo callback) {
        if (!((Object) this instanceof EnergyNetwork)) return;

        double largestCapacity = 0;
        boolean hasLargeCable = false;
        for (IGridTransmitter<EnergyAcceptorWrapper, EnergyNetwork, EnergyStack> transmitter : transmitters) {
            double capacity = ExtraEnergyNetworkCapacity.getCapacity(transmitter);
            hasLargeCable |= capacity > Integer.MAX_VALUE && ExtraEnergyNetworkCapacity.isExtraCable(transmitter);
            largestCapacity = Math.max(largestCapacity, capacity);
        }
        if (!hasLargeCable) return;

        double remainingWeight = 0;
        for (IGridTransmitter<EnergyAcceptorWrapper, EnergyNetwork, EnergyStack> transmitter : transmitters) {
            remainingWeight += ExtraEnergyNetworkCapacity.getCapacity(transmitter) / largestCapacity;
        }
        saveShares.clear();
        double remaining = Math.max(0, getSaveAmount());
        for (IGridTransmitter<EnergyAcceptorWrapper, EnergyNetwork, EnergyStack> transmitter : transmitters) {
            double capacity = ExtraEnergyNetworkCapacity.getCapacity(transmitter);
            double weight = capacity / largestCapacity;
            double share = remainingWeight <= 0 ? 0 : Math.min(capacity, remaining * Math.min(1, weight / remainingWeight));
            saveShares.put(transmitter, share);
            remaining = Math.max(0, remaining - share);
            remainingWeight = Math.max(0, remainingWeight - weight);
        }
        savedAmount = getSaveAmount();
        sharesDirty = false;
        callback.cancel();
    }
}
