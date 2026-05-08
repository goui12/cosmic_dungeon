package net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HitboxRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Team;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class LivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
    extends EntityRenderer<T, S>
    implements RenderLayerParent<S, M> {
    private static final float EYE_BED_OFFSET = 0.1F;
    protected M model;
    protected final ItemModelResolver itemModelResolver;
    protected final List<RenderLayer<S, M>> layers = Lists.newArrayList();

    public LivingEntityRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.model = model;
        this.shadowRadius = shadowRadius;
    }

    public final boolean addLayer(RenderLayer<S, M> layer) {
        return this.layers.add(layer);
    }

    @Override
    public M getModel() {
        return this.model;
    }

    protected AABB getBoundingBoxForCulling(T minecraft) {
        AABB aabb = super.getBoundingBoxForCulling(minecraft);
        if (minecraft.getItemBySlot(EquipmentSlot.HEAD).is(Items.DRAGON_HEAD)) {
            float f = 0.5F;
            return aabb.inflate(0.5, 0.5, 0.5);
        } else {
            return aabb;
        }
    }

    public void submit(S renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<T, S, M>(renderState, this, renderState.partialTick, poseStack, nodeCollector)).isCanceled()) return;
        poseStack.pushPose();
        if (renderState.hasPose(Pose.SLEEPING)) {
            Direction direction = renderState.bedOrientation;
            if (direction != null) {
                float f = renderState.eyeHeight - 0.1F;
                poseStack.translate(-direction.getStepX() * f, 0.0F, -direction.getStepZ() * f);
            }
        }

        float f1 = renderState.scale;
        poseStack.scale(f1, f1, f1);
        this.setupRotations(renderState, poseStack, renderState.bodyRot, f1);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        this.scale(renderState, poseStack);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        boolean flag1 = this.isBodyVisible(renderState);
        boolean flag = !flag1 && !renderState.isInvisibleToPlayer;
        RenderType rendertype = this.getRenderType(renderState, flag1, flag, renderState.appearsGlowing());
        if (rendertype != null) {
            int i = getOverlayCoords(renderState, this.getWhiteOverlayProgress(renderState));
            int j = flag ? 654311423 : -1;
            int k = ARGB.multiply(j, this.getModelTint(renderState));
            nodeCollector.submitModel(this.model, renderState, poseStack, rendertype, renderState.lightCoords, i, k, null, renderState.outlineColor, null);
        }

        if (this.shouldRenderLayers(renderState) && !this.layers.isEmpty()) {
            this.model.setupAnim(renderState);

            for (RenderLayer<S, M> renderlayer : this.layers) {
                renderlayer.submit(poseStack, nodeCollector, renderState.lightCoords, renderState, renderState.yRot, renderState.xRot);
            }
        }

        poseStack.popPose();
        super.submit(renderState, poseStack, nodeCollector, cameraRenderState);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.client.event.RenderLivingEvent.Post<T, S, M>(renderState, this, renderState.partialTick, poseStack, nodeCollector));
    }

    protected boolean shouldRenderLayers(S renderState) {
        return true;
    }

    protected int getModelTint(S renderState) {
        return -1;
    }

    public abstract ResourceLocation getTextureLocation(S renderState);

    @Nullable
    protected RenderType getRenderType(S renderState, boolean isVisible, boolean renderTranslucent, boolean appearsGlowing) {
        ResourceLocation resourcelocation = this.getTextureLocation(renderState);
        if (renderTranslucent) {
            return RenderType.itemEntityTranslucentCull(resourcelocation);
        } else if (isVisible) {
            return this.model.renderType(resourcelocation);
        } else {
            return appearsGlowing ? RenderType.outline(resourcelocation) : null;
        }
    }

    public static int getOverlayCoords(LivingEntityRenderState renderState, float overlay) {
        return OverlayTexture.pack(OverlayTexture.u(overlay), OverlayTexture.v(renderState.hasRedOverlay));
    }

    protected boolean isBodyVisible(S renderState) {
        return !renderState.isInvisible;
    }

    private static float sleepDirectionToRotation(Direction facing) {
        switch (facing) {
            case SOUTH:
                return 90.0F;
            case WEST:
                return 0.0F;
            case NORTH:
                return 270.0F;
            case EAST:
                return 180.0F;
            default:
                return 0.0F;
        }
    }

    protected boolean isShaking(S renderState) {
        return renderState.isFullyFrozen;
    }

    protected void setupRotations(S renderState, PoseStack poseStack, float bodyRot, float scale) {
        if (this.isShaking(renderState)) {
            bodyRot += (float)(Math.cos(Mth.floor(renderState.ageInTicks) * 3.25F) * Math.PI * 0.4F);
        }

        if (!renderState.hasPose(Pose.SLEEPING)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        }

        if (renderState.deathTime > 0.0F) {
            float f = (renderState.deathTime - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            poseStack.mulPose(Axis.ZP.rotationDegrees(f * this.getFlipDegrees()));
        } else if (renderState.isAutoSpinAttack) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - renderState.xRot));
            poseStack.mulPose(Axis.YP.rotationDegrees(renderState.ageInTicks * -75.0F));
        } else if (renderState.hasPose(Pose.SLEEPING)) {
            Direction direction = renderState.bedOrientation;
            float f1 = direction != null ? sleepDirectionToRotation(direction) : bodyRot;
            poseStack.mulPose(Axis.YP.rotationDegrees(f1));
            poseStack.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees()));
            poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
        } else if (renderState.isUpsideDown) {
            poseStack.translate(0.0F, (renderState.boundingBoxHeight + 0.1F) / scale, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
    }

    protected float getFlipDegrees() {
        return 90.0F;
    }

    protected float getWhiteOverlayProgress(S renderState) {
        return 0.0F;
    }

    protected void scale(S renderState, PoseStack poseStack) {
    }

    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        if (entity.isDiscrete()) {
            float f = 32.0F;
            if (!net.neoforged.neoforge.client.ClientHooks.isNameplateInRenderDistance(entity, distanceToCameraSq)) {
                return false;
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localplayer = minecraft.player;
        boolean flag = !entity.isInvisibleTo(localplayer);
        if (entity != localplayer) {
            Team team = entity.getTeam();
            Team team1 = localplayer.getTeam();
            if (team != null) {
                Team.Visibility team$visibility = team.getNameTagVisibility();
                switch (team$visibility) {
                    case ALWAYS:
                        return flag;
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return team1 == null ? flag : team.isAlliedTo(team1) && (team.canSeeFriendlyInvisibles() || flag);
                    case HIDE_FOR_OWN_TEAM:
                        return team1 == null ? flag : !team.isAlliedTo(team1) && flag;
                    default:
                        return true;
                }
            }
        }

        return Minecraft.renderNames() && entity != minecraft.getCameraEntity() && flag && !entity.isVehicle();
    }

    public boolean isEntityUpsideDown(T entity) {
        Component component = entity.getCustomName();
        return component != null && isUpsideDownName(component.getString());
    }

    protected static boolean isUpsideDownName(String name) {
        return "Dinnerbone".equals(name) || "Grumm".equals(name);
    }

    protected float getShadowRadius(S renderState) {
        return super.getShadowRadius(renderState) * renderState.scale;
    }

    public void extractRenderState(T entity, S reusedState, float partialTick) {
        super.extractRenderState(entity, reusedState, partialTick);
        float f = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        reusedState.bodyRot = solveBodyRot(entity, f, partialTick);
        reusedState.yRot = Mth.wrapDegrees(f - reusedState.bodyRot);
        reusedState.xRot = entity.getXRot(partialTick);
        reusedState.isUpsideDown = this.isEntityUpsideDown(entity);
        if (reusedState.isUpsideDown) {
            reusedState.xRot *= -1.0F;
            reusedState.yRot *= -1.0F;
        }

        if (!entity.isPassenger() && entity.isAlive()) {
            reusedState.walkAnimationPos = entity.walkAnimation.position(partialTick);
            reusedState.walkAnimationSpeed = entity.walkAnimation.speed(partialTick);
        } else {
            reusedState.walkAnimationPos = 0.0F;
            reusedState.walkAnimationSpeed = 0.0F;
        }

        if (entity.getVehicle() instanceof LivingEntity livingentity) {
            reusedState.wornHeadAnimationPos = livingentity.walkAnimation.position(partialTick);
        } else {
            reusedState.wornHeadAnimationPos = reusedState.walkAnimationPos;
        }

        reusedState.scale = entity.getScale();
        reusedState.ageScale = entity.getAgeScale();
        reusedState.pose = entity.getPose();
        reusedState.bedOrientation = entity.getBedOrientation();
        if (reusedState.bedOrientation != null) {
            reusedState.eyeHeight = entity.getEyeHeight(Pose.STANDING);
        }

        reusedState.isFullyFrozen = entity.isFullyFrozen();
        reusedState.isBaby = entity.isBaby();
        reusedState.isInWater = entity.isInWater() || entity.isInFluidType((fluidType, height) -> entity.canSwimInFluidType(fluidType));
        reusedState.isAutoSpinAttack = entity.isAutoSpinAttack();
        reusedState.hasRedOverlay = entity.hurtTime > 0 || entity.deathTime > 0;
        ItemStack itemstack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (itemstack.getItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof AbstractSkullBlock abstractskullblock) {
            reusedState.wornHeadType = abstractskullblock.getType();
            reusedState.wornHeadProfile = itemstack.get(DataComponents.PROFILE);
            reusedState.headItem.clear();
        } else {
            reusedState.wornHeadType = null;
            reusedState.wornHeadProfile = null;
            if (!HumanoidArmorLayer.shouldRender(itemstack, EquipmentSlot.HEAD)) {
                this.itemModelResolver.updateForLiving(reusedState.headItem, itemstack, ItemDisplayContext.HEAD, entity);
            } else {
                reusedState.headItem.clear();
            }
        }

        reusedState.deathTime = entity.deathTime > 0 ? entity.deathTime + partialTick : 0.0F;
        Minecraft minecraft = Minecraft.getInstance();
        reusedState.isInvisibleToPlayer = reusedState.isInvisible && entity.isInvisibleTo(minecraft.player);
    }

    protected void extractAdditionalHitboxes(T entity, Builder<HitboxRenderState> hitboxes, float partialTick) {
        AABB aabb = entity.getBoundingBox();
        float f = 0.01F;
        HitboxRenderState hitboxrenderstate = new HitboxRenderState(
            aabb.minX - entity.getX(),
            entity.getEyeHeight() - 0.01F,
            aabb.minZ - entity.getZ(),
            aabb.maxX - entity.getX(),
            entity.getEyeHeight() + 0.01F,
            aabb.maxZ - entity.getZ(),
            1.0F,
            0.0F,
            0.0F
        );
        hitboxes.add(hitboxrenderstate);
    }

    private static float solveBodyRot(LivingEntity entity, float yHeadRot, float partialTick) {
        if (entity.getVehicle() instanceof LivingEntity livingentity) {
            float f2 = Mth.rotLerp(partialTick, livingentity.yBodyRotO, livingentity.yBodyRot);
            float f = 85.0F;
            float f1 = Mth.clamp(Mth.wrapDegrees(yHeadRot - f2), -85.0F, 85.0F);
            f2 = yHeadRot - f1;
            if (Math.abs(f1) > 50.0F) {
                f2 += f1 * 0.2F;
            }

            return f2;
        } else {
            return Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        }
    }
}
