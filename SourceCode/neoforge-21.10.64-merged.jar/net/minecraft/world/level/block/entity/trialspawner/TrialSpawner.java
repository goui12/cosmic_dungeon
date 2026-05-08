package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;

public final class TrialSpawner implements net.neoforged.neoforge.common.extensions.IOwnedSpawner {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int DETECT_PLAYER_SPAWN_BUFFER = 40;
    private static final int DEFAULT_TARGET_COOLDOWN_LENGTH = 36000;
    private static final int DEFAULT_PLAYER_SCAN_RANGE = 14;
    private static final int MAX_MOB_TRACKING_DISTANCE = 47;
    private static final int MAX_MOB_TRACKING_DISTANCE_SQR = Mth.square(47);
    private static final float SPAWNING_AMBIENT_SOUND_CHANCE = 0.02F;
    private final TrialSpawnerStateData data = new TrialSpawnerStateData();
    private TrialSpawner.FullConfig config;
    private final TrialSpawner.StateAccessor stateAccessor;
    private PlayerDetector playerDetector;
    private final PlayerDetector.EntitySelector entitySelector;
    private boolean overridePeacefulAndMobSpawnRule;
    private boolean isOminous;

    public TrialSpawner(
        TrialSpawner.FullConfig config, TrialSpawner.StateAccessor stateAccessor, PlayerDetector playerDetector, PlayerDetector.EntitySelector entitySelector
    ) {
        this.config = config;
        this.stateAccessor = stateAccessor;
        this.playerDetector = playerDetector;
        this.entitySelector = entitySelector;
    }

    public TrialSpawnerConfig activeConfig() {
        return this.isOminous ? this.config.ominous().value() : this.config.normal.value();
    }

    public TrialSpawnerConfig normalConfig() {
        return this.config.normal.value();
    }

    public TrialSpawnerConfig ominousConfig() {
        return this.config.ominous.value();
    }

    public void load(ValueInput input) {
        input.read(TrialSpawnerStateData.Packed.MAP_CODEC).ifPresent(this.data::apply);
        this.config = input.read(TrialSpawner.FullConfig.MAP_CODEC).orElse(TrialSpawner.FullConfig.DEFAULT);
    }

    public void store(ValueOutput output) {
        output.store(TrialSpawnerStateData.Packed.MAP_CODEC, this.data.pack());
        output.store(TrialSpawner.FullConfig.MAP_CODEC, this.config);
    }

