package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class SubmitNodeStorage implements SubmitNodeCollector {
    private final Int2ObjectAVLTreeMap<SubmitNodeCollection> submitsPerOrder = new Int2ObjectAVLTreeMap<>();

    public SubmitNodeCollection order(int p_439752_) {
        return this.submitsPerOrder.computeIfAbsent(p_439752_, p_438783_ -> new SubmitNodeCollection(this));
    }

    @Override
    public void submitHitbox(PoseStack p_433419_, EntityRenderState p_434449_, HitboxesRenderState p_433471_) {
        this.order(0).submitHitbox(p_433419_, p_434449_, p_433471_);
    }

    @Override
    public void submitShadow(PoseStack p_434181_, float p_435337_, List<EntityRenderState.ShadowPiece> p_432741_) {
        this.order(0).submitShadow(p_434181_, p_435337_, p_432741_);
    }

    @Override
    public void submitNameTag(
        PoseStack p_434460_,
        @Nullable Vec3 p_434946_,
        int p_435789_,
        Component p_433492_,
        boolean p_433752_,
        int p_451638_,
        double p_434416_,
        CameraRenderState p_451305_
    ) {
        this.order(0).submitNameTag(p_434460_, p_434946_, p_435789_, p_433492_, p_433752_, p_451638_, p_434416_, p_451305_);
    }

    @Override
    public void submitText(
        PoseStack p_433468_,
        float p_435856_,
        float p_433422_,
        FormattedCharSequence p_432906_,
        boolean p_432998_,
        Font.DisplayMode p_433889_,
        int p_435431_,
        int p_435185_,
        int p_434470_,
        int p_440091_
    ) {
        this.order(0).submitText(p_433468_, p_435856_, p_433422_, p_432906_, p_432998_, p_433889_, p_435431_, p_435185_, p_434470_, p_440091_);
    }

    @Override
    public void submitFlame(PoseStack p_432801_, EntityRenderState p_433147_, Quaternionf p_433641_) {
        this.order(0).submitFlame(p_432801_, p_433147_, p_433641_);
    }

    @Override
    public void submitLeash(PoseStack p_435279_, EntityRenderState.LeashState p_433278_) {
        this.order(0).submitLeash(p_435279_, p_433278_);
    }

    @Override
    public <S> void submitModel(
        Model<? super S> p_433938_,
        S p_434123_,
        PoseStack p_434445_,
        RenderType p_434095_,
        int p_433912_,
        int p_435238_,
        int p_433959_,
        @Nullable TextureAtlasSprite p_433439_,
        int p_435627_,
        @Nullable ModelFeatureRenderer.CrumblingOverlay p_439709_
    ) {
        this.order(0).submitModel(p_433938_, p_434123_, p_434445_, p_434095_, p_433912_, p_435238_, p_433959_, p_433439_, p_435627_, p_439709_);
    }

    @Override
    public void submitModelPart(
        ModelPart p_439246_,
        PoseStack p_440594_,
        RenderType p_440135_,
        int p_440257_,
        int p_439236_,
        @Nullable TextureAtlasSprite p_439406_,
        boolean p_440273_,
        boolean p_440604_,
        int p_440090_,
        ModelFeatureRenderer.CrumblingOverlay p_442784_,
        int p_451669_
    ) {
        this.order(0).submitModelPart(p_439246_, p_440594_, p_440135_, p_440257_, p_439236_, p_439406_, p_440273_, p_440604_, p_440090_, p_442784_, p_451669_);
    }

    @Override
    public void submitBlock(PoseStack p_435262_, BlockState p_434652_, int p_433842_, int p_432806_, int p_440733_) {
        this.order(0).submitBlock(p_435262_, p_434652_, p_433842_, p_432806_, p_440733_);
    }

    @Override
    public void submitMovingBlock(PoseStack p_438975_, MovingBlockRenderState p_439050_) {
        this.order(0).submitMovingBlock(p_438975_, p_439050_);
    }

    @Override
    public void submitBlockModel(
        PoseStack p_434193_,
        RenderType p_432947_,
        BlockStateModel p_434441_,
        float p_433630_,
        float p_434933_,
        float p_434573_,
        int p_435375_,
        int p_435284_,
        int p_440730_
    ) {
        this.order(0).submitBlockModel(p_434193_, p_432947_, p_434441_, p_433630_, p_434933_, p_434573_, p_435375_, p_435284_, p_440730_);
    }

    @Override
    public void submitItem(
        PoseStack p_434225_,
        ItemDisplayContext p_439693_,
        int p_433159_,
        int p_435776_,
        int p_440725_,
        int[] p_439144_,
        List<BakedQuad> p_439266_,
        RenderType p_439740_,
        ItemStackRenderState.FoilType p_440348_
    ) {
        this.order(0).submitItem(p_434225_, p_439693_, p_433159_, p_435776_, p_440725_, p_439144_, p_439266_, p_439740_, p_440348_);
    }

    @Override
    public void submitCustomGeometry(PoseStack p_435351_, RenderType p_433293_, SubmitNodeCollector.CustomGeometryRenderer p_432908_) {
        this.order(0).submitCustomGeometry(p_435351_, p_433293_, p_432908_);
    }

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer p_446769_) {
        this.order(0).submitParticleGroup(p_446769_);
    }

    public void clear() {
        this.submitsPerOrder.values().forEach(SubmitNodeCollection::clear);
    }

    public void endFrame() {
        this.submitsPerOrder.values().removeIf(p_438784_ -> !p_438784_.wasUsed());
        this.submitsPerOrder.values().forEach(SubmitNodeCollection::endFrame);
    }

    public Int2ObjectAVLTreeMap<SubmitNodeCollection> getSubmitsPerOrder() {
        return this.submitsPerOrder;
    }

    @OnlyIn(Dist.CLIENT)
    public record BlockModelSubmit(
        PoseStack.Pose pose, RenderType renderType, BlockStateModel model, float r, float g, float b, int lightCoords, int overlayCoords, int outlineColor
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    public record BlockSubmit(PoseStack.Pose pose, BlockState state, int lightCoords, int overlayCoords, int outlineColor) {
    }

    @OnlyIn(Dist.CLIENT)
    public record CustomGeometrySubmit(PoseStack.Pose pose, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
    }

    @OnlyIn(Dist.CLIENT)
    public record FlameSubmit(PoseStack.Pose pose, EntityRenderState entityRenderState, Quaternionf rotation) {
    }

    @OnlyIn(Dist.CLIENT)
    public record HitboxSubmit(Matrix4f pose, EntityRenderState entityRenderState, HitboxesRenderState hitboxesRenderState) {
    }

    @OnlyIn(Dist.CLIENT)
    public record ItemSubmit(
        PoseStack.Pose pose,
        ItemDisplayContext displayContext,
        int lightCoords,
        int overlayCoords,
        int outlineColor,
        int[] tintLayers,
        List<BakedQuad> quads,
        RenderType renderType,
        ItemStackRenderState.FoilType foilType
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    public record LeashSubmit(Matrix4f pose, EntityRenderState.LeashState leashState) {
    }

    @OnlyIn(Dist.CLIENT)
    public record ModelPartSubmit(
        PoseStack.Pose pose,
        ModelPart modelPart,
        int lightCoords,
        int overlayCoords,
        @Nullable TextureAtlasSprite sprite,
        boolean sheeted,
        boolean hasFoil,
        int tintedColor,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
        int outlineColor
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    public record ModelSubmit<S>(
        PoseStack.Pose pose,
        Model<? super S> model,
        S state,
        int lightCoords,
        int overlayCoords,
        int tintedColor,
        @Nullable TextureAtlasSprite sprite,
        int outlineColor,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    public record MovingBlockSubmit(Matrix4f pose, MovingBlockRenderState movingBlockRenderState) {
    }

    @OnlyIn(Dist.CLIENT)
    public record NameTagSubmit(Matrix4f pose, float x, float y, Component text, int lightCoords, int color, int backgroundColor, double distanceToCameraSq) {
    }

    @OnlyIn(Dist.CLIENT)
    public record ShadowSubmit(Matrix4f pose, float radius, List<EntityRenderState.ShadowPiece> pieces) {
    }

    @OnlyIn(Dist.CLIENT)
    public record TextSubmit(
        Matrix4f pose,
        float x,
        float y,
        FormattedCharSequence string,
        boolean dropShadow,
        Font.DisplayMode displayMode,
        int lightCoords,
        int color,
        int backgroundColor,
        int outlineColor
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    public record TranslucentModelSubmit<S>(SubmitNodeStorage.ModelSubmit<S> modelSubmit, RenderType renderType, Vector3f position) {
    }
}
