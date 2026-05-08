package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface Leashable {
    String LEASH_TAG = "leash";
    double LEASH_TOO_FAR_DIST = 12.0;
    double LEASH_ELASTIC_DIST = 6.0;
    double MAXIMUM_ALLOWED_LEASHED_DIST = 16.0;
    Vec3 AXIS_SPECIFIC_ELASTICITY = new Vec3(0.8, 0.2, 0.8);
    float SPRING_DAMPENING = 0.7F;
    double TORSIONAL_ELASTICITY = 10.0;
    double STIFFNESS = 0.11;
    List<Vec3> ENTITY_ATTACHMENT_POINT = ImmutableList.of(new Vec3(0.0, 0.5, 0.5));
    List<Vec3> LEASHER_ATTACHMENT_POINT = ImmutableList.of(new Vec3(0.0, 0.5, 0.0));
    List<Vec3> SHARED_QUAD_ATTACHMENT_POINTS = ImmutableList.of(
        new Vec3(-0.5, 0.5, 0.5), new Vec3(-0.5, 0.5, -0.5), new Vec3(0.5, 0.5, -0.5), new Vec3(0.5, 0.5, 0.5)
    );

    @Nullable
    Leashable.LeashData getLeashData();

    void setLeashData(@Nullable Leashable.LeashData leashData);

    default boolean isLeashed() {
        return this.getLeashData() != null && this.getLeashData().leashHolder != null;
    }

    default boolean mayBeLeashed() {
        return this.getLeashData() != null;
    }

    default boolean canHaveALeashAttachedTo(Entity entity) {
        if (this == entity) {
            return false;
        } else {
            return this.leashDistanceTo(entity) > this.leashSnapDistance() ? false : this.canBeLeashed();
        }
    }

    default double leashDistanceTo(Entity entity) {
        return entity.getBoundingBox().getCenter().distanceTo(((Entity)this).getBoundingBox().getCenter());
    }

    default boolean canBeLeashed() {
        return true;
    }

    default void setDelayedLeashHolderId(int delayedLeashHolderId) {
        this.setLeashData(new Leashable.LeashData(delayedLeashHolderId));
        dropLeash((Entity & Leashable)this, false, false);
    }

    default void readLeashData(ValueInput input) {
        Leashable.LeashData leashable$leashdata = input.read("leash", Leashable.LeashData.CODEC).orElse(null);
        if (this.getLeashData() != null && leashable$leashdata == null) {
            this.removeLeash();
        }

        this.setLeashData(leashable$leashdata);
    }

    default void writeLeashData(ValueOutput output, @Nullable Leashable.LeashData leashData) {
        output.storeNullable("leash", Leashable.LeashData.CODEC, leashData);
    }

    private static <E extends Entity & Leashable> void restoreLeashFromSave(E p_entity, Leashable.LeashData leashData) {
        if (leashData.delayedLeashInfo != null && p_entity.level() instanceof ServerLevel serverlevel) {
            Optional<UUID> optional1 = leashData.delayedLeashInfo.left();
            Optional<BlockPos> optional = leashData.delayedLeashInfo.right();
            if (optional1.isPresent()) {
                Entity entity = serverlevel.getEntity(optional1.get());
                if (entity != null) {
                    setLeashedTo(p_entity, entity, true);
                    return;
                }
            } else if (optional.isPresent()) {
                setLeashedTo(p_entity, LeashFenceKnotEntity.getOrCreateKnot(serverlevel, optional.get()), true);
                return;
            }

            if (p_entity.tickCount > 100) {
                p_entity.spawnAtLocation(serverlevel, Items.LEAD);
                p_entity.setLeashData(null);
            }
        }
    }

    default void dropLeash() {
        dropLeash((Entity & Leashable)this, true, true);
    }

    default void removeLeash() {
        dropLeash((Entity & Leashable)this, true, false);
    }

    default void onLeashRemoved() {
    }

    private static <E extends Entity & Leashable> void dropLeash(E entity, boolean broadcastPacket, boolean dropItem) {
        Leashable.LeashData leashable$leashdata = entity.getLeashData();
        if (leashable$leashdata != null && leashable$leashdata.leashHolder != null) {
            entity.setLeashData(null);
            entity.onLeashRemoved();
            if (entity.level() instanceof ServerLevel serverlevel) {
                if (dropItem) {
                    entity.spawnAtLocation(serverlevel, Items.LEAD);
                }

                if (broadcastPacket) {
                    serverlevel.getChunkSource().sendToTrackingPlayers(entity, new ClientboundSetEntityLinkPacket(entity, null));
                }

                leashable$leashdata.leashHolder.notifyLeasheeRemoved(entity);
            }
        }
    }

    static <E extends Entity & Leashable> void tickLeash(ServerLevel level, E p_entity) {
        Leashable.LeashData leashable$leashdata = p_entity.getLeashData();
        if (leashable$leashdata != null && leashable$leashdata.delayedLeashInfo != null) {
            restoreLeashFromSave(p_entity, leashable$leashdata);
        }

        if (leashable$leashdata != null && leashable$leashdata.leashHolder != null) {
            if (!p_entity.canInteractWithLevel() || !leashable$leashdata.leashHolder.canInteractWithLevel()) {
                if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                    p_entity.dropLeash();
                } else {
                    p_entity.removeLeash();
                }
            }

            Entity entity = p_entity.getLeashHolder();
            if (entity != null && entity.level() == p_entity.level()) {
                double d0 = p_entity.leashDistanceTo(entity);
                p_entity.whenLeashedTo(entity);
                if (d0 > p_entity.leashSnapDistance()) {
                    level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    p_entity.leashTooFarBehaviour();
                } else if (d0 > p_entity.leashElasticDistance() - entity.getBbWidth() - p_entity.getBbWidth()
                    && p_entity.checkElasticInteractions(entity, leashable$leashdata)) {
                    p_entity.onElasticLeashPull();
                } else {
                    p_entity.closeRangeLeashBehaviour(entity);
                }

                p_entity.setYRot((float)(p_entity.getYRot() - leashable$leashdata.angularMomentum));
                leashable$leashdata.angularMomentum = leashable$leashdata.angularMomentum * angularFriction(p_entity);
            }
        }
    }

    default void onElasticLeashPull() {
        Entity entity = (Entity)this;
        entity.checkFallDistanceAccumulation();
    }

    default double leashSnapDistance() {
        return 12.0;
    }

    default double leashElasticDistance() {
        return 6.0;
    }

    static <E extends Entity & Leashable> float angularFriction(E entity) {
        if (entity.onGround()) {
            var groundPos = entity.getBlockPosBelowThatAffectsMyMovement();
            return entity.level().getBlockState(groundPos).getFriction(entity.level(), groundPos, entity) * 0.91F;
        } else {
            return entity.isInLiquid() ? 0.8F : 0.91F;
        }
    }

    default void whenLeashedTo(Entity entity) {
        entity.notifyLeashHolder(this);
    }

    default void leashTooFarBehaviour() {
        this.dropLeash();
    }

    default void closeRangeLeashBehaviour(Entity entity) {
    }

    default boolean checkElasticInteractions(Entity entity, Leashable.LeashData leashData) {
        boolean flag = entity.supportQuadLeashAsHolder() && this.supportQuadLeash();
        List<Leashable.Wrench> list = computeElasticInteraction(
            (Entity & Leashable)this,
            entity,
            flag ? SHARED_QUAD_ATTACHMENT_POINTS : ENTITY_ATTACHMENT_POINT,
            flag ? SHARED_QUAD_ATTACHMENT_POINTS : LEASHER_ATTACHMENT_POINT
        );
        if (list.isEmpty()) {
            return false;
        } else {
            Leashable.Wrench leashable$wrench = Leashable.Wrench.accumulate(list).scale(flag ? 0.25 : 1.0);
            leashData.angularMomentum = leashData.angularMomentum + 10.0 * leashable$wrench.torque();
            Vec3 vec3 = getHolderMovement(entity).subtract(((Entity)this).getKnownMovement());
            ((Entity)this).addDeltaMovement(leashable$wrench.force().multiply(AXIS_SPECIFIC_ELASTICITY).add(vec3.scale(0.11)));
            return true;
        }
    }

    private static Vec3 getHolderMovement(Entity holder) {
        return holder instanceof Mob mob && mob.isNoAi() ? Vec3.ZERO : holder.getKnownMovement();
    }

    private static <E extends Entity & Leashable> List<Leashable.Wrench> computeElasticInteraction(
        E entity, Entity leashHolder, List<Vec3> entityAttachmentPoint, List<Vec3> leasherAttachmentPoint
    ) {
        double d0 = entity.leashElasticDistance();
        Vec3 vec3 = getHolderMovement(entity);
        float f = entity.getYRot() * (float) (Math.PI / 180.0);
        Vec3 vec31 = new Vec3(entity.getBbWidth(), entity.getBbHeight(), entity.getBbWidth());
        float f1 = leashHolder.getYRot() * (float) (Math.PI / 180.0);
        Vec3 vec32 = new Vec3(leashHolder.getBbWidth(), leashHolder.getBbHeight(), leashHolder.getBbWidth());
        List<Leashable.Wrench> list = new ArrayList<>();

        for (int i = 0; i < entityAttachmentPoint.size(); i++) {
            Vec3 vec33 = entityAttachmentPoint.get(i).multiply(vec31).yRot(-f);
            Vec3 vec34 = entity.position().add(vec33);
            Vec3 vec35 = leasherAttachmentPoint.get(i).multiply(vec32).yRot(-f1);
            Vec3 vec36 = leashHolder.position().add(vec35);
            computeDampenedSpringInteraction(vec36, vec34, d0, vec3, vec33).ifPresent(list::add);
        }

        return list;
    }

    private static Optional<Leashable.Wrench> computeDampenedSpringInteraction(Vec3 entityAttachmentPoint, Vec3 leasherAttachmentPoint, double elasticDistance, Vec3 knownMovement, Vec3 relativeAttachmentPoint) {
        double d0 = leasherAttachmentPoint.distanceTo(entityAttachmentPoint);
        if (d0 < elasticDistance) {
            return Optional.empty();
        } else {
            Vec3 vec3 = entityAttachmentPoint.subtract(leasherAttachmentPoint).normalize().scale(d0 - elasticDistance);
            double d1 = Leashable.Wrench.torqueFromForce(relativeAttachmentPoint, vec3);
            boolean flag = knownMovement.dot(vec3) >= 0.0;
            if (flag) {
                vec3 = vec3.scale(0.3F);
            }

            return Optional.of(new Leashable.Wrench(vec3, d1));
        }
    }

    default boolean supportQuadLeash() {
        return false;
    }

    default Vec3[] getQuadLeashOffsets() {
        return createQuadLeashOffsets((Entity)this, 0.0, 0.5, 0.5, 0.5);
    }

    static Vec3[] createQuadLeashOffsets(Entity entity, double zOffset, double z, double x, double y) {
        float f = entity.getBbWidth();
        double d0 = zOffset * f;
        double d1 = z * f;
        double d2 = x * f;
        double d3 = y * entity.getBbHeight();
        return new Vec3[]{new Vec3(-d2, d3, d1 + d0), new Vec3(-d2, d3, -d1 + d0), new Vec3(d2, d3, -d1 + d0), new Vec3(d2, d3, d1 + d0)};
    }

    default Vec3 getLeashOffset(float partialTick) {
        return this.getLeashOffset();
    }

    default Vec3 getLeashOffset() {
        Entity entity = (Entity)this;
        return new Vec3(0.0, entity.getEyeHeight(), entity.getBbWidth() * 0.4F);
    }

    default void setLeashedTo(Entity leashHolder, boolean broadcastPacket) {
        if (this != leashHolder) {
            setLeashedTo((Entity & Leashable)this, leashHolder, broadcastPacket);
        }
    }

    private static <E extends Entity & Leashable> void setLeashedTo(E p_entity, Entity leashHolder, boolean broadcastPacket) {
        Leashable.LeashData leashable$leashdata = p_entity.getLeashData();
        if (leashable$leashdata == null) {
            leashable$leashdata = new Leashable.LeashData(leashHolder);
            p_entity.setLeashData(leashable$leashdata);
        } else {
            Entity entity = leashable$leashdata.leashHolder;
            leashable$leashdata.setLeashHolder(leashHolder);
            if (entity != null && entity != leashHolder) {
                entity.notifyLeasheeRemoved(p_entity);
            }
        }

        if (broadcastPacket && p_entity.level() instanceof ServerLevel serverlevel) {
            serverlevel.getChunkSource().sendToTrackingPlayers(p_entity, new ClientboundSetEntityLinkPacket(p_entity, leashHolder));
        }

        if (p_entity.isPassenger()) {
            p_entity.stopRiding();
        }
    }

    @Nullable
    default Entity getLeashHolder() {
        return getLeashHolder((Entity & Leashable)this);
    }

    @Nullable
    private static <E extends Entity & Leashable> Entity getLeashHolder(E p_entity) {
        Leashable.LeashData leashable$leashdata = p_entity.getLeashData();
        if (leashable$leashdata == null) {
            return null;
        } else {
            if (leashable$leashdata.delayedLeashHolderId != 0 && p_entity.level().isClientSide()) {
                Entity entity = p_entity.level().getEntity(leashable$leashdata.delayedLeashHolderId);
                if (entity instanceof Entity) {
                    leashable$leashdata.setLeashHolder(entity);
                }
            }

            return leashable$leashdata.leashHolder;
        }
    }

    static List<Leashable> leashableLeashedTo(Entity entity) {
        return leashableInArea(entity, p_418528_ -> p_418528_.getLeashHolder() == entity);
    }

    static List<Leashable> leashableInArea(Entity entity, Predicate<Leashable> predicate) {
        return leashableInArea(entity.level(), entity.getBoundingBox().getCenter(), predicate);
    }

    static List<Leashable> leashableInArea(Level level, Vec3 pos, Predicate<Leashable> predicate) {
        double d0 = 32.0;
        AABB aabb = AABB.ofSize(pos, 32.0, 32.0, 32.0);
        return level.getEntitiesOfClass(Entity.class, aabb, p_418131_ -> p_418131_ instanceof Leashable leashable && predicate.test(leashable))
            .stream()
            .map(Leashable.class::cast)
            .toList();
    }

    public static final class LeashData {
        public static final Codec<Leashable.LeashData> CODEC = Codec.xor(UUIDUtil.CODEC.fieldOf("UUID").codec(), BlockPos.CODEC)
            .xmap(
                Leashable.LeashData::new,
                p_412912_ -> {
                    if (p_412912_.leashHolder instanceof LeashFenceKnotEntity leashfenceknotentity) {
                        return Either.right(leashfenceknotentity.getPos());
                    } else {
                        return p_412912_.leashHolder != null
                            ? Either.left(p_412912_.leashHolder.getUUID())
                            : Objects.requireNonNull(p_412912_.delayedLeashInfo, "Invalid LeashData had no attachment");
                    }
                }
            );
        int delayedLeashHolderId;
        @Nullable
        public Entity leashHolder;
        @Nullable
        public Either<UUID, BlockPos> delayedLeashInfo;
        public double angularMomentum;

        private LeashData(Either<UUID, BlockPos> delayedLeashInfo) {
            this.delayedLeashInfo = delayedLeashInfo;
        }

        LeashData(Entity leashHolder) {
            this.leashHolder = leashHolder;
        }

        LeashData(int delayedLeashInfoId) {
            this.delayedLeashHolderId = delayedLeashInfoId;
        }

        public void setLeashHolder(Entity leashHolder) {
            this.leashHolder = leashHolder;
            this.delayedLeashInfo = null;
            this.delayedLeashHolderId = 0;
        }
    }

    public record Wrench(Vec3 force, double torque) {
        static Leashable.Wrench ZERO = new Leashable.Wrench(Vec3.ZERO, 0.0);

        static double torqueFromForce(Vec3 attachmentPoint, Vec3 force) {
            return attachmentPoint.z * force.x - attachmentPoint.x * force.z;
        }

        static Leashable.Wrench accumulate(List<Leashable.Wrench> wrenches) {
            if (wrenches.isEmpty()) {
                return ZERO;
            } else {
                double d0 = 0.0;
                double d1 = 0.0;
                double d2 = 0.0;
                double d3 = 0.0;

                for (Leashable.Wrench leashable$wrench : wrenches) {
                    Vec3 vec3 = leashable$wrench.force;
                    d0 += vec3.x;
                    d1 += vec3.y;
                    d2 += vec3.z;
                    d3 += leashable$wrench.torque;
                }

                return new Leashable.Wrench(new Vec3(d0, d1, d2), d3);
            }
        }

        public Leashable.Wrench scale(double scale) {
            return new Leashable.Wrench(this.force.scale(scale), this.torque * scale);
        }
    }
}