    public void applyOminous(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, level.getBlockState(pos).setValue(TrialSpawnerBlock.OMINOUS, true), 3);
        level.levelEvent(3020, pos, 1);
        this.isOminous = true;
        this.data.resetAfterBecomingOminous(this, level);
    }

    public void removeOminous(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, level.getBlockState(pos).setValue(TrialSpawnerBlock.OMINOUS, false), 3);
        this.isOminous = false;
    }

    public boolean isOminous() {
        return this.isOminous;
    }

    public int getTargetCooldownLength() {
        return this.config.targetCooldownLength;
    }

    public int getRequiredPlayerRange() {
        return this.config.requiredPlayerRange;
    }

    public TrialSpawnerState getState() {
        return this.stateAccessor.getState();
    }

    public TrialSpawnerStateData getStateData() {
        return this.data;
    }

    public void setState(Level level, TrialSpawnerState state) {
        this.stateAccessor.setState(level, state);
    }

    public void markUpdated() {
        this.stateAccessor.markUpdated();
    }

    public PlayerDetector getPlayerDetector() {
        return this.playerDetector;
    }

    public PlayerDetector.EntitySelector getEntitySelector() {
        return this.entitySelector;
    }

    public boolean canSpawnInLevel(ServerLevel level) {
        if (!level.getServer().getGameRules().getBoolean(GameRules.RULE_SPAWNER_BLOCKS_ENABLED)) {
            return false;
        } else if (this.overridePeacefulAndMobSpawnRule) {
            return true;
        } else {
            return level.getDifficulty() == Difficulty.PEACEFUL ? false : level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        }
    }

    public Optional<UUID> spawnMob(ServerLevel level, BlockPos pos) {
        RandomSource randomsource = level.getRandom();
        SpawnData spawndata = this.data.getOrCreateNextSpawnData(this, level.getRandom());

        Optional optional1;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(() -> "spawner@" + pos, LOGGER)) {
            ValueInput valueinput = TagValueInput.create(problemreporter$scopedcollector, level.registryAccess(), spawndata.entityToSpawn());
            Optional<EntityType<?>> optional = EntityType.by(valueinput);
            if (optional.isEmpty()) {
                return Optional.empty();
            }

            Vec3 vec3 = valueinput.read("Pos", Vec3.CODEC)
                .orElseGet(
                    () -> {
                        TrialSpawnerConfig trialspawnerconfig = this.activeConfig();
                        return new Vec3(
                            pos.getX() + (randomsource.nextDouble() - randomsource.nextDouble()) * trialspawnerconfig.spawnRange() + 0.5,
                            pos.getY() + randomsource.nextInt(3) - 1,
                            pos.getZ() + (randomsource.nextDouble() - randomsource.nextDouble()) * trialspawnerconfig.spawnRange() + 0.5
                        );
                    }
                );
            if (!level.noCollision(optional.get().getSpawnAABB(vec3.x, vec3.y, vec3.z))) {
                return Optional.empty();
            }

            if (!inLineOfSight(level, pos.getCenter(), vec3)) {
                return Optional.empty();
            }

            BlockPos blockpos = BlockPos.containing(vec3);
            if (!SpawnPlacements.checkSpawnRules(optional.get(), level, EntitySpawnReason.TRIAL_SPAWNER, blockpos, level.getRandom())) {
                return Optional.empty();
            }

            if (spawndata.getCustomSpawnRules().isPresent()) {
                SpawnData.CustomSpawnRules spawndata$customspawnrules = spawndata.getCustomSpawnRules().get();
                if (!spawndata$customspawnrules.isValidPosition(blockpos, level)) {
                    return Optional.empty();
                }
            }

            Entity entity = EntityType.loadEntityRecursive(valueinput, level, EntitySpawnReason.TRIAL_SPAWNER, p_404588_ -> {
                p_404588_.snapTo(vec3.x, vec3.y, vec3.z, randomsource.nextFloat() * 360.0F, 0.0F);
                return p_404588_;
            });
            if (entity == null) {
                return Optional.empty();
            }

            if (entity instanceof Mob mob) {
                if (!mob.checkSpawnObstruction(level)) {
                    return Optional.empty();
                }

                boolean flag = spawndata.getEntityToSpawn().size() == 1 && spawndata.getEntityToSpawn().getString("id").isPresent();
                // Neo: Patch in FinalizeSpawn for spawners so it may be fired unconditionally, instead of only when vanilla would normally call it.
                // The local flag is the conditions under which the spawner will normally call Mob#finalizeSpawn.
                net.neoforged.neoforge.event.EventHooks.finalizeMobSpawnSpawner(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.TRIAL_SPAWNER, null, this, flag);

                mob.setPersistenceRequired();
                spawndata.getEquipment().ifPresent(mob::equip);
            }

            if (!level.tryAddFreshEntityWithPassengers(entity)) {
                return Optional.empty();
            }

            TrialSpawner.FlameParticle trialspawner$flameparticle = this.isOminous ? TrialSpawner.FlameParticle.OMINOUS : TrialSpawner.FlameParticle.NORMAL;
            level.levelEvent(3011, pos, trialspawner$flameparticle.encode());
            level.levelEvent(3012, blockpos, trialspawner$flameparticle.encode());
            level.gameEvent(entity, GameEvent.ENTITY_PLACE, blockpos);
            optional1 = Optional.of(entity.getUUID());
        }

        return optional1;
    }

    public void ejectReward(ServerLevel level, BlockPos pos, ResourceKey<LootTable> lootTable) {
        LootTable loottable = level.getServer().reloadableRegistries().getLootTable(lootTable);
        LootParams lootparams = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams);
        if (!objectarraylist.isEmpty()) {
            for (ItemStack itemstack : objectarraylist) {
                DefaultDispenseItemBehavior.spawnItem(level, itemstack, 2, Direction.UP, Vec3.atBottomCenterOf(pos).relative(Direction.UP, 1.2));
            }

            level.levelEvent(3014, pos, 0);
        }
    }

    public void tickClient(Level level, BlockPos pos, boolean isOminous) {
        TrialSpawnerState trialspawnerstate = this.getState();
        trialspawnerstate.emitParticles(level, pos, isOminous);
        if (trialspawnerstate.hasSpinningMob()) {
            double d0 = Math.max(0L, this.data.nextMobSpawnsAt - level.getGameTime());
            this.data.oSpin = this.data.spin;
            this.data.spin = (this.data.spin + trialspawnerstate.spinningMobSpeed() / (d0 + 200.0)) % 360.0;
        }

        if (trialspawnerstate.isCapableOfSpawning()) {
            RandomSource randomsource = level.getRandom();
            if (randomsource.nextFloat() <= 0.02F) {
                SoundEvent soundevent = isOminous ? SoundEvents.TRIAL_SPAWNER_AMBIENT_OMINOUS : SoundEvents.TRIAL_SPAWNER_AMBIENT;
                level.playLocalSound(
                    pos, soundevent, SoundSource.BLOCKS, randomsource.nextFloat() * 0.25F + 0.75F, randomsource.nextFloat() + 0.5F, false
                );
            }
        }
    }

    public void tickServer(ServerLevel level, BlockPos pos, boolean isOminous) {
        this.isOminous = isOminous;
        TrialSpawnerState trialspawnerstate = this.getState();
        if (this.data.currentMobs.removeIf(p_312870_ -> shouldMobBeUntracked(level, pos, p_312870_))) {
            this.data.nextMobSpawnsAt = level.getGameTime() + this.activeConfig().ticksBetweenSpawn();
        }

        TrialSpawnerState trialspawnerstate1 = trialspawnerstate.tickAndGetNext(pos, this, level);
        if (trialspawnerstate1 != trialspawnerstate) {
            this.setState(level, trialspawnerstate1);
        }
    }

    private static boolean shouldMobBeUntracked(ServerLevel level, BlockPos pos, UUID uuid) {
        Entity entity = level.getEntity(uuid);
        return entity == null
            || !entity.isAlive()
            || !entity.level().dimension().equals(level.dimension())
            || entity.blockPosition().distSqr(pos) > MAX_MOB_TRACKING_DISTANCE_SQR;
    }

    private static boolean inLineOfSight(Level level, Vec3 spawnerPos, Vec3 mobPos) {
        BlockHitResult blockhitresult = level.clip(
            new ClipContext(mobPos, spawnerPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())
        );
        return blockhitresult.getBlockPos().equals(BlockPos.containing(spawnerPos)) || blockhitresult.getType() == HitResult.Type.MISS;
    }

    public static void addSpawnParticles(Level level, BlockPos pos, RandomSource random, SimpleParticleType particleType) {
        for (int i = 0; i < 20; i++) {
            double d0 = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double d1 = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double d2 = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            level.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
            level.addParticle(particleType, d0, d1, d2, 0.0, 0.0, 0.0);
        }
    }

    public static void addBecomeOminousParticles(Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 20; i++) {
            double d0 = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double d1 = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double d2 = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2.0;
            double d3 = random.nextGaussian() * 0.02;
            double d4 = random.nextGaussian() * 0.02;
            double d5 = random.nextGaussian() * 0.02;
            level.addParticle(ParticleTypes.TRIAL_OMEN, d0, d1, d2, d3, d4, d5);
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, d0, d1, d2, d3, d4, d5);
        }
    }

    public static void addDetectPlayerParticles(Level level, BlockPos pos, RandomSource random, int type, ParticleOptions particle) {
        for (int i = 0; i < 30 + Math.min(type, 10) * 5; i++) {
            double d0 = (2.0F * random.nextFloat() - 1.0F) * 0.65;
            double d1 = (2.0F * random.nextFloat() - 1.0F) * 0.65;
            double d2 = pos.getX() + 0.5 + d0;
            double d3 = pos.getY() + 0.1 + random.nextFloat() * 0.8;
            double d4 = pos.getZ() + 0.5 + d1;
            level.addParticle(particle, d2, d3, d4, 0.0, 0.0, 0.0);
        }
    }

    public static void addEjectItemParticles(Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 20; i++) {
            double d0 = pos.getX() + 0.4 + random.nextDouble() * 0.2;
            double d1 = pos.getY() + 0.4 + random.nextDouble() * 0.2;
            double d2 = pos.getZ() + 0.4 + random.nextDouble() * 0.2;
            double d3 = random.nextGaussian() * 0.02;
            double d4 = random.nextGaussian() * 0.02;
            double d5 = random.nextGaussian() * 0.02;
            level.addParticle(ParticleTypes.SMALL_FLAME, d0, d1, d2, d3, d4, d5 * 0.25);
            level.addParticle(ParticleTypes.SMOKE, d0, d1, d2, d3, d4, d5);
        }
    }

    public void overrideEntityToSpawn(EntityType<?> entityType, Level level) {
        this.data.reset();
        this.config = this.config.overrideEntity(entityType);
        this.setState(level, TrialSpawnerState.INACTIVE);
    }

    @Deprecated(
        forRemoval = true
    )
    @VisibleForTesting
    public void setPlayerDetector(PlayerDetector playerDetector) {
        this.playerDetector = playerDetector;
    }

    @Deprecated(
        forRemoval = true
    )
    @VisibleForTesting
    public void overridePeacefulAndMobSpawnRule() {
        this.overridePeacefulAndMobSpawnRule = true;
    }

    public static enum FlameParticle {
        NORMAL(ParticleTypes.FLAME),
        OMINOUS(ParticleTypes.SOUL_FIRE_FLAME);

        public final SimpleParticleType particleType;

        private FlameParticle(SimpleParticleType particleType) {
            this.particleType = particleType;
        }

        public static TrialSpawner.FlameParticle decode(int id) {
            TrialSpawner.FlameParticle[] atrialspawner$flameparticle = values();
            return id <= atrialspawner$flameparticle.length && id >= 0 ? atrialspawner$flameparticle[id] : NORMAL;
        }

        public int encode() {
            return this.ordinal();
        }
    }

    public record FullConfig(Holder<TrialSpawnerConfig> normal, Holder<TrialSpawnerConfig> ominous, int targetCooldownLength, int requiredPlayerRange) {
        public static final MapCodec<TrialSpawner.FullConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_422015_ -> p_422015_.group(
                    TrialSpawnerConfig.CODEC
                        .optionalFieldOf("normal_config", Holder.direct(TrialSpawnerConfig.DEFAULT))
                        .forGetter(TrialSpawner.FullConfig::normal),
                    TrialSpawnerConfig.CODEC
                        .optionalFieldOf("ominous_config", Holder.direct(TrialSpawnerConfig.DEFAULT))
                        .forGetter(TrialSpawner.FullConfig::ominous),
                    ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("target_cooldown_length", 36000).forGetter(TrialSpawner.FullConfig::targetCooldownLength),
                    Codec.intRange(1, 128).optionalFieldOf("required_player_range", 14).forGetter(TrialSpawner.FullConfig::requiredPlayerRange)
                )
                .apply(p_422015_, TrialSpawner.FullConfig::new)
        );
        public static final TrialSpawner.FullConfig DEFAULT = new TrialSpawner.FullConfig(
            Holder.direct(TrialSpawnerConfig.DEFAULT), Holder.direct(TrialSpawnerConfig.DEFAULT), 36000, 14
        );

        public TrialSpawner.FullConfig overrideEntity(EntityType<?> entity) {
            return new TrialSpawner.FullConfig(
                Holder.direct(this.normal.value().withSpawning(entity)),
                Holder.direct(this.ominous.value().withSpawning(entity)),
                this.targetCooldownLength,
                this.requiredPlayerRange
            );
        }
    }

    public interface StateAccessor {
        void setState(Level level, TrialSpawnerState state);

        TrialSpawnerState getState();

        void markUpdated();
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public com.mojang.datafixers.util.Either<net.minecraft.world.level.block.entity.BlockEntity, Entity> getOwner() {
        if (this.stateAccessor instanceof net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity be) {
            return com.mojang.datafixers.util.Either.left(be);
        }
        return null;
    }
}
