package net.minecraft.world.level.block.entity.trialspawner;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OminousItemSpawner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public enum TrialSpawnerState implements StringRepresentable {
    INACTIVE("inactive", 0, TrialSpawnerState.ParticleEmission.NONE, -1.0, false),
    WAITING_FOR_PLAYERS("waiting_for_players", 4, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, 200.0, true),
    ACTIVE("active", 8, TrialSpawnerState.ParticleEmission.FLAMES_AND_SMOKE, 1000.0, true),
    WAITING_FOR_REWARD_EJECTION("waiting_for_reward_ejection", 8, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, -1.0, false),
    EJECTING_REWARD("ejecting_reward", 8, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, -1.0, false),
    COOLDOWN("cooldown", 0, TrialSpawnerState.ParticleEmission.SMOKE_INSIDE_AND_TOP_FACE, -1.0, false);

    private static final float DELAY_BEFORE_EJECT_AFTER_KILLING_LAST_MOB = 40.0F;
    private static final int TIME_BETWEEN_EACH_EJECTION = Mth.floor(30.0F);
    private final String name;
    private final int lightLevel;
    private final double spinningMobSpeed;
    private final TrialSpawnerState.ParticleEmission particleEmission;
    private final boolean isCapableOfSpawning;

    private TrialSpawnerState(String name, int lightLevel, TrialSpawnerState.ParticleEmission particleEmission, double spinningMobSpeed, boolean isCapableOfSpawning) {
        this.name = name;
        this.lightLevel = lightLevel;
        this.particleEmission = particleEmission;
        this.spinningMobSpeed = spinningMobSpeed;
        this.isCapableOfSpawning = isCapableOfSpawning;
    }

    TrialSpawnerState tickAndGetNext(BlockPos pos, TrialSpawner spawner, ServerLevel level) {
        TrialSpawnerStateData trialspawnerstatedata = spawner.getStateData();
        TrialSpawnerConfig trialspawnerconfig = spawner.activeConfig();

        return switch (this) {
            case INACTIVE -> trialspawnerstatedata.getOrCreateDisplayEntity(spawner, level, WAITING_FOR_PLAYERS) == null ? this : WAITING_FOR_PLAYERS;
            case WAITING_FOR_PLAYERS -> {
                if (!spawner.canSpawnInLevel(level)) {
                    trialspawnerstatedata.resetStatistics();
                    yield this;
                } else if (!trialspawnerstatedata.hasMobToSpawn(spawner, level.random)) {
                    yield INACTIVE;
                } else {
                    trialspawnerstatedata.tryDetectPlayers(level, pos, spawner);
                    yield trialspawnerstatedata.detectedPlayers.isEmpty() ? this : ACTIVE;
                }
            }
            case ACTIVE -> {
                if (!spawner.canSpawnInLevel(level)) {
                    trialspawnerstatedata.resetStatistics();
                    yield WAITING_FOR_PLAYERS;
                } else if (!trialspawnerstatedata.hasMobToSpawn(spawner, level.random)) {
                    yield INACTIVE;
                } else {
                    int i = trialspawnerstatedata.countAdditionalPlayers(pos);
                    trialspawnerstatedata.tryDetectPlayers(level, pos, spawner);
                    if (spawner.isOminous()) {
                        this.spawnOminousOminousItemSpawner(level, pos, spawner);
                    }

                    if (trialspawnerstatedata.hasFinishedSpawningAllMobs(trialspawnerconfig, i)) {
                        if (trialspawnerstatedata.haveAllCurrentMobsDied()) {
                            trialspawnerstatedata.cooldownEndsAt = level.getGameTime() + spawner.getTargetCooldownLength();
                            trialspawnerstatedata.totalMobsSpawned = 0;
                            trialspawnerstatedata.nextMobSpawnsAt = 0L;
                            yield WAITING_FOR_REWARD_EJECTION;
                        }
                    } else if (trialspawnerstatedata.isReadyToSpawnNextMob(level, trialspawnerconfig, i)) {
                        spawner.spawnMob(level, pos).ifPresent(p_432717_ -> {
                            trialspawnerstatedata.currentMobs.add(p_432717_);
                            trialspawnerstatedata.totalMobsSpawned++;
                            trialspawnerstatedata.nextMobSpawnsAt = level.getGameTime() + trialspawnerconfig.ticksBetweenSpawn();
                            trialspawnerconfig.spawnPotentialsDefinition().getRandom(level.getRandom()).ifPresent(p_421425_ -> {
                                trialspawnerstatedata.nextSpawnData = Optional.of(p_421425_);
                                spawner.markUpdated();
                            });
                        });
                    }

                    yield this;
                }
            }
            case WAITING_FOR_REWARD_EJECTION -> {
                if (trialspawnerstatedata.isReadyToOpenShutter(level, 40.0F, spawner.getTargetCooldownLength())) {
                    level.playSound(null, pos, SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, SoundSource.BLOCKS);
                    yield EJECTING_REWARD;
                } else {
                    yield this;
                }
            }
            case EJECTING_REWARD -> {
                if (!trialspawnerstatedata.isReadyToEjectItems(level, TIME_BETWEEN_EACH_EJECTION, spawner.getTargetCooldownLength())) {
                    yield this;
                } else if (trialspawnerstatedata.detectedPlayers.isEmpty()) {
                    level.playSound(null, pos, SoundEvents.TRIAL_SPAWNER_CLOSE_SHUTTER, SoundSource.BLOCKS);
                    trialspawnerstatedata.ejectingLootTable = Optional.empty();
                    yield COOLDOWN;
                } else {
                    if (trialspawnerstatedata.ejectingLootTable.isEmpty()) {
                        trialspawnerstatedata.ejectingLootTable = trialspawnerconfig.lootTablesToEject().getRandom(level.getRandom());
                    }

                    trialspawnerstatedata.ejectingLootTable
                        .ifPresent(p_335304_ -> spawner.ejectReward(level, pos, (ResourceKey<LootTable>)p_335304_));
                    trialspawnerstatedata.detectedPlayers.remove(trialspawnerstatedata.detectedPlayers.iterator().next());
                    yield this;
                }
            }
            case COOLDOWN -> {
                trialspawnerstatedata.tryDetectPlayers(level, pos, spawner);
                if (!trialspawnerstatedata.detectedPlayers.isEmpty()) {
                    trialspawnerstatedata.totalMobsSpawned = 0;
                    trialspawnerstatedata.nextMobSpawnsAt = 0L;
                    yield ACTIVE;
                } else if (trialspawnerstatedata.isCooldownFinished(level)) {
                    spawner.removeOminous(level, pos);
                    trialspawnerstatedata.reset();
                    yield WAITING_FOR_PLAYERS;
                } else {
                    yield this;
                }
            }
        };
    }

    private void spawnOminousOminousItemSpawner(ServerLevel level, BlockPos pos, TrialSpawner spawner) {
        TrialSpawnerStateData trialspawnerstatedata = spawner.getStateData();
        TrialSpawnerConfig trialspawnerconfig = spawner.activeConfig();
        ItemStack itemstack = trialspawnerstatedata.getDispensingItems(level, trialspawnerconfig, pos)
            .getRandom(level.random)
            .orElse(ItemStack.EMPTY);
        if (!itemstack.isEmpty()) {
            if (this.timeToSpawnItemSpawner(level, trialspawnerstatedata)) {
                calculatePositionToSpawnSpawner(level, pos, spawner, trialspawnerstatedata).ifPresent(p_338064_ -> {
                    OminousItemSpawner ominousitemspawner = OminousItemSpawner.create(level, itemstack);
                    ominousitemspawner.snapTo(p_338064_);
                    level.addFreshEntity(ominousitemspawner);
                    float f = (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F + 1.0F;
                    level.playSound(null, BlockPos.containing(p_338064_), SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM_BEGIN, SoundSource.BLOCKS, 1.0F, f);
                    trialspawnerstatedata.cooldownEndsAt = level.getGameTime() + spawner.ominousConfig().ticksBetweenItemSpawners();
                });
            }
        }
    }

    private static Optional<Vec3> calculatePositionToSpawnSpawner(
        ServerLevel level, BlockPos pos, TrialSpawner spawner, TrialSpawnerStateData data
    ) {
        List<Player> list = data.detectedPlayers
            .stream()
            .map(level::getPlayerByUUID)
            .filter(Objects::nonNull)
            .filter(
                p_427215_ -> !p_427215_.isCreative()
                    && !p_427215_.isSpectator()
                    && p_427215_.isAlive()
                    && p_427215_.distanceToSqr(pos.getCenter()) <= Mth.square(spawner.getRequiredPlayerRange())
            )
            .toList();
        if (list.isEmpty()) {
            return Optional.empty();
        } else {
            Entity entity = selectEntityToSpawnItemAbove(list, data.currentMobs, spawner, pos, level);
            return entity == null ? Optional.empty() : calculatePositionAbove(entity, level);
        }
    }

    private static Optional<Vec3> calculatePositionAbove(Entity entity, ServerLevel level) {
        Vec3 vec3 = entity.position();
        Vec3 vec31 = vec3.relative(Direction.UP, entity.getBbHeight() + 2.0F + level.random.nextInt(4));
        BlockHitResult blockhitresult = level.clip(new ClipContext(vec3, vec31, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 vec32 = blockhitresult.getBlockPos().getCenter().relative(Direction.DOWN, 1.0);
        BlockPos blockpos = BlockPos.containing(vec32);
        return !level.getBlockState(blockpos).getCollisionShape(level, blockpos).isEmpty() ? Optional.empty() : Optional.of(vec32);
    }

    @Nullable
    private static Entity selectEntityToSpawnItemAbove(
        List<Player> player, Set<UUID> currentMobs, TrialSpawner spawner, BlockPos pos, ServerLevel level
    ) {
        Stream<Entity> stream = currentMobs.stream()
            .map(level::getEntity)
            .filter(Objects::nonNull)
            .filter(p_338051_ -> p_338051_.isAlive() && p_338051_.distanceToSqr(pos.getCenter()) <= Mth.square(spawner.getRequiredPlayerRange()));
        List<? extends Entity> list = level.random.nextBoolean() ? stream.toList() : player;
        if (list.isEmpty()) {
            return null;
        } else {
            return list.size() == 1 ? list.getFirst() : Util.getRandom(list, level.random);
        }
    }

    private boolean timeToSpawnItemSpawner(ServerLevel level, TrialSpawnerStateData data) {
        return level.getGameTime() >= data.cooldownEndsAt;
    }

    public int lightLevel() {
        return this.lightLevel;
    }

    public double spinningMobSpeed() {
        return this.spinningMobSpeed;
    }

    public boolean hasSpinningMob() {
        return this.spinningMobSpeed >= 0.0;
    }

    public boolean isCapableOfSpawning() {
        return this.isCapableOfSpawning;
    }

    public void emitParticles(Level level, BlockPos pos, boolean isOminous) {
        this.particleEmission.emit(level, level.getRandom(), pos, isOminous);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    static class LightLevel {
        private static final int UNLIT = 0;
        private static final int HALF_LIT = 4;
        private static final int LIT = 8;

        private LightLevel() {
        }
    }

    interface ParticleEmission {
        TrialSpawnerState.ParticleEmission NONE = (p_311998_, p_311983_, p_312351_, p_338371_) -> {};
        TrialSpawnerState.ParticleEmission SMALL_FLAMES = (p_338069_, p_338070_, p_338071_, p_338072_) -> {
            if (p_338070_.nextInt(2) == 0) {
                Vec3 vec3 = p_338071_.getCenter().offsetRandom(p_338070_, 0.9F);
                addParticle(p_338072_ ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMALL_FLAME, vec3, p_338069_);
            }
        };
        TrialSpawnerState.ParticleEmission FLAMES_AND_SMOKE = (p_338065_, p_338066_, p_338067_, p_338068_) -> {
            Vec3 vec3 = p_338067_.getCenter().offsetRandom(p_338066_, 1.0F);
            addParticle(ParticleTypes.SMOKE, vec3, p_338065_);
            addParticle(p_338068_ ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, vec3, p_338065_);
        };
        TrialSpawnerState.ParticleEmission SMOKE_INSIDE_AND_TOP_FACE = (p_311899_, p_311762_, p_312096_, p_338301_) -> {
            Vec3 vec3 = p_312096_.getCenter().offsetRandom(p_311762_, 0.9F);
            if (p_311762_.nextInt(3) == 0) {
                addParticle(ParticleTypes.SMOKE, vec3, p_311899_);
            }

            if (p_311899_.getGameTime() % 20L == 0L) {
                Vec3 vec31 = p_312096_.getCenter().add(0.0, 0.5, 0.0);
                int i = p_311899_.getRandom().nextInt(4) + 20;

                for (int j = 0; j < i; j++) {
                    addParticle(ParticleTypes.SMOKE, vec31, p_311899_);
                }
            }
        };

        private static void addParticle(SimpleParticleType particleType, Vec3 pos, Level level) {
            level.addParticle(particleType, pos.x(), pos.y(), pos.z(), 0.0, 0.0, 0.0);
        }

        void emit(Level level, RandomSource random, BlockPos pos, boolean isOminous);
    }

    static class SpinningMob {
        private static final double NONE = -1.0;
        private static final double SLOW = 200.0;
        private static final double FAST = 1000.0;

        private SpinningMob() {
        }
    }
}
