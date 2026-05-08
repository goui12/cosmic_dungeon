package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HitboxRenderState;
import net.minecraft.client.renderer.entity.state.HitboxesRenderState;
import net.minecraft.client.renderer.entity.state.ServerHitboxesRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class EntityRenderer<T extends Entity, S extends EntityRenderState> {
    private static final float SHADOW_POWER_FALLOFF_Y = 0.5F;
    private static final float MAX_SHADOW_RADIUS = 32.0F;
    public static final float NAMETAG_SCALE = 0.025F;
    protected final EntityRenderDispatcher entityRenderDispatcher;
    private final Font font;
    protected float shadowRadius;
    protected float shadowStrength = 1.0F;

    protected EntityRenderer(EntityRendererProvider.Context context) {
        this.entityRenderDispatcher = context.getEntityRenderDispatcher();
        this.font = context.getFont();
    }

    public final int getPackedLightCoords(T entity, float partialTicks) {
        BlockPos blockpos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
        return LightTexture.pack(this.getBlockLightLevel(entity, blockpos), this.getSkyLightLevel(entity, blockpos));
    }

    protected int getSkyLightLevel(T entity, BlockPos pos) {
        return entity.level().getBrightness(LightLayer.SKY, pos);
    }

    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, pos);
    }

    public boolean shouldRender(T livingEntity, Frustum camera, double camX, double camY, double camZ) {
        if (!livingEntity.shouldRender(camX, camY, camZ)) {
            return false;
        } else if (!this.affectedByCulling(livingEntity)) {
            return true;
        } else {
            AABB aabb = this.getBoundingBoxForCulling(livingEntity).inflate(0.5);
            if (aabb.hasNaN() || aabb.getSize() == 0.0) {
                aabb = new AABB(
                    livingEntity.getX() - 2.0,
                    livingEntity.getY() - 2.0,
                    livingEntity.getZ() - 2.0,
                    livingEntity.getX() + 2.0,
                    livingEntity.getY() + 2.0,
                    livingEntity.getZ() + 2.0
                );
            }

            if (camera.isVisible(aabb)) {
                return true;
            } else {
                if (livingEntity instanceof Leashable leashable) {
                    Entity entity = leashable.getLeashHolder();
                    if (entity != null) {
                        AABB aabb1 = this.entityRenderDispatcher.getRenderer(entity).getBoundingBoxForCulling(entity);
                        return camera.isVisible(aabb1) || camera.isVisible(aabb.minmax(aabb1));
                    }
                }

                return false;
            }
        }
    }

    protected AABB getBoundingBoxForCulling(T minecraft) {
        return minecraft.getBoundingBox();
    }

    protected boolean affectedByCulling(T display) {
        return true;
    }

    public Vec3 getRenderOffset(S renderState) {
        return renderState.passengerOffset != null ? renderState.passengerOffset : Vec3.ZERO;
    }

    public void submit(S renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.leashStates != null) {
            for (EntityRenderState.LeashState entityrenderstate$leashstate : renderState.leashStates) {
                nodeCollector.submitLeash(poseStack, entityrenderstate$leashstate);
            }
        }

        this.submitNameTag(renderState, poseStack, nodeCollector, cameraRenderState);
    }

    protected boolean shouldShowName(T entity, double distanceToCameraSq) {
        return entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity;
    }

    public Font getFont() {
        return this.font;
    }

    protected void submitNameTag(S renderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        if (renderState.nameTag != null) {
            var event = new net.neoforged.neoforge.client.event.RenderNameTagEvent.DoRender(renderState, renderState.nameTag, this, poseStack, nodeCollector, cameraRenderState, renderState.partialTick);
            if (!net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled())
            nodeCollector.submitNameTag(
                poseStack,
                renderState.nameTagAttachment,
                0,
                renderState.nameTag,
                !renderState.isDiscrete,
                renderState.lightCoords,
                renderState.distanceToCameraSq,
                cameraRenderState
            );
        }
    }

    @Nullable
    protected Component getNameTag(T entity) {
        return entity.getDisplayName();
    }

    protected float getShadowRadius(S renderState) {
        return this.shadowRadius;
    }

    protected float getShadowStrength(S renderState) {
        return this.shadowStrength;
    }

    public abstract S createRenderState();

    public final S createRenderState(T entity, float partialTick) {
        S s = this.createRenderState();
        this.extractRenderState(entity, s, partialTick);
        this.finalizeRenderState(entity, s);
        net.neoforged.neoforge.client.renderstate.RenderStateExtensions.onUpdateEntityRenderState(this, entity, s);
        return s;
    }

    public void extractRenderState(T entity, S reusedState, float partialTick) {
        reusedState.entityType = entity.getType();
        reusedState.x = Mth.lerp((double)partialTick, entity.xOld, entity.getX());
        reusedState.y = Mth.lerp((double)partialTick, entity.yOld, entity.getY());
        reusedState.z = Mth.lerp((double)partialTick, entity.zOld, entity.getZ());
        reusedState.isInvisible = entity.isInvisible();
        reusedState.partialTick = partialTick;
        reusedState.ageInTicks = entity.tickCount + partialTick;
        reusedState.boundingBoxWidth = entity.getBbWidth();
        reusedState.boundingBoxHeight = entity.getBbHeight();
        reusedState.eyeHeight = entity.getEyeHeight();
        if (entity.isPassenger()
            && entity.getVehicle() instanceof AbstractMinecart abstractminecart
            && abstractminecart.getBehavior() instanceof NewMinecartBehavior newminecartbehavior
            && newminecartbehavior.cartHasPosRotLerp()) {
            double d2 = Mth.lerp((double)partialTick, abstractminecart.xOld, abstractminecart.getX());
            double d0 = Mth.lerp((double)partialTick, abstractminecart.yOld, abstractminecart.getY());
            double d1 = Mth.lerp((double)partialTick, abstractminecart.zOld, abstractminecart.getZ());
            reusedState.passengerOffset = newminecartbehavior.getCartLerpPosition(partialTick).subtract(new Vec3(d2, d0, d1));
        } else {
            reusedState.passengerOffset = null;
        }

        if (this.entityRenderDispatcher.camera != null) {
            reusedState.distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr(entity);
            var event = new net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender(entity, reusedState, this.getNameTag(entity), this, partialTick);
            boolean flag1 = event.canRender().isTrue() || (event.canRender().isDefault() && reusedState.distanceToCameraSq < 4096.0 && this.shouldShowName(entity, reusedState.distanceToCameraSq));
            if (flag1) {
                reusedState.nameTag = event.getContent();
                reusedState.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));
            } else {
                reusedState.nameTag = null;
            }
        }

        label83: {
            reusedState.isDiscrete = entity.isDiscrete();
            Level level = entity.level();
            if (entity instanceof Leashable leashable) {
                Entity $$12 = leashable.getLeashHolder();
                if ($$12 instanceof Entity) {
                    float f = entity.getPreciseBodyRotation(partialTick) * (float) (Math.PI / 180.0);
                    Vec3 vec31 = leashable.getLeashOffset(partialTick);
                    BlockPos blockpos = BlockPos.containing(entity.getEyePosition(partialTick));
                    BlockPos blockpos1 = BlockPos.containing($$12.getEyePosition(partialTick));
                    int i = this.getBlockLightLevel(entity, blockpos);
                    int j = this.entityRenderDispatcher.getRenderer($$12).getBlockLightLevel($$12, blockpos1);
                    int k = level.getBrightness(LightLayer.SKY, blockpos);
                    int l = level.getBrightness(LightLayer.SKY, blockpos1);
                    boolean flag = $$12.supportQuadLeashAsHolder() && leashable.supportQuadLeash();
                    int i1 = flag ? 4 : 1;
                    if (reusedState.leashStates == null || reusedState.leashStates.size() != i1) {
                        reusedState.leashStates = new ArrayList<>(i1);

                        for (int j1 = 0; j1 < i1; j1++) {
                            reusedState.leashStates.add(new EntityRenderState.LeashState());
                        }
                    }

                    if (flag) {
                        float f1 = $$12.getPreciseBodyRotation(partialTick) * (float) (Math.PI / 180.0);
                        Vec3 vec3 = $$12.getPosition(partialTick);
                        Vec3[] avec3 = leashable.getQuadLeashOffsets();
                        Vec3[] avec31 = $$12.getQuadLeashHolderOffsets();
                        int k1 = 0;

                        while (true) {
                            if (k1 >= i1) {
                                break label83;
                            }

                            EntityRenderState.LeashState entityrenderstate$leashstate = reusedState.leashStates.get(k1);
                            entityrenderstate$leashstate.offset = avec3[k1].yRot(-f);
                            entityrenderstate$leashstate.start = entity.getPosition(partialTick).add(entityrenderstate$leashstate.offset);
                            entityrenderstate$leashstate.end = vec3.add(avec31[k1].yRot(-f1));
                            entityrenderstate$leashstate.startBlockLight = i;
                            entityrenderstate$leashstate.endBlockLight = j;
                            entityrenderstate$leashstate.startSkyLight = k;
                            entityrenderstate$leashstate.endSkyLight = l;
                            entityrenderstate$leashstate.slack = false;
                            k1++;
                        }
                    } else {
                        Vec3 vec32 = vec31.yRot(-f);
                        EntityRenderState.LeashState entityrenderstate$leashstate1 = reusedState.leashStates.getFirst();
                        entityrenderstate$leashstate1.offset = vec32;
                        entityrenderstate$leashstate1.start = entity.getPosition(partialTick).add(vec32);
                        entityrenderstate$leashstate1.end = $$12.getRopeHoldPosition(partialTick);
                        entityrenderstate$leashstate1.startBlockLight = i;
                        entityrenderstate$leashstate1.endBlockLight = j;
                        entityrenderstate$leashstate1.startSkyLight = k;
                        entityrenderstate$leashstate1.endSkyLight = l;
                        break label83;
                    }
                }
            }

            reusedState.leashStates = null;
        }

        reusedState.displayFireAnimation = entity.displayFireAnimation();
        Minecraft minecraft = Minecraft.getInstance();
        boolean flag2 = minecraft.shouldEntityAppearGlowing(entity);
        reusedState.outlineColor = flag2 ? ARGB.opaque(entity.getTeamColor()) : 0;
        if (minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES) && !reusedState.isInvisible && !minecraft.showOnlyReducedInfo()) {
            this.extractHitboxes(entity, reusedState, partialTick);
        } else {
            reusedState.hitboxesRenderState = null;
            reusedState.serverHitboxesRenderState = null;
        }

        reusedState.lightCoords = this.getPackedLightCoords(entity, partialTick);
    }

    protected void finalizeRenderState(T entity, S renderState) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = entity.level();
        this.extractShadow(renderState, minecraft, level);
    }

    private void extractShadow(S renderState, Minecraft minecraft, Level level) {
        renderState.shadowPieces.clear();
        if (minecraft.options.entityShadows().get() && !renderState.isInvisible) {
            float f = Math.min(this.getShadowRadius(renderState), 32.0F);
            renderState.shadowRadius = f;
            if (f > 0.0F) {
                double d0 = renderState.distanceToCameraSq;
                float f1 = (float)((1.0 - d0 / 256.0) * this.getShadowStrength(renderState));
                if (f1 > 0.0F) {
                    int i = Mth.floor(renderState.x - f);
                    int j = Mth.floor(renderState.x + f);
                    int k = Mth.floor(renderState.z - f);
                    int l = Mth.floor(renderState.z + f);
                    float f2 = Math.min(f1 / 0.5F - 1.0F, f);
                    int i1 = Mth.floor(renderState.y - f2);
                    int j1 = Mth.floor(renderState.y);
                    BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                    for (int k1 = k; k1 <= l; k1++) {
                        for (int l1 = i; l1 <= j; l1++) {
                            blockpos$mutableblockpos.set(l1, 0, k1);
                            ChunkAccess chunkaccess = level.getChunk(blockpos$mutableblockpos);

                            for (int i2 = i1; i2 <= j1; i2++) {
                                blockpos$mutableblockpos.setY(i2);
                                this.extractShadowPiece(renderState, level, f1, blockpos$mutableblockpos, chunkaccess);
                            }
                        }
                    }
                }
            }
        } else {
            renderState.shadowRadius = 0.0F;
        }
    }

    private void extractShadowPiece(S renderState, Level level, float shadowStrength, BlockPos.MutableBlockPos pos, ChunkAccess chunk) {
        float f = shadowStrength - (float)(renderState.y - pos.getY()) * 0.5F;
        BlockPos blockpos = pos.below();
        BlockState blockstate = chunk.getBlockState(blockpos);
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            int i = level.getMaxLocalRawBrightness(pos);
            if (i > 3) {
                if (blockstate.isCollisionShapeFullBlock(chunk, blockpos)) {
                    VoxelShape voxelshape = blockstate.getShape(chunk, blockpos);
                    if (!voxelshape.isEmpty()) {
                        float f1 = Mth.clamp(f * 0.5F * LightTexture.getBrightness(level.dimensionType(), i), 0.0F, 1.0F);
                        float f2 = (float)(pos.getX() - renderState.x);
                        float f3 = (float)(pos.getY() - renderState.y);
                        float f4 = (float)(pos.getZ() - renderState.z);
                        renderState.shadowPieces.add(new EntityRenderState.ShadowPiece(f2, f3, f4, voxelshape, f1));
                    }
                }
            }
        }
    }

    private void extractHitboxes(T p_entity, S reusedState, float partialTick) {
        reusedState.hitboxesRenderState = this.extractHitboxes(p_entity, partialTick, false);
        if (SharedConstants.DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES) {
            Entity entity = getServerSideEntity(p_entity);
            if (entity != null) {
                Vec3 vec3 = entity.getDeltaMovement();
                reusedState.serverHitboxesRenderState = new ServerHitboxesRenderState(
                    false,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    vec3.x,
                    vec3.y,
                    vec3.z,
                    entity.getEyeHeight(),
                    this.extractHitboxes((T)entity, 1.0F, true)
                );
            } else {
                reusedState.serverHitboxesRenderState = new ServerHitboxesRenderState(true);
            }
        } else {
            reusedState.serverHitboxesRenderState = null;
        }
    }

    private HitboxesRenderState extractHitboxes(T p_entity, float partialTick, boolean green) {
        Builder<HitboxRenderState> builder = new Builder<>();
        AABB aabb = p_entity.getBoundingBox();
        HitboxRenderState hitboxrenderstate;
        if (green) {
            hitboxrenderstate = new HitboxRenderState(
                aabb.minX - p_entity.getX(),
                aabb.minY - p_entity.getY(),
                aabb.minZ - p_entity.getZ(),
                aabb.maxX - p_entity.getX(),
                aabb.maxY - p_entity.getY(),
                aabb.maxZ - p_entity.getZ(),
                0.0F,
                1.0F,
                0.0F
            );
        } else {
            hitboxrenderstate = new HitboxRenderState(
                aabb.minX - p_entity.getX(),
                aabb.minY - p_entity.getY(),
                aabb.minZ - p_entity.getZ(),
                aabb.maxX - p_entity.getX(),
                aabb.maxY - p_entity.getY(),
                aabb.maxZ - p_entity.getZ(),
                1.0F,
                1.0F,
                1.0F
            );
        }

        builder.add(hitboxrenderstate);
        Entity entity = p_entity.getVehicle();
        if (entity != null) {
            float f = Math.min(entity.getBbWidth(), p_entity.getBbWidth()) / 2.0F;
            float f1 = 0.0625F;
            Vec3 vec3 = entity.getPassengerRidingPosition(p_entity).subtract(p_entity.position());
            HitboxRenderState hitboxrenderstate1 = new HitboxRenderState(
                vec3.x - f, vec3.y, vec3.z - f, vec3.x + f, vec3.y + 0.0625, vec3.z + f, 1.0F, 1.0F, 0.0F
            );
            builder.add(hitboxrenderstate1);
        }

        this.extractAdditionalHitboxes(p_entity, builder, partialTick);
        Vec3 vec31 = p_entity.getViewVector(partialTick);
        return new HitboxesRenderState(vec31.x, vec31.y, vec31.z, builder.build());
    }

    protected void extractAdditionalHitboxes(T entity, Builder<HitboxRenderState> hitboxes, float partialTick) {
    }

    @Nullable
    private static Entity getServerSideEntity(Entity entity) {
        IntegratedServer integratedserver = Minecraft.getInstance().getSingleplayerServer();
        if (integratedserver != null) {
            ServerLevel serverlevel = integratedserver.getLevel(entity.level().dimension());
            if (serverlevel != null) {
                return serverlevel.getEntity(entity.getId());
            }
        }

        return null;
    }
}
