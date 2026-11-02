package com.jerry.mekextras.client.model;

import mekanism.client.render.item.ItemLayerWrapper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;

@SideOnly(Side.CLIENT)
public class ConfiguredItemLayerWrapper extends ItemLayerWrapper {

    public ConfiguredItemLayerWrapper(IBakedModel internal) {
        super(internal);
    }

    @Nonnull
    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(@Nonnull TransformType type) {
        super.handlePerspective(type);
        return Pair.of(this, getInternal().handlePerspective(type).getRight());
    }
}
