package com.jerry.mekextras.common.util;

import com.jerry.mekextras.common.tile.transmitter.TileEntityExtraUniversalCable;
import mekanism.api.transmitters.IGridTransmitter;
import mekanism.common.transmitters.TransmitterImpl;

public final class ExtraEnergyNetworkCapacity {

    private ExtraEnergyNetworkCapacity() {
    }

    public static boolean isExtraCable(IGridTransmitter<?, ?, ?> transmitter) {
        return transmitter instanceof TransmitterImpl<?, ?, ?>
              && ((TransmitterImpl<?, ?, ?>) transmitter).getTileEntity() instanceof TileEntityExtraUniversalCable;
    }

    public static double getCapacity(IGridTransmitter<?, ?, ?> transmitter) {
        if (isExtraCable(transmitter)) {
            TileEntityExtraUniversalCable cable = (TileEntityExtraUniversalCable) ((TransmitterImpl<?, ?, ?>) transmitter).getTileEntity();
            return cable.getFullCapacity();
        }
        return Math.max(0, transmitter.getCapacity());
    }

    public static double addCapacities(double first, double second) {
        return first > Double.MAX_VALUE - second ? Double.MAX_VALUE : first + second;
    }

    public static double extraCapacity(IGridTransmitter<?, ?, ?> transmitter) {
        return isExtraCable(transmitter) ? Math.max(0, getCapacity(transmitter) - transmitter.getCapacity()) : 0;
    }
}
