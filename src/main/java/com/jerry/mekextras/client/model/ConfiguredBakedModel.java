package com.jerry.mekextras.client.model;

import com.jerry.mekextras.api.tier.AdvancedTier;
import com.jerry.mekextras.common.config.ExtraConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Retains the baked geometry and applies tier-configured camera transforms. Texture replacement is done by the
 * resource pack generated from tiers.json; this wrapper intentionally does not change quad UVs at runtime.
 */
@SideOnly(Side.CLIENT)
public class ConfiguredBakedModel implements IBakedModel {

    private final IBakedModel parent;
    private final AdvancedTier tier;

    public ConfiguredBakedModel(IBakedModel parent, AdvancedTier tier) {
        this.parent = parent;
        this.tier = tier;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return parent.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return parent.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return parent.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return parent.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return parent.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        ItemCameraTransforms original = parent.getItemCameraTransforms();
        net.minecraft.client.renderer.block.model.ItemTransformVec3f thirdLeft =
              configuredTransform(original.thirdperson_left, "thirdperson_lefthand");
        net.minecraft.client.renderer.block.model.ItemTransformVec3f thirdRight =
              configuredTransform(original.thirdperson_right, "thirdperson_righthand");
        net.minecraft.client.renderer.block.model.ItemTransformVec3f firstLeft =
              configuredTransform(original.firstperson_left, "firstperson_lefthand");
        net.minecraft.client.renderer.block.model.ItemTransformVec3f firstRight =
              configuredTransform(original.firstperson_right, "firstperson_righthand");
        net.minecraft.client.renderer.block.model.ItemTransformVec3f head =
              configuredTransform(original.head, "head");
        net.minecraft.client.renderer.block.model.ItemTransformVec3f gui =
              configuredTransform(original.gui, "gui");
        net.minecraft.client.renderer.block.model.ItemTransformVec3f ground =
              configuredTransform(original.ground, "ground");
        net.minecraft.client.renderer.block.model.ItemTransformVec3f fixed =
              configuredTransform(original.fixed, "fixed");
        return new ItemCameraTransforms(thirdLeft, thirdRight, firstLeft, firstRight, head, gui, ground, fixed);
    }

    public IBakedModel getParent() {
        return parent;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return parent.getOverrides();
    }

    @Override
    public org.apache.commons.lang3.tuple.Pair<? extends IBakedModel, javax.vecmath.Matrix4f> handlePerspective(
          ItemCameraTransforms.TransformType type) {
        org.apache.commons.lang3.tuple.Pair<? extends IBakedModel, javax.vecmath.Matrix4f> result = parent.handlePerspective(type);
        if (result == null) return null;
        String perspective = perspectiveName(type);
        float[] rotation = ExtraConfig.getDisplayRotation(tier, perspective, null);
        float[] translation = ExtraConfig.getDisplayTranslation(tier, perspective, null);
        float[] scale = ExtraConfig.getDisplayScale(tier, perspective, null);
        if (rotation == null && translation == null && scale == null) {
            return org.apache.commons.lang3.tuple.Pair.of(this, result.getRight());
        }
        net.minecraft.client.renderer.block.model.ItemTransformVec3f configured =
              configuredTransform(parent.getItemCameraTransforms().getTransform(type), perspective);
        return org.apache.commons.lang3.tuple.Pair.of(this,
              net.minecraftforge.common.model.TRSRTransformation.from(configured).getMatrix());
    }

    private net.minecraft.client.renderer.block.model.ItemTransformVec3f configuredTransform(
          net.minecraft.client.renderer.block.model.ItemTransformVec3f original, String perspective) {
        float[] rotation = ExtraConfig.getDisplayRotation(tier, perspective,
              new float[]{original.rotation.x, original.rotation.y, original.rotation.z});
        float[] translation = ExtraConfig.getDisplayTranslation(tier, perspective, null);
        float[] scale = ExtraConfig.getDisplayScale(tier, perspective,
              new float[]{original.scale.x, original.scale.y, original.scale.z});
        org.lwjgl.util.vector.Vector3f translated = translation == null
              ? new org.lwjgl.util.vector.Vector3f(original.translation)
              : new org.lwjgl.util.vector.Vector3f(translation[0] / 16F, translation[1] / 16F, translation[2] / 16F);
        return new net.minecraft.client.renderer.block.model.ItemTransformVec3f(
              new org.lwjgl.util.vector.Vector3f(rotation[0], rotation[1], rotation[2]),
              translated,
              new org.lwjgl.util.vector.Vector3f(scale[0], scale[1], scale[2]));
    }

    private static String perspectiveName(ItemCameraTransforms.TransformType type) {
        switch (type) {
            case THIRD_PERSON_LEFT_HAND: return "thirdperson_lefthand";
            case THIRD_PERSON_RIGHT_HAND: return "thirdperson_righthand";
            case FIRST_PERSON_LEFT_HAND: return "firstperson_lefthand";
            case FIRST_PERSON_RIGHT_HAND: return "firstperson_righthand";
            case HEAD: return "head";
            case GUI: return "gui";
            case GROUND: return "ground";
            case FIXED: return "fixed";
            default: return "none";
        }
    }
}
