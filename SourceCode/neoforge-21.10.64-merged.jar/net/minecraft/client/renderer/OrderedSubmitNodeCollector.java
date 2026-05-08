package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public interface OrderedSubmitNodeCollector {
    void submitHitbox(PoseStack poseStack, EntityRenderState entityRenderState, HitboxesRenderState hitboxesRenderState);

    void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces);

    void submitNameTag(
        PoseStack poseStack,
        @Nullable Vec3 pos,
        int yOffset,
        Component text,
        boolean seethrough,
        int packedLight,
        double distanceToCameraSq,
        CameraRenderState cameraRenderState
    );

    void submitText(
        PoseStack poseStack,
        float x,
        float y,
        FormattedCharSequence string,
        boolean dropShadow,
        Font.DisplayMode displayMode,
        int packedLight,
        int color,
        int backgroundColor,
        int outlineColor
    );

    void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation);

    void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState);

    <S> void submitModel(
        Model<? super S> model,
        S renderState,
        PoseStack poseStack,
        RenderType renderType,
        int packedLight,
        int packedOverlay,
        int tintColor,
        @Nullable TextureAtlasSprite sprite,
        int outlineColor,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    );

    default <S> void submitModel(
        Model<? super S> model,
        S renderState,
        PoseStack poseStack,
        RenderType renderType,
        int packedLight,
        int packedOverlay,
        int outlineColor,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        this.submitModel(model, renderState, poseStack, renderType, packedLight, packedOverlay, -1, null, outlineColor, crumblingOverlay);
    }

    default void submitModelPart(
        ModelPart modelPart, PoseStack poseStack, RenderType renderType, int packedLight, int packedOverlay, @Nullable TextureAtlasSprite sprite
    ) {
        this.submitModelPart(modelPart, poseStack, renderType, packedLight, packedOverlay, sprite, false, false, -1, null, 0);
    }

    default void submitModelPart(
        ModelPart modelPart,
        PoseStack poseStack,
        RenderType renderType,
        int packedLight,
        int packedOverlay,
        @Nullable TextureAtlasSprite sprite,
        int tintColor,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        this.submitModelPart(modelPart, poseStack, renderType, packedLight, packedOverlay, sprite, false, false, tintColor, crumblingOverlay, 0);
    }

    default void submitModelPart(
        ModelPart modelPart,
        PoseStack poseStack,
        RenderType renderType,
        int packedLight,
        int packedOverlay,
        @Nullable TextureAtlasSprite sprite,
        boolean sheeted,
        boolean hasFoil
    ) {
        this.submitModelPart(modelPart, poseStack, renderType, packedLight, packedOverlay, sprite, sheeted, hasFoil, -1, null, 0);
    }

    void submitModelPart(
        ModelPart modelPart,
        PoseStack poseStack,
        RenderType renderType,
        int packedLight,
        int packedOverlay,
        @Nullable TextureAtlasSprite sprite,
        boolean sheeted,
        boolean hasFoil,
        int tintColor,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
        int outlineColor
    );

    void submitBlock(PoseStack poseStack, BlockState blockState, int packedLight, int packedOverlay, int outlineColor);

    void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState renderState);

    void submitBlockModel(
        PoseStack poseStack,
        RenderType renderType,
        BlockStateModel model,
        float r,
        float g,
        float b,
        int packedLight,
        int packedOverlay,
        int outlineColor
    );

    void submitItem(
        PoseStack poseStack,
        ItemDisplayContext displayContext,
        int packedLight,
        int packedOverlay,
        int outlineColor,
        int[] tintLayers,
        List<BakedQuad> quads,
        RenderType renderType,
        ItemStackRenderState.FoilType foilType
    );

    void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer);

    void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer);
}
