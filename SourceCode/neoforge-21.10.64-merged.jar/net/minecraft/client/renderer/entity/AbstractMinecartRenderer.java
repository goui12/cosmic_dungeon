package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Objects;
import net.minecraft.client.model.MinecartModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.OldMinecartBehavior;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractMinecartRenderer<T extends AbstractMinecart, S extends MinecartRenderState> extends EntityRenderer<T, S> {
    private static final ResourceLocation MINECART_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/minecart.png");
    private static final float DISPLAY_BLOCK_SCALE = 0.75F;
    protected final MinecartModel model;

    public AbstractMinecartRenderer(EntityRendererProvider.Context context, ModelLayerLocation modelLayer) {
        super(context);
        this.shadowRadius = 0.7F;
        this.model = new MinecartModel(context.bakeLayer(modelLayer));
    }

    public void submit(S renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        poseStack.pushPose();
        long i = renderState.offsetSeed;
        float f = (((float)(i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f1 = (((float)(i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f2 = (((float)(i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        poseStack.translate(f, f1, f2);
        if (renderState.isNewRender) {
            newRender(renderState, poseStack);
        } else {
            oldRender(renderState, poseStack);
        }

        float f3 = renderState.hurtTime;
        if (f3 > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(f3) * f3 * renderState.damageTime / 10.0F * renderState.hurtDir));
        }

        BlockState blockstate = renderState.displayBlockState;
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            poseStack.pushPose();
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.translate(-0.5F, (renderState.displayOffset - 8) / 16.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            this.submitMinecartContents(renderState, blockstate, poseStack, nodeCollector, renderState.lightCoords);
            poseStack.popPose();
        }

        poseStack.scale(-1.0F, -1.0F, 1.0F);
        nodeCollector.submitModel(
            this.model,
            renderState,
            poseStack,
            this.model.renderType(MINECART_LOCATION),
            renderState.lightCoords,
            OverlayTexture.NO_OVERLAY,
            renderState.outlineColor,
            null
        );
        poseStack.popPose();
    }

    private static <S extends MinecartRenderState> void newRender(S renderState, PoseStack poseStack) {
        poseStack.mulPose(Axis.YP.rotationDegrees(renderState.yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-renderState.xRot));
        poseStack.translate(0.0F, 0.375F, 0.0F);
    }

    private static <S extends MinecartRenderState> void oldRender(S renderState, PoseStack poseStack) {
        double d0 = renderState.x;
        double d1 = renderState.y;
        double d2 = renderState.z;
        float f = renderState.xRot;
        float f1 = renderState.yRot;
        if (renderState.posOnRail != null && renderState.frontPos != null && renderState.backPos != null) {
            Vec3 vec3 = renderState.frontPos;
            Vec3 vec31 = renderState.backPos;
            poseStack.translate(renderState.posOnRail.x - d0, (vec3.y + vec31.y) / 2.0 - d1, renderState.posOnRail.z - d2);
            Vec3 vec32 = vec31.add(-vec3.x, -vec3.y, -vec3.z);
            if (vec32.length() != 0.0) {
                vec32 = vec32.normalize();
                f1 = (float)(Math.atan2(vec32.z, vec32.x) * 180.0 / Math.PI);
                f = (float)(Math.atan(vec32.y) * 73.0);
            }
        }

        poseStack.translate(0.0F, 0.375F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - f1));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-f));
    }

    public void extractRenderState(T entity, S reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        if (entity.getBehavior() instanceof NewMinecartBehavior newminecartbehavior) {
            newExtractState(entity, newminecartbehavior, reusedState, partialTick);
            reusedState.isNewRender = true;
        } else if (entity.getBehavior() instanceof OldMinecartBehavior oldminecartbehavior) {
            oldExtractState(entity, oldminecartbehavior, reusedState, partialTick);
            reusedState.isNewRender = false;
        }

        long i = entity.getId() * 493286711L;
        reusedState.offsetSeed = i * i * 4392167121L + i * 98761L;
        reusedState.hurtTime = entity.getHurtTime() - partialTick;
        reusedState.hurtDir = entity.getHurtDir();
        reusedState.damageTime = Math.max(entity.getDamage() - partialTick, 0.0F);
        reusedState.displayOffset = entity.getDisplayOffset();
        reusedState.displayBlockState = entity.getDisplayBlockState();
    }

    private static <T extends AbstractMinecart, S extends MinecartRenderState> void newExtractState(
        T minecart, NewMinecartBehavior behavior, S renderState, float partialTick
    ) {
        if (behavior.cartHasPosRotLerp()) {
            renderState.renderPos = behavior.getCartLerpPosition(partialTick);
            renderState.xRot = behavior.getCartLerpXRot(partialTick);
            renderState.yRot = behavior.getCartLerpYRot(partialTick);
        } else {
            renderState.renderPos = null;
            renderState.xRot = minecart.getXRot();
            renderState.yRot = minecart.getYRot();
        }
    }

    private static <T extends AbstractMinecart, S extends MinecartRenderState> void oldExtractState(
        T minecart, OldMinecartBehavior behavior, S renderState, float partialTick
    ) {
        float f = 0.3F;
        renderState.xRot = minecart.getXRot(partialTick);
        renderState.yRot = minecart.getYRot(partialTick);
        double d0 = renderState.x;
        double d1 = renderState.y;
        double d2 = renderState.z;
        Vec3 vec3 = behavior.getPos(d0, d1, d2);
        if (vec3 != null) {
            renderState.posOnRail = vec3;
            Vec3 vec31 = behavior.getPosOffs(d0, d1, d2, 0.3F);
            Vec3 vec32 = behavior.getPosOffs(d0, d1, d2, -0.3F);
            renderState.frontPos = Objects.requireNonNullElse(vec31, vec3);
            renderState.backPos = Objects.requireNonNullElse(vec32, vec3);
        } else {
            renderState.posOnRail = null;
            renderState.frontPos = null;
            renderState.backPos = null;
        }
    }

    protected void submitMinecartContents(S renderState, BlockState blockState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight) {
        nodeCollector.submitBlock(poseStack, blockState, packedLight, OverlayTexture.NO_OVERLAY, renderState.outlineColor);
    }

    protected AABB getBoundingBoxForCulling(T minecraft) {
        AABB aabb = super.getBoundingBoxForCulling(minecraft);
        return !minecraft.getDisplayBlockState().isAir() ? aabb.expandTowards(0.0, minecraft.getDisplayOffset() * 0.75F / 16.0F, 0.0) : aabb;
    }

    public Vec3 getRenderOffset(S renderState) {
        Vec3 vec3 = super.getRenderOffset(renderState);
        return renderState.isNewRender && renderState.renderPos != null
            ? vec3.add(renderState.renderPos.x - renderState.x, renderState.renderPos.y - renderState.y, renderState.renderPos.z - renderState.z)
            : vec3;
    }
}
