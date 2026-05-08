package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemFrameRenderer<T extends ItemFrame> extends EntityRenderer<T, ItemFrameRenderState> {
    public static final int GLOW_FRAME_BRIGHTNESS = 5;
    public static final int BRIGHT_MAP_LIGHT_ADJUSTMENT = 30;
    private final ItemModelResolver itemModelResolver;
    private final MapRenderer mapRenderer;
    private final BlockRenderDispatcher blockRenderer;

    public ItemFrameRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.mapRenderer = context.getMapRenderer();
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return entity.getType() == EntityType.GLOW_ITEM_FRAME
            ? Math.max(5, super.getBlockLightLevel(entity, pos))
            : super.getBlockLightLevel(entity, pos);
    }

    public void submit(ItemFrameRenderState renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        poseStack.pushPose();
        Direction direction = renderState.direction;
        Vec3 vec3 = this.getRenderOffset(renderState);
        poseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
        double d0 = 0.46875;
        poseStack.translate(direction.getStepX() * 0.46875, direction.getStepY() * 0.46875, direction.getStepZ() * 0.46875);
        float f;
        float f1;
        if (direction.getAxis().isHorizontal()) {
            f = 0.0F;
            f1 = 180.0F - direction.toYRot();
        } else {
            f = -90 * direction.getAxisDirection().getStep();
            f1 = 180.0F;
        }

        poseStack.mulPose(Axis.XP.rotationDegrees(f));
        poseStack.mulPose(Axis.YP.rotationDegrees(f1));
        if (!renderState.isInvisible) {
            BlockState blockstate = BlockStateDefinitions.getItemFrameFakeState(renderState.isGlowFrame, renderState.mapId != null);
            BlockStateModel blockstatemodel = this.blockRenderer.getBlockModel(blockstate);
            poseStack.pushPose();
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            nodeCollector.submitBlockModel(
                poseStack,
                RenderType.entitySolidZOffsetForward(TextureAtlas.LOCATION_BLOCKS),
                blockstatemodel,
                1.0F,
                1.0F,
                1.0F,
                renderState.lightCoords,
                OverlayTexture.NO_OVERLAY,
                renderState.outlineColor
            );
            poseStack.popPose();
        }

        if (renderState.isInvisible) {
            poseStack.translate(0.0F, 0.0F, 0.5F);
        } else {
            poseStack.translate(0.0F, 0.0F, 0.4375F);
        }

        if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderItemInFrameEvent(renderState, this, poseStack, nodeCollector)).isCanceled()) {
        if (renderState.mapId != null) {
            int j = renderState.rotation % 4 * 2;
            poseStack.mulPose(Axis.ZP.rotationDegrees(j * 360.0F / 8.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float f2 = 0.0078125F;
            poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
            poseStack.translate(-64.0F, -64.0F, 0.0F);
            poseStack.translate(0.0F, 0.0F, -1.0F);
            int i = this.getLightCoords(renderState.isGlowFrame, 15728850, renderState.lightCoords);
            this.mapRenderer.render(renderState.mapRenderState, poseStack, nodeCollector, true, i);
        } else if (!renderState.item.isEmpty()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(renderState.rotation * 360.0F / 8.0F));
            int k = this.getLightCoords(renderState.isGlowFrame, 15728880, renderState.lightCoords);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            renderState.item.submit(poseStack, nodeCollector, k, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
        }
        }

        poseStack.popPose();
    }

    private int getLightCoords(boolean isGlowFrame, int glowLight, int normalLight) {
        return isGlowFrame ? glowLight : normalLight;
    }

    public Vec3 getRenderOffset(ItemFrameRenderState renderState) {
        return new Vec3(renderState.direction.getStepX() * 0.3F, -0.25, renderState.direction.getStepZ() * 0.3F);
    }

    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        return Minecraft.renderNames() && this.entityRenderDispatcher.crosshairPickEntity == entity && entity.getItem().getCustomName() != null;
    }

    protected Component getNameTag(T entity) {
        return entity.getItem().getHoverName();
    }

    public ItemFrameRenderState createRenderState() {
        return new ItemFrameRenderState();
    }

    public void extractRenderState(T entity, ItemFrameRenderState reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        reusedState.direction = entity.getDirection();
        ItemStack itemstack = entity.getItem();
        this.itemModelResolver.updateForNonLiving(reusedState.item, itemstack, ItemDisplayContext.FIXED, entity);
        reusedState.rotation = entity.getRotation();
        reusedState.isGlowFrame = entity.getType() == EntityType.GLOW_ITEM_FRAME;
        reusedState.mapId = null;
        if (!itemstack.isEmpty()) {
            MapId mapid = entity.getFramedMapId(itemstack);
            if (mapid != null) {
                MapItemSavedData mapitemsaveddata = net.minecraft.world.item.MapItem.getSavedData(itemstack, entity.level());
                if (mapitemsaveddata != null) {
                    this.mapRenderer.extractRenderState(mapid, mapitemsaveddata, reusedState.mapRenderState);
                    reusedState.mapId = mapid;
                }
            }
        }
    }
}
