package net.minecraft.world.level;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public abstract class BaseSpawner implements net.neoforged.neoforge.common.extensions.IOwnedSpawner {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String SPAWN_DATA_TAG = "SpawnData";
    private static final int EVENT_SPAWN = 1;
    private static final int DEFAULT_SPAWN_DELAY = 20;
    private static final int DEFAULT_MIN_SPAWN_DELAY = 200;
    private static final int DEFAULT_MAX_SPAWN_DELAY = 800;
    private static final int DEFAULT_SPAWN_COUNT = 4;
    private static final int DEFAULT_MAX_NEARBY_ENTITIES = 6;
    private static final int DEFAULT_REQUIRED_PLAYER_RANGE = 16;
    private static final int DEFAULT_SPAWN_RANGE = 4;
    private int spawnDelay = 20;
    private WeightedList<SpawnData> spawnPotentials = WeightedList.of();
    @Nullable
    private SpawnData nextSpawnData;
    private double spin;
    private double oSpin;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 4;
    /**
     * Cached instance of the entity to render inside the spawner.
     */
    @Nullable
    private Entity displayEntity;
    private int maxNearbyEntities = 6;
    private int requiredPlayerRange = 16;
    private int spawnRange = 4;

    public void setEntityId(EntityType<?> type, @Nullable Level level, RandomSource random, BlockPos pos) {
        this.getOrCreateNextSpawnData(level, random, pos)
            .getEntityToSpawn()
            .putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
    }

    private boolean isNearPlayer(Level level, BlockPos pos) {
        return level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, this.requiredPlayerRange);
    }

    public void clientTick(Level level, BlockPos pos) {
        if (!this.isNearPlayer(level, pos)) {
            this.oSpin = this.spin;
        } else if (this.displayEntity != null) {
            RandomSource randomsource = level.getRandom();
            double d0 = pos.getX() + randomsource.nextDouble();
            double d1 = pos.getY() + randomsource.nextDouble();
            double d2 = pos.getZ() + randomsource.nextDouble();
            level.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, d0, d1, d2, 0.0, 0.0, 0.0);
            if (this.spawnDelay > 0) {
                this.spawnDelay--;
            }

            this.oSpin = this.spin;
            this.spin = (this.spin + 1000.0F / (this.spawnDelay + 200.0F)) % 360.0;
        }
    }

    public void serverTick(ServerLevel serverLevel, BlockPos pos) {
        if (this.isNearPlayer(serverLevel, pos) && serverLevel.getServer().isSpawnerBlockEnabled()) {
            if (this.spawnDelay == -1) {
                this.delay(serverLevel, pos);
            }

            if (this.spawnDelay > 0) {
                this.spawnDelay--;
            } else {
                boolean flag = false;
                RandomSource randomsource = serverLevel.getRandom();
                SpawnData spawndata = this.getOrCreateNextSpawnData(serverLevel, randomsource, pos);

                for (int i = 0; i < this.spawnCount; i++) {
                    try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this::toString, LOGGER)) {
                        ValueInput valueinput = TagValueInput.create(problemreporter$scopedcollector, serverLevel.registryAccess(), spawndata.getEntityToSpawn());
                        Optional<EntityType<?>> optional = EntityType.by(valueinput);
                        if (optional.isEmpty()) {
                            this.delay(serverLevel, pos);
                            return;
                        }

                        Vec3 vec3 = valueinput.read("Pos", Vec3.CODEC)
                            .orElseGet(
                                () -> new Vec3(
                                    pos.getX() + (randomsource.nextDouble() - randomsource.nextDouble()) * this.spawnRange + 0.5,
                                    pos.getY() + randomsource.nextInt(3) - 1,
                                    pos.getZ() + (randomsource.nextDouble() - randomsource.nextDouble()) * this.spawnRange + 0.5
                                )
                            );
                        if (serverLevel.noCollision(optional.get().getSpawnAABB(vec3.x, vec3.y, vec3.z))) {
                            BlockPos blockpos = BlockPos.containing(vec3);
                            if (spawndata.getCustomSpawnRules().isPresent()) {
                                if (!optional.get().getCategory().isFriendly() && serverLevel.getDifficulty() == Difficulty.PEACEFUL) {
                                    continue;
                                }

                                SpawnData.CustomSpawnRules spawndata$customspawnrules = spawndata.getCustomSpawnRules().get();
                                if (!spawndata$customspawnrules.isValidPosition(blockpos, serverLevel)) {
                                    continue;
                                }
                            } else if (!SpawnPlacements.checkSpawnRules(optional.get(), serverLevel, EntitySpawnReason.SPAWNER, blockpos, serverLevel.getRandom())) {
                                continue;
                            }

                            Entity entity = EntityType.loadEntityRecursive(valueinput, serverLevel, EntitySpawnReason.SPAWNER, p_404552_ -> {
                                p_404552_.snapTo(vec3.x, vec3.y, vec3.z, p_404552_.getYRot(), p_404552_.getXRot());
                                return p_404552_;
                            });
                            if (entity == null) {
                                this.delay(serverLevel, pos);
                                return;
                            }

                            int j = serverLevel.getEntities(
                                    EntityTypeTest.forExactClass(entity.getClass()),
                                    new AABB(
                                            pos.getX(),
                                            pos.getY(),
                                            pos.getZ(),
                                            pos.getX() + 1,
                                            pos.getY() + 1,
                                            pos.getZ() + 1
                                        )
                                        .inflate(this.spawnRange),
                                    EntitySelector.NO_SPECTATORS
                                )
                                .size();
                            if (j >= this.maxNearbyEntities) {
                                this.delay(serverLevel, pos);
                                return;
                            }

                            entity.snapTo(entity.getX(), entity.getY(), entity.getZ(), randomsource.nextFloat() * 360.0F, 0.0F);
                            if (entity instanceof Mob mob) {
                                if (!net.neoforged.neoforge.event.EventHooks.checkSpawnPositionSpawner(mob, serverLevel, EntitySpawnReason.SPAWNER, spawndata, this)) {
                                    continue;
                                }

                                boolean flag1 = spawndata.getEntityToSpawn().size() == 1 && spawndata.getEntityToSpawn().getString("id").isPresent();
                                // Neo: Patch in FinalizeSpawn for spawners so it may be fired unconditionally, instead of only when vanilla would normally call it.
                                // The local flag1 is the conditions under which the spawner will normally call Mob#finalizeSpawn.
                                net.neoforged.neoforge.event.EventHooks.finalizeMobSpawnSpawner(mob, serverLevel, serverLevel.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.SPAWNER, null, this, flag1);

                                spawndata.getEquipment().ifPresent(mob::equip);
                            }

                            if (!serverLevel.tryAddFreshEntityWithPassengers(entity)) {
                                this.delay(serverLevel, pos);
                                return;
                            }

                            serverLevel.levelEvent(2004, pos, 0);
                            serverLevel.gameEvent(entity, GameEvent.ENTITY_PLACE, blockpos);
                            if (entity instanceof Mob) {
                                ((Mob)entity).spawnAnim();
                            }

                            flag = true;
                        }
                    }
                }

                if (flag) {
                    this.delay(serverLevel, pos);
                }

                return;
            }
        }
    }

    private void delay(Level level, BlockPos pos) {
        RandomSource randomsource = level.random;
        if (this.maxSpawnDelay <= this.minSpawnDelay) {
            this.spawnDelay = this.minSpawnDelay;
        } else {
            this.spawnDelay = this.minSpawnDelay + randomsource.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
        }

        this.spawnPotentials.getRandom(randomsource).ifPresent(p_393311_ -> this.setNextSpawnData(level, pos, p_393311_));
        this.broadcastEvent(level, pos, 1);
    }

    public void load(@Nullable Level level, BlockPos pos, ValueInput input) {
        this.spawnDelay = input.getShortOr("Delay", (short)20);
        input.read("SpawnData", SpawnData.CODEC).ifPresent(p_400944_ -> this.setNextSpawnData(level, pos, p_400944_));
        this.spawnPotentials = input.read("SpawnPotentials", SpawnData.LIST_CODEC)
            .orElseGet(() -> WeightedList.of(this.nextSpawnData != null ? this.nextSpawnData : new SpawnData()));
        this.minSpawnDelay = input.getIntOr("MinSpawnDelay", 200);
        this.maxSpawnDelay = input.getIntOr("MaxSpawnDelay", 800);
        this.spawnCount = input.getIntOr("SpawnCount", 4);
        this.maxNearbyEntities = input.getIntOr("MaxNearbyEntities", 6);
        this.requiredPlayerRange = input.getIntOr("RequiredPlayerRange", 16);
        this.spawnRange = input.getIntOr("SpawnRange", 4);
        this.displayEntity = null;
    }

    public void save(ValueOutput output) {
        output.putShort("Delay", (short)this.spawnDelay);
        output.putShort("MinSpawnDelay", (short)this.minSpawnDelay);
        output.putShort("MaxSpawnDelay", (short)this.maxSpawnDelay);
        output.putShort("SpawnCount", (short)this.spawnCount);
        output.putShort("MaxNearbyEntities", (short)this.maxNearbyEntities);
        output.putShort("RequiredPlayerRange", (short)this.requiredPlayerRange);
        output.putShort("SpawnRange", (short)this.spawnRange);
        output.storeNullable("SpawnData", SpawnData.CODEC, this.nextSpawnData);
        output.store("SpawnPotentials", SpawnData.LIST_CODEC, this.spawnPotentials);
    }

    @Nullable
    public Entity getOrCreateDisplayEntity(Level level, BlockPos pos) {
        if (this.displayEntity == null) {
            CompoundTag compoundtag = this.getOrCreateNextSpawnData(level, level.getRandom(), pos).getEntityToSpawn();
            if (compoundtag.getString("id").isEmpty()) {
                return null;
            }

            this.displayEntity = EntityType.loadEntityRecursive(compoundtag, level, EntitySpawnReason.SPAWNER, Function.identity());
            if (compoundtag.size() == 1 && this.displayEntity instanceof Mob) {
            }
        }

        return this.displayEntity;
    }

    public boolean onEventTriggered(Level level, int id) {
        if (id == 1) {
            if (level.isClientSide()) {
                this.spawnDelay = this.minSpawnDelay;
            }

            return true;
        } else {
            return false;
        }
    }

    protected void setNextSpawnData(@Nullable Level level, BlockPos pos, SpawnData nextSpawnData) {
        this.nextSpawnData = nextSpawnData;
    }

    private SpawnData getOrCreateNextSpawnData(@Nullable Level level, RandomSource random, BlockPos pos) {
        if (this.nextSpawnData != null) {
            return this.nextSpawnData;
        } else {
            this.setNextSpawnData(level, pos, this.spawnPotentials.getRandom(random).orElseGet(SpawnData::new));
            return this.nextSpawnData;
        }
    }

    public abstract void broadcastEvent(Level level, BlockPos pos, int eventId);

    public double getSpin() {
        return this.spin;
    }

    public double getOSpin() {
        return this.oSpin;
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public com.mojang.datafixers.util.Either<net.minecraft.world.level.block.entity.BlockEntity, Entity> getOwner() {
        // The vanilla anonymous classes have proper overrides, but we return null here for compatibility.
        return null;
    }
}
