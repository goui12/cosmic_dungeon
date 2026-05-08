package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class SubmitNodeCollection implements OrderedSubmitNodeCollector {
    private final List<SubmitNodeStorage.ShadowSubmit> shadowSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.FlameSubmit> flameSubmits = new ArrayList<>();
    private final NameTagFeatureRenderer.Storage nameTagSubmits = new NameTagFeatureRenderer.Storage();
    private final List<SubmitNodeStorage.TextSubmit> textSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.HitboxSubmit> hitboxSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.LeashSubmit> leashSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.BlockSubmit> blockSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.MovingBlockSubmit> movingBlockSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.BlockModelSubmit> blockModelSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.ItemSubmit> itemSubmits = new ArrayList<>();
    private final List<SubmitNodeCollector.ParticleGroupRenderer> particleGroupRenderers = new ArrayList<>();
    private final ModelFeatureRenderer.Storage modelSubmits = new ModelFeatureRenderer.Storage();
    private final ModelPartFeatureRenderer.Storage modelPartSubmits = new ModelPartFeatureRenderer.Storage();
    private final CustomFeatureRenderer.Storage customGeometrySubmits = new CustomFeatureRenderer.Storage();
    private final SubmitNodeStorage submitNodeStorage;
    private boolean wasUsed = false;

    public SubmitNodeCollection(SubmitNodeStorage submitNodeStorage) {
        this.submitNodeStorage = submitNodeStorage;
    }

    @Override
    public void submitHitbox(PoseStack poseStack, EntityRenderState entityRenderState, HitboxesRenderState hitboxesRenderState) {
        this.wasUsed = true;
        this.hitboxSubmits.add(new SubmitNodeStorage.HitboxSubmit(new Matrix4f(poseStack.last().pose()), entityRenderState, hitboxesRenderState));
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        this.wasUsed = true;
        PoseStack.Pose posestack$pose = poseStack.last();
        this.shadowSubmits.add(new SubmitNodeStorage.ShadowSubmit(new Matrix4f(posestack$pose.pose()), radius, pieces));
    }

    @Override
    public void submitNameTag(
        PoseStack poseStack,
        @Nullable Vec3 pos,
        int yOffset,
        Component text,
        boolean seethrough,
        int packedLight,
        double distanceToCameraSq,
        CameraRenderState cameraRenderState
    ) {
        this.wasUsed = true;
        this.nameTagSubmits.add(poseStack, pos, yOffset, text, seethrough, packedLight, distanceToCameraSq, cameraRenderState);
    }

    @Override
    public void submitText(
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
    ) {
        this.wasUsed = true;
        this.textSubmits
            .add(
                new SubmitNodeStorage.TextSubmit(
                    new Matrix4f(poseStack.last().pose()), x, y, string, dropShadow, displayMode, packedLight, color, backgroundColor, outlineColor
                )
            );
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        this.wasUsed = true;
        this.flameSubmits.add(new SubmitNodeStorage.FlameSubmit(poseStack.last().copy(), renderState, rotation));
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        this.wasUsed = true;
        this.leashSubmits.add(new SubmitNodeStorage.LeashSubmit(new Matrix4f(poseStack.last().pose()), leashState));
    }

    @Override
    public <S> void submitModel(
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
    ) {
        this.wasUsed = true;
        SubmitNodeStorage.ModelSubmit<S> modelsubmit = new SubmitNodeStorage.ModelSubmit<>(
            poseStack.last().copy(), model, renderState, packedLight, packedOverlay, tintColor, sprite, outlineColor, crumblingOverlay
        );
        this.modelSubmits.add(renderType, modelsubmit);
    }

    @Override
    public void submitModelPart(
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
    ) {
        this.wasUsed = true;
        this.modelPartSubmits
            .add(
                renderType,
                new SubmitNodeStorage.ModelPartSubmit(
                    poseStack.last().copy(), modelPart, packedLight, packedOverlay, sprite, sheeted, hasFoil, tintColor, crumblingOverlay, outlineColor
                )
            );
    }

    @Override
    public void submitBlock(PoseStack poseStack, BlockState blockState, int packedLight, int packedOverlay, int outlineColor) {
        this.wasUsed = true;
        this.blockSubmits.add(new SubmitNodeStorage.BlockSubmit(poseStack.last().copy(), blockState, packedLight, packedOverlay, outlineColor));
        Minecraft.getInstance()
            .getModelManager()
            .specialBlockModelRenderer()
            .get()
            .renderByBlock(blockState.getBlock(), ItemDisplayContext.NONE, poseStack, this.submitNodeStorage, packedLight, packedOverlay, outlineColor);
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState renderState) {
        this.wasUsed = true;
        this.movingBlockSubmits.add(new SubmitNodeStorage.MovingBlockSubmit(new Matrix4f(poseStack.last().pose()), renderState));
    }

    @Override
    public void submitBlockModel(
        PoseStack poseStack,
        RenderType renderType,
        BlockStateModel model,
        float r,
        float g,
        float b,
        int packedLight,
        int packedOverlay,
        int outlineColor
    ) {
        this.wasUsed = true;
        this.blockModelSubmits
            .add(
                new SubmitNodeStorage.BlockModelSubmit(
                    poseStack.last().copy(), renderType, model, r, g, b, packedLight, packedOverlay, outlineColor
                )
            );
    }

    @Override
    public void submitItem(
        PoseStack poseStack,
        ItemDisplayContext displayContext,
        int packedLight,
        int packedOverlay,
        int outlineColor,
        int[] tintLayers,
        List<BakedQuad> quads,
        RenderType renderType,
        ItemStackRenderState.FoilType foilType
    ) {
        this.wasUsed = true;
        this.itemSubmits
            .add(
                new SubmitNodeStorage.ItemSubmit(
                    poseStack.last().copy(), displayContext, packedLight, packedOverlay, outlineColor, tintLayers, quads, renderType, foilType
                )
            );
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        this.wasUsed = true;
        this.customGeometrySubmits.add(poseStack, renderType, renderer);
    }

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
        this.wasUsed = true;
        this.particleGroupRenderers.add(renderer);
    }

    public List<SubmitNodeStorage.ShadowSubmit> getShadowSubmits() {
        return this.shadowSubmits;
    }

    public List<SubmitNodeStorage.FlameSubmit> getFlameSubmits() {
        return this.flameSubmits;
    }

    public NameTagFeatureRenderer.Storage getNameTagSubmits() {
        return this.nameTagSubmits;
    }

    public List<SubmitNodeStorage.TextSubmit> getTextSubmits() {
        return this.textSubmits;
    }

    public List<SubmitNodeStorage.HitboxSubmit> getHitboxSubmits() {
        return this.hitboxSubmits;
    }

    public List<SubmitNodeStorage.LeashSubmit> getLeashSubmits() {
        return this.leashSubmits;
    }

    public List<SubmitNodeStorage.BlockSubmit> getBlockSubmits() {
        return this.blockSubmits;
    }

    public List<SubmitNodeStorage.MovingBlockSubmit> getMovingBlockSubmits() {
        return this.movingBlockSubmits;
    }

    public List<SubmitNodeStorage.BlockModelSubmit> getBlockModelSubmits() {
        return this.blockModelSubmits;
    }

    public ModelPartFeatureRenderer.Storage getModelPartSubmits() {
        return this.modelPartSubmits;
    }

    public List<SubmitNodeStorage.ItemSubmit> getItemSubmits() {
        return this.itemSubmits;
    }

    public List<SubmitNodeCollector.ParticleGroupRenderer> getParticleGroupRenderers() {
        return this.particleGroupRenderers;
    }

    public ModelFeatureRenderer.Storage getModelSubmits() {
        return this.modelSubmits;
    }

    public CustomFeatureRenderer.Storage getCustomGeometrySubmits() {
        return this.customGeometrySubmits;
    }

    public boolean wasUsed() {
        return this.wasUsed;
    }

    public void clear() {
        this.shadowSubmits.clear();
        this.flameSubmits.clear();
        this.nameTagSubmits.clear();
        this.textSubmits.clear();
        this.hitboxSubmits.clear();
        this.leashSubmits.clear();
        this.blockSubmits.clear();
        this.movingBlockSubmits.clear();
        this.blockModelSubmits.clear();
        this.itemSubmits.clear();
        this.particleGroupRenderers.clear();
        this.modelSubmits.clear();
        this.customGeometrySubmits.clear();
        this.modelPartSubmits.clear();
    }

    public void endFrame() {
        this.modelSubmits.endFrame();
        this.modelPartSubmits.endFrame();
        this.customGeometrySubmits.endFrame();
        this.wasUsed = false;
    }
}
