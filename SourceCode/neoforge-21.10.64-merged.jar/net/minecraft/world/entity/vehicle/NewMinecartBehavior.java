package net.minecraft.world.entity.vehicle;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.ByteBuf;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NewMinecartBehavior extends MinecartBehavior {
    public static final int POS_ROT_LERP_TICKS = 3;
    public static final double ON_RAIL_Y_OFFSET = 0.1;
    public static final double OPPOSING_SLOPES_REST_AT_SPEED_THRESHOLD = 0.005;
    @Nullable
    private NewMinecartBehavior.StepPartialTicks cacheIndexAlpha;
    private int cachedLerpDelay;
    private float cachedPartialTick;
    private int lerpDelay = 0;
    public final List<NewMinecartBehavior.MinecartStep> lerpSteps = new LinkedList<>();
    public final List<NewMinecartBehavior.MinecartStep> currentLerpSteps = new LinkedList<>();
    public double currentLerpStepsTotalWeight = 0.0;
    public NewMinecartBehavior.MinecartStep oldLerp = NewMinecartBehavior.MinecartStep.ZERO;

    public NewMinecartBehavior(AbstractMinecart minecart) {
        super(minecart);
    }

    @Override
    public void tick() {
        if (this.level() instanceof ServerLevel serverlevel) {
            BlockPos blockpos = this.minecart.getCurrentBlockPosOrRailBelow();
            BlockState $$4 = this.level().getBlockState(blockpos);
            if (this.minecart.isFirstTick()) {
                this.minecart.setOnRails(BaseRailBlock.isRail($$4));
                this.adjustToRails(blockpos, $$4, true);
            }

            this.minecart.applyGravity();
            this.minecart.moveAlongTrack(serverlevel);
        } else {
            this.lerpClientPositionAndRotation();
            boolean flag = BaseRailBlock.isRail(this.level().getBlockState(this.minecart.getCurrentBlockPosOrRailBelow()));
            this.minecart.setOnRails(flag);
        }
    }

    private void lerpClientPositionAndRotation() {
        if (--this.lerpDelay <= 0) {
            this.setOldLerpValues();
            this.currentLerpSteps.clear();
            if (!this.lerpSteps.isEmpty()) {
                this.currentLerpSteps.addAll(this.lerpSteps);
                this.lerpSteps.clear();
                this.currentLerpStepsTotalWeight = 0.0;

                for (NewMinecartBehavior.MinecartStep newminecartbehavior$minecartstep : this.currentLerpSteps) {
                    this.currentLerpStepsTotalWeight = this.currentLerpStepsTotalWeight + newminecartbehavior$minecartstep.weight;
                }

                this.lerpDelay = this.currentLerpStepsTotalWeight == 0.0 ? 0 : 3;
            }
        }

        if (this.cartHasPosRotLerp()) {
            this.setPos(this.getCartLerpPosition(1.0F));
            this.setDeltaMovement(this.getCartLerpMovements(1.0F));
            this.setXRot(this.getCartLerpXRot(1.0F));
            this.setYRot(this.getCartLerpYRot(1.0F));
        }
    }

    public void setOldLerpValues() {
        this.oldLerp = new NewMinecartBehavior.MinecartStep(this.position(), this.getDeltaMovement(), this.getYRot(), this.getXRot(), 0.0F);
    }

    public boolean cartHasPosRotLerp() {
        return !this.currentLerpSteps.isEmpty();
    }

    public float getCartLerpXRot(float partialTick) {
        NewMinecartBehavior.StepPartialTicks newminecartbehavior$steppartialticks = this.getCurrentLerpStep(partialTick);
        return Mth.rotLerp(
            newminecartbehavior$steppartialticks.partialTicksInStep,
            newminecartbehavior$steppartialticks.previousStep.xRot,
            newminecartbehavior$steppartialticks.currentStep.xRot
        );
    }

    public float getCartLerpYRot(float partialTick) {
        NewMinecartBehavior.StepPartialTicks newminecartbehavior$steppartialticks = this.getCurrentLerpStep(partialTick);
        return Mth.rotLerp(
            newminecartbehavior$steppartialticks.partialTicksInStep,
            newminecartbehavior$steppartialticks.previousStep.yRot,
            newminecartbehavior$steppartialticks.currentStep.yRot
        );
    }

    public Vec3 getCartLerpPosition(float partialTick) {
        NewMinecartBehavior.StepPartialTicks newminecartbehavior$steppartialticks = this.getCurrentLerpStep(partialTick);
        return Mth.lerp(
            newminecartbehavior$steppartialticks.partialTicksInStep,
            newminecartbehavior$steppartialticks.previousStep.position,
            newminecartbehavior$steppartialticks.currentStep.position
        );
    }

    public Vec3 getCartLerpMovements(float partialTick) {
        NewMinecartBehavior.StepPartialTicks newminecartbehavior$steppartialticks = this.getCurrentLerpStep(partialTick);
        return Mth.lerp(
            newminecartbehavior$steppartialticks.partialTicksInStep,
            newminecartbehavior$steppartialticks.previousStep.movement,
            newminecartbehavior$steppartialticks.currentStep.movement
        );
    }

    private NewMinecartBehavior.StepPartialTicks getCurrentLerpStep(float partialTick) {
        if (partialTick == this.cachedPartialTick && this.lerpDelay == this.cachedLerpDelay && this.cacheIndexAlpha != null) {
            return this.cacheIndexAlpha;
        } else {
            float f = (3 - this.lerpDelay + partialTick) / 3.0F;
            float f1 = 0.0F;
            float f2 = 1.0F;
            boolean flag = false;

            int i;
            for (i = 0; i < this.currentLerpSteps.size(); i++) {
                float f3 = this.currentLerpSteps.get(i).weight;
                if (!(f3 <= 0.0F)) {
                    f1 += f3;
                    if (f1 >= this.currentLerpStepsTotalWeight * f) {
                        float f4 = f1 - f3;
                        f2 = (float)((f * this.currentLerpStepsTotalWeight - f4) / f3);
                        flag = true;
                        break;
                    }
                }
            }

            if (!flag) {
                i = this.currentLerpSteps.size() - 1;
            }

            NewMinecartBehavior.MinecartStep newminecartbehavior$minecartstep = this.currentLerpSteps.get(i);
            NewMinecartBehavior.MinecartStep newminecartbehavior$minecartstep1 = i > 0 ? this.currentLerpSteps.get(i - 1) : this.oldLerp;
            this.cacheIndexAlpha = new NewMinecartBehavior.StepPartialTicks(f2, newminecartbehavior$minecartstep, newminecartbehavior$minecartstep1);
            this.cachedLerpDelay = this.lerpDelay;
            this.cachedPartialTick = partialTick;
            return this.cacheIndexAlpha;
        }
    }

    public void adjustToRails(BlockPos pos, BlockState state, boolean snapToStart) {
        if (BaseRailBlock.isRail(state)) {
            RailShape railshape = ((BaseRailBlock)state.getBlock()).getRailDirection(state, this.level(), pos, this.minecart);
            Pair<Vec3i, Vec3i> pair = AbstractMinecart.exits(railshape);
            Vec3 vec3 = new Vec3(pair.getFirst()).scale(0.5);
            Vec3 vec31 = new Vec3(pair.getSecond()).scale(0.5);
            Vec3 vec32 = vec3.horizontal();
            Vec3 vec33 = vec31.horizontal();
            if (this.getDeltaMovement().length() > 1.0E-5F && this.getDeltaMovement().dot(vec32) < this.getDeltaMovement().dot(vec33)
                || this.isDecending(vec33, railshape)) {
                Vec3 vec34 = vec32;
                vec32 = vec33;
                vec33 = vec34;
            }

            float f = 180.0F - (float)(Math.atan2(vec32.z, vec32.x) * 180.0 / Math.PI);
            f += this.minecart.isFlipped() ? 180.0F : 0.0F;
            Vec3 vec35 = this.position();
            boolean flag = vec3.x() != vec31.x() && vec3.z() != vec31.z();
            Vec3 vec36;
            if (flag) {
                Vec3 vec37 = vec31.subtract(vec3);
                Vec3 vec38 = vec35.subtract(pos.getBottomCenter()).subtract(vec3);
                Vec3 vec39 = vec37.scale(vec37.dot(vec38) / vec37.dot(vec37));
                vec36 = pos.getBottomCenter().add(vec3).add(vec39);
                f = 180.0F - (float)(Math.atan2(vec39.z, vec39.x) * 180.0 / Math.PI);
                f += this.minecart.isFlipped() ? 180.0F : 0.0F;
            } else {
                boolean flag1 = vec3.subtract(vec31).x != 0.0;
                boolean flag2 = vec3.subtract(vec31).z != 0.0;
                vec36 = new Vec3(flag2 ? pos.getCenter().x : vec35.x, pos.getY(), flag1 ? pos.getCenter().z : vec35.z);
            }

            Vec3 vec311 = vec36.subtract(vec35);
            this.setPos(vec35.add(vec311));
            float f1 = 0.0F;
            boolean flag3 = vec3.y() != vec31.y();
            if (flag3) {
                Vec3 vec310 = pos.getBottomCenter().add(vec33);
                double d0 = vec310.distanceTo(this.position());
                this.setPos(this.position().add(0.0, d0 + 0.1, 0.0));
                f1 = this.minecart.isFlipped() ? 45.0F : -45.0F;
            } else {
                this.setPos(this.position().add(0.0, 0.1, 0.0));
            }

            this.setRotation(f, f1);
            double d1 = vec35.distanceTo(this.position());
            if (d1 > 0.0) {
                this.lerpSteps
                    .add(
                        new NewMinecartBehavior.MinecartStep(
                            this.position(), this.getDeltaMovement(), this.getYRot(), this.getXRot(), snapToStart ? 0.0F : (float)d1
                        )
                    );
            }
        }
    }

    private void setRotation(float yRot, float xRot) {
        double d0 = Math.abs(yRot - this.getYRot());
        if (d0 >= 175.0 && d0 <= 185.0) {
            this.minecart.setFlipped(!this.minecart.isFlipped());
            yRot -= 180.0F;
            xRot *= -1.0F;
        }

        xRot = Math.clamp(xRot, -45.0F, 45.0F);
        this.setXRot(xRot % 360.0F);
        this.setYRot(yRot % 360.0F);
    }

    @Override
    public void moveAlongTrack(ServerLevel level) {
        for (NewMinecartBehavior.TrackIteration newminecartbehavior$trackiteration = new NewMinecartBehavior.TrackIteration();
            newminecartbehavior$trackiteration.shouldIterate() && this.minecart.isAlive();
            newminecartbehavior$trackiteration.firstIteration = false
        ) {
            Vec3 vec3 = this.getDeltaMovement();
            BlockPos blockpos = this.minecart.getCurrentBlockPosOrRailBelow();
            BlockState blockstate = this.level().getBlockState(blockpos);
            boolean flag = BaseRailBlock.isRail(blockstate);
            if (this.minecart.isOnRails() != flag) {
                this.minecart.setOnRails(flag);
                this.adjustToRails(blockpos, blockstate, false);
            }

            if (flag) {
                this.minecart.resetFallDistance();
                this.minecart.setOldPosAndRot();
                if (blockstate.getBlock() instanceof PoweredRailBlock poweredRail && poweredRail.isActivatorRail()) {
                    this.minecart.activateMinecart(blockpos.getX(), blockpos.getY(), blockpos.getZ(), blockstate.getValue(PoweredRailBlock.POWERED));
                }

                RailShape railshape = ((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, this.level(), blockpos, this.minecart);;
                Vec3 vec31 = this.calculateTrackSpeed(level, vec3.horizontal(), newminecartbehavior$trackiteration, blockpos, blockstate, railshape);
                if (newminecartbehavior$trackiteration.firstIteration) {
                    newminecartbehavior$trackiteration.movementLeft = vec31.horizontalDistance();
                } else {
                    newminecartbehavior$trackiteration.movementLeft = newminecartbehavior$trackiteration.movementLeft
                        + (vec31.horizontalDistance() - vec3.horizontalDistance());
                }

                this.setDeltaMovement(vec31);
                newminecartbehavior$trackiteration.movementLeft = this.minecart
                    .makeStepAlongTrack(blockpos, railshape, newminecartbehavior$trackiteration.movementLeft);
            } else {
                this.minecart.comeOffTrack(level);
                newminecartbehavior$trackiteration.movementLeft = 0.0;
            }

            Vec3 vec32 = this.position();
            Vec3 vec33 = vec32.subtract(this.minecart.oldPosition());
            double d0 = vec33.length();
            if (d0 > 1.0E-5F) {
                if (!(vec33.horizontalDistanceSqr() > 1.0E-5F)) {
                    if (!this.minecart.isOnRails()) {
                        this.setXRot(this.minecart.onGround() ? 0.0F : Mth.rotLerp(0.2F, this.getXRot(), 0.0F));
                    }
                } else {
                    float f = 180.0F - (float)(Math.atan2(vec33.z, vec33.x) * 180.0 / Math.PI);
                    float f1 = this.minecart.onGround() && !this.minecart.isOnRails()
                        ? 0.0F
                        : 90.0F - (float)(Math.atan2(vec33.horizontalDistance(), vec33.y) * 180.0 / Math.PI);
                    f += this.minecart.isFlipped() ? 180.0F : 0.0F;
                    f1 *= this.minecart.isFlipped() ? -1.0F : 1.0F;
                    this.setRotation(f, f1);
                }

                this.lerpSteps
                    .add(
                        new NewMinecartBehavior.MinecartStep(
                            vec32, this.getDeltaMovement(), this.getYRot(), this.getXRot(), (float)Math.min(d0, this.getMaxSpeed(level))
                        )
                    );
            } else if (vec3.horizontalDistanceSqr() > 0.0) {
                this.lerpSteps.add(new NewMinecartBehavior.MinecartStep(vec32, this.getDeltaMovement(), this.getYRot(), this.getXRot(), 1.0F));
            }

            if (d0 > 1.0E-5F || newminecartbehavior$trackiteration.firstIteration) {
                this.minecart.applyEffectsFromBlocks();
                this.minecart.applyEffectsFromBlocks();
            }
        }
    }

    private Vec3 calculateTrackSpeed(
        ServerLevel level, Vec3 speed, NewMinecartBehavior.TrackIteration trackIteration, BlockPos pos, BlockState state, RailShape railShape
    ) {
        Vec3 vec3 = speed;
        if (!trackIteration.hasGainedSlopeSpeed) {
            Vec3 vec31 = this.calculateSlopeSpeed(speed, railShape);
            if (vec31.horizontalDistanceSqr() != speed.horizontalDistanceSqr()) {
                trackIteration.hasGainedSlopeSpeed = true;
                vec3 = vec31;
            }
        }

        if (trackIteration.firstIteration) {
            Vec3 vec32 = this.calculatePlayerInputSpeed(vec3);
            if (vec32.horizontalDistanceSqr() != vec3.horizontalDistanceSqr()) {
                trackIteration.hasHalted = true;
                vec3 = vec32;
            }
        }

        if (!trackIteration.hasHalted) {
            Vec3 vec33 = this.calculateHaltTrackSpeed(vec3, state);
            if (vec33.horizontalDistanceSqr() != vec3.horizontalDistanceSqr()) {
                trackIteration.hasHalted = true;
                vec3 = vec33;
            }
        }

        if (trackIteration.firstIteration) {
            vec3 = this.minecart.applyNaturalSlowdown(vec3);
            if (vec3.lengthSqr() > 0.0) {
                double d0 = Math.min(vec3.length(), this.minecart.getMaxSpeed(level));
                vec3 = vec3.normalize().scale(d0);
            }
        }

        if (!trackIteration.hasBoosted) {
            Vec3 vec34 = this.calculateBoostTrackSpeed(vec3, pos, state);
            if (vec34.horizontalDistanceSqr() != vec3.horizontalDistanceSqr()) {
                trackIteration.hasBoosted = true;
                vec3 = vec34;
            }
        }

        return vec3;
    }

    private Vec3 calculateSlopeSpeed(Vec3 speed, RailShape railShape) {
        double d0 = Math.max(0.0078125, speed.horizontalDistance() * 0.02);
        if (this.minecart.isInWater()) {
            d0 *= 0.2;
        }
        return switch (railShape) {
            case ASCENDING_EAST -> speed.add(-d0, 0.0, 0.0);
            case ASCENDING_WEST -> speed.add(d0, 0.0, 0.0);
            case ASCENDING_NORTH -> speed.add(0.0, 0.0, d0);
            case ASCENDING_SOUTH -> speed.add(0.0, 0.0, -d0);
            default -> speed;
        };
    }

    private Vec3 calculatePlayerInputSpeed(Vec3 speed) {
        if (this.minecart.getFirstPassenger() instanceof ServerPlayer serverplayer) {
            Vec3 vec31 = serverplayer.getLastClientMoveIntent();
            if (vec31.lengthSqr() > 0.0) {
                Vec3 vec3 = vec31.normalize();
                double d0 = speed.horizontalDistanceSqr();
                if (vec3.lengthSqr() > 0.0 && d0 < 0.01) {
                    return speed.add(new Vec3(vec3.x, 0.0, vec3.z).normalize().scale(0.001));
                }
            }

            return speed;
        } else {
            return speed;
        }
    }

    private Vec3 calculateHaltTrackSpeed(Vec3 speed, BlockState state) {
        if (state.getBlock() instanceof PoweredRailBlock poweredRail && !poweredRail.isActivatorRail() && !state.getValue(PoweredRailBlock.POWERED)) {
            return speed.length() < 0.03 ? Vec3.ZERO : speed.scale(0.5);
        } else {
            return speed;
        }
    }

    private Vec3 calculateBoostTrackSpeed(Vec3 speed, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof PoweredRailBlock poweredRail && !poweredRail.isActivatorRail() && state.getValue(PoweredRailBlock.POWERED)) {
            if (speed.length() > 0.01) {
                return speed.normalize().scale(speed.length() + 0.06);
            } else {
                Vec3 vec3 = this.minecart.getRedstoneDirection(pos);
                return vec3.lengthSqr() <= 0.0 ? speed : vec3.scale(speed.length() + 0.2);
            }
        } else {
            return speed;
        }
    }

    @Override
    public double stepAlongTrack(BlockPos pos, RailShape railShape, double speed) {
        if (speed < 1.0E-5F) {
            return 0.0;
        } else {
            Vec3 vec3 = this.position();
            Pair<Vec3i, Vec3i> pair = AbstractMinecart.exits(railShape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i1 = pair.getSecond();
            Vec3 vec31 = this.getDeltaMovement().horizontal();
            if (vec31.length() < 1.0E-5F) {
                this.setDeltaMovement(Vec3.ZERO);
                return 0.0;
            } else {
                boolean flag = vec3i.getY() != vec3i1.getY();
                Vec3 vec32 = new Vec3(vec3i1).scale(0.5).horizontal();
                Vec3 vec33 = new Vec3(vec3i).scale(0.5).horizontal();
                if (vec31.dot(vec33) < vec31.dot(vec32)) {
                    vec33 = vec32;
                }

                Vec3 vec34 = pos.getBottomCenter().add(vec33).add(0.0, 0.1, 0.0).add(vec33.normalize().scale(1.0E-5F));
                if (flag && !this.isDecending(vec31, railShape)) {
                    vec34 = vec34.add(0.0, 1.0, 0.0);
                }

                Vec3 vec35 = vec34.subtract(this.position()).normalize();
                vec31 = vec35.scale(vec31.length() / vec35.horizontalDistance());
                Vec3 vec36 = vec3.add(vec31.normalize().scale(speed * (flag ? Mth.SQRT_OF_TWO : 1.0F)));
                if (vec3.distanceToSqr(vec34) <= vec3.distanceToSqr(vec36)) {
                    speed = vec34.subtract(vec36).horizontalDistance();
                    vec36 = vec34;
                } else {
                    speed = 0.0;
                }

                this.minecart.move(MoverType.SELF, vec36.subtract(vec3));
                BlockPos railPos = BlockPos.containing(vec36);
                BlockState blockstate = this.level().getBlockState(railPos);
                if (flag) {
                    if (BaseRailBlock.isRail(blockstate)) {
                        RailShape railshape = ((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, this.level(), railPos, this.minecart);;
                        if (this.restAtVShape(railShape, railshape)) {
                            return 0.0;
                        }
                    }

                    double d1 = vec34.horizontal().distanceTo(this.position().horizontal());
                    double d0 = vec34.y + (this.isDecending(vec31, railShape) ? d1 : -d1);
                    if (this.position().y < d0) {
                        this.setPos(this.position().x, d0, this.position().z);
                    }
                }

                if (this.position().distanceTo(vec3) < 1.0E-5F && vec36.distanceTo(vec3) > 1.0E-5F) {
                    this.setDeltaMovement(Vec3.ZERO);
                    return 0.0;
                } else {
                    this.setDeltaMovement(vec31);
                    return speed;
                }
            }
        }
    }

    private boolean restAtVShape(RailShape shape1, RailShape shape2) {
        if (this.getDeltaMovement().lengthSqr() < 0.005
            && shape2.isSlope()
            && this.isDecending(this.getDeltaMovement(), shape1)
            && !this.isDecending(this.getDeltaMovement(), shape2)) {
            this.setDeltaMovement(Vec3.ZERO);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public double getMaxSpeed(ServerLevel level) {
        return level.getGameRules().getInt(GameRules.RULE_MINECART_MAX_SPEED) * (this.minecart.isInWater() ? 0.5 : 1.0) / 20.0;
    }

    private boolean isDecending(Vec3 speed, RailShape railShape) {
        return switch (railShape) {
            case ASCENDING_EAST -> speed.x < 0.0;
            case ASCENDING_WEST -> speed.x > 0.0;
            case ASCENDING_NORTH -> speed.z > 0.0;
            case ASCENDING_SOUTH -> speed.z < 0.0;
            default -> false;
        };
    }

    @Override
    public double getSlowdownFactor() {
        return this.minecart.isVehicle() ? 0.997 : 0.975;
    }

    @Override
    public boolean pushAndPickupEntities() {
        boolean flag = this.pickupEntities(this.minecart.getBoundingBox().inflate(0.2, 0.0, 0.2));
        if (!this.minecart.horizontalCollision && !this.minecart.verticalCollision) {
            return false;
        } else {
            boolean flag1 = this.pushEntities(this.minecart.getBoundingBox().inflate(1.0E-7));
            return flag && !flag1;
        }
    }

    public boolean pickupEntities(AABB box) {
        if (this.minecart.isRideable() && !this.minecart.isVehicle()) {
            List<Entity> list = this.level().getEntities(this.minecart, box, EntitySelector.pushableBy(this.minecart));
            if (!list.isEmpty()) {
                for (Entity entity : list) {
                    if (!(entity instanceof Player)
                        && !(entity instanceof IronGolem)
                        && !(entity instanceof AbstractMinecart)
                        && !this.minecart.isVehicle()
                        && !entity.isPassenger()) {
                        boolean flag = entity.startRiding(this.minecart);
                        if (flag) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean pushEntities(AABB box) {
        boolean flag = false;
        if (this.minecart.isRideable()) {
            List<Entity> list = this.level().getEntities(this.minecart, box, EntitySelector.pushableBy(this.minecart));
            if (!list.isEmpty()) {
                for (Entity entity : list) {
                    if (entity instanceof Player
                        || entity instanceof IronGolem
                        || entity instanceof AbstractMinecart
                        || this.minecart.isVehicle()
                        || entity.isPassenger()) {
                        entity.push(this.minecart);
                        flag = true;
                    }
                }
            }
        } else {
            for (Entity entity1 : this.level().getEntities(this.minecart, box)) {
                if (!this.minecart.hasPassenger(entity1) && entity1.isPushable() && entity1 instanceof AbstractMinecart) {
                    entity1.push(this.minecart);
                    flag = true;
                }
            }
        }

        return flag;
    }

    public record MinecartStep(Vec3 position, Vec3 movement, float yRot, float xRot, float weight) {
        public static final StreamCodec<ByteBuf, NewMinecartBehavior.MinecartStep> STREAM_CODEC = StreamCodec.composite(
            Vec3.STREAM_CODEC,
            NewMinecartBehavior.MinecartStep::position,
            Vec3.STREAM_CODEC,
            NewMinecartBehavior.MinecartStep::movement,
            ByteBufCodecs.ROTATION_BYTE,
            NewMinecartBehavior.MinecartStep::yRot,
            ByteBufCodecs.ROTATION_BYTE,
            NewMinecartBehavior.MinecartStep::xRot,
            ByteBufCodecs.FLOAT,
            NewMinecartBehavior.MinecartStep::weight,
            NewMinecartBehavior.MinecartStep::new
        );
        public static NewMinecartBehavior.MinecartStep ZERO = new NewMinecartBehavior.MinecartStep(Vec3.ZERO, Vec3.ZERO, 0.0F, 0.0F, 0.0F);
    }

    record StepPartialTicks(float partialTicksInStep, NewMinecartBehavior.MinecartStep currentStep, NewMinecartBehavior.MinecartStep previousStep) {
    }

    static class TrackIteration {
        double movementLeft = 0.0;
        boolean firstIteration = true;
        boolean hasGainedSlopeSpeed = false;
        boolean hasHalted = false;
        boolean hasBoosted = false;

        public boolean shouldIterate() {
            return this.firstIteration || this.movementLeft > 1.0E-5F;
        }
    }
}
