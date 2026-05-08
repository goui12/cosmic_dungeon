package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.model.PiglinHeadModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.dragon.DragonHeadModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkullBlockRenderer implements BlockEntityRenderer<SkullBlockEntity, SkullBlockRenderState> {
    private final Function<SkullBlock.Type, SkullModelBase> modelByType;
    public static final Map<SkullBlock.Type, ResourceLocation> SKIN_BY_TYPE = Util.make(Maps.newHashMap(), p_349877_ -> {
        p_349877_.put(SkullBlock.Types.SKELETON, ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png"));
        p_349877_.put(SkullBlock.Types.WITHER_SKELETON, ResourceLocation.withDefaultNamespace("textures/entity/skeleton/wither_skeleton.png"));
        p_349877_.put(SkullBlock.Types.ZOMBIE, ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png"));
        p_349877_.put(SkullBlock.Types.CREEPER, ResourceLocation.withDefaultNamespace("textures/entity/creeper/creeper.png"));
        p_349877_.put(SkullBlock.Types.DRAGON, ResourceLocation.withDefaultNamespace("textures/entity/enderdragon/dragon.png"));
        p_349877_.put(SkullBlock.Types.PIGLIN, ResourceLocation.withDefaultNamespace("textures/entity/piglin/piglin.png"));
        p_349877_.put(SkullBlock.Types.PLAYER, DefaultPlayerSkin.getDefaultTexture());
    });
    private final PlayerSkinRenderCache playerSkinRenderCache;

    @Nullable
    public static SkullModelBase createModel(EntityModelSet modelSet, SkullBlock.Type type) {
        if (type instanceof SkullBlock.Types skullblock$types) {
            return (SkullModelBase)(switch (skullblock$types) {
                case SKELETON -> new SkullModel(modelSet.bakeLayer(ModelLayers.SKELETON_SKULL));
                case WITHER_SKELETON -> new SkullModel(modelSet.bakeLayer(ModelLayers.WITHER_SKELETON_SKULL));
                case PLAYER -> new SkullModel(modelSet.bakeLayer(ModelLayers.PLAYER_HEAD));
                case ZOMBIE -> new SkullModel(modelSet.bakeLayer(ModelLayers.ZOMBIE_HEAD));
                case CREEPER -> new SkullModel(modelSet.bakeLayer(ModelLayers.CREEPER_HEAD));
                case DRAGON -> new DragonHeadModel(modelSet.bakeLayer(ModelLayers.DRAGON_SKULL));
                case PIGLIN -> new PiglinHeadModel(modelSet.bakeLayer(ModelLayers.PIGLIN_HEAD));
            });
        } else {
            return net.neoforged.neoforge.client.ClientHooks.getModdedSkullModel(modelSet, type); // Neo: Lookup model for modded skull types
        }
    }

    public SkullBlockRenderer(BlockEntityRendererProvider.Context context) {
        EntityModelSet entitymodelset = context.entityModelSet();
        this.playerSkinRenderCache = context.playerSkinRenderCache();
        this.modelByType = Util.memoize(p_386232_ -> createModel(entitymodelset, p_386232_));
    }

    public SkullBlockRenderState createRenderState() {
        return new SkullBlockRenderState();
    }

    public void extractRenderState(
        SkullBlockEntity blockEntity, SkullBlockRenderState renderState, float partialTick, Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, renderState, partialTick, cameraPosition, breakProgress);
        renderState.animationProgress = blockEntity.getAnimation(partialTick);
        BlockState blockstate = blockEntity.getBlockState();
        boolean flag = blockstate.getBlock() instanceof WallSkullBlock;
        renderState.direction = flag ? blockstate.getValue(WallSkullBlock.FACING) : null;
        int i = flag ? RotationSegment.convertToSegment(renderState.direction.getOpposite()) : blockstate.getValue(SkullBlock.ROTATION);
        renderState.rotationDegrees = RotationSegment.convertToDegrees(i);
        renderState.skullType = ((AbstractSkullBlock)blockstate.getBlock()).getType();
        renderState.renderType = this.resolveSkullRenderType(renderState.skullType, blockEntity);
    }

    public void submit(SkullBlockRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        SkullModelBase skullmodelbase = this.modelByType.apply(renderState.skullType);
        submitSkull(
            renderState.direction,
            renderState.rotationDegrees,
            renderState.animationProgress,
            poseStack,
            nodeCollector,
            renderState.lightCoords,
            skullmodelbase,
            renderState.renderType,
            0,
            renderState.breakProgress
        );
    }

    public static void submitSkull(
        @Nullable Direction direction,
        float yRot,
        float animationPos,
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        SkullModelBase model,
        RenderType renderType,
        int outlineColor,
        @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        poseStack.pushPose();
        if (direction == null) {
            poseStack.translate(0.5F, 0.0F, 0.5F);
        } else {
            float f = 0.25F;
            poseStack.translate(0.5F - direction.getStepX() * 0.25F, 0.25F, 0.5F - direction.getStepZ() * 0.25F);
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        SkullModelBase.State skullmodelbase$state = new SkullModelBase.State();
        skullmodelbase$state.animationPos = animationPos;
        skullmodelbase$state.yRot = yRot;
        nodeCollector.submitModel(model, skullmodelbase$state, poseStack, renderType, packedLight, OverlayTexture.NO_OVERLAY, outlineColor, crumblingOverlay);
        poseStack.popPose();
    }

    private RenderType resolveSkullRenderType(SkullBlock.Type type, SkullBlockEntity blockEntity) {
        if (type == SkullBlock.Types.PLAYER) {
            ResolvableProfile resolvableprofile = blockEntity.getOwnerProfile();
            if (resolvableprofile != null) {
                return this.playerSkinRenderCache.getOrDefault(resolvableprofile).renderType();
            }
        }

        return getSkullRenderType(type, null);
    }

    public static RenderType getSkullRenderType(SkullBlock.Type type, @Nullable ResourceLocation textureLocation) {
        return RenderType.entityCutoutNoCullZOffset(textureLocation != null ? textureLocation : SKIN_BY_TYPE.get(type));
    }

    public static RenderType getPlayerSkinRenderType(ResourceLocation textureLocation) {
        return RenderType.entityTranslucent(textureLocation);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(SkullBlockEntity blockEntity) {
        SkullBlock.Type type = ((AbstractSkullBlock) blockEntity.getBlockState().getBlock()).getType();
        if (type == SkullBlock.Types.DRAGON) {
            net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
            return new net.minecraft.world.phys.AABB(pos.getX() - .75, pos.getY() - .35, pos.getZ() - .75, pos.getX() + 1.75, pos.getY() + 1.0, pos.getZ() + 1.75);
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity);
    }
}
