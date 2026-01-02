package net.goui.cosmicdungeon.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Optional;

public class CosmicSpawnerBlockEntity extends BlockEntity implements Spawner {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Persisted: what this spawner is set to spawn (label + commands + client preview bootstrap)
    private String spawnerEntityId = "none";

    // Client-side spin
    private float renderSpin;
    private float renderSpinPrev;
    private static final float SPIN_DEG_PER_TICK = 2f;

    // Client preview depends on BaseSpawner internal state
    private boolean clientSpawnerDirty = true;

    // -------- Reflection access to BaseSpawner private config fields --------
    private static final Field F_SPAWN_DELAY;
    private static final Field F_MIN_SPAWN_DELAY;
    private static final Field F_MAX_SPAWN_DELAY;
    private static final Field F_SPAWN_COUNT;
    private static final Field F_SPAWN_RANGE;
    private static final Field F_MAX_NEARBY_ENTITIES;
    private static final Field F_REQUIRED_PLAYER_RANGE;

    static {
        try {
            F_SPAWN_DELAY = BaseSpawner.class.getDeclaredField("spawnDelay");
            F_MIN_SPAWN_DELAY = BaseSpawner.class.getDeclaredField("minSpawnDelay");
            F_MAX_SPAWN_DELAY = BaseSpawner.class.getDeclaredField("maxSpawnDelay");
            F_SPAWN_COUNT = BaseSpawner.class.getDeclaredField("spawnCount");
            F_SPAWN_RANGE = BaseSpawner.class.getDeclaredField("spawnRange");
            F_MAX_NEARBY_ENTITIES = BaseSpawner.class.getDeclaredField("maxNearbyEntities");
            F_REQUIRED_PLAYER_RANGE = BaseSpawner.class.getDeclaredField("requiredPlayerRange");

            F_SPAWN_DELAY.setAccessible(true);
            F_MIN_SPAWN_DELAY.setAccessible(true);
            F_MAX_SPAWN_DELAY.setAccessible(true);
            F_SPAWN_COUNT.setAccessible(true);
            F_SPAWN_RANGE.setAccessible(true);
            F_MAX_NEARBY_ENTITIES.setAccessible(true);
            F_REQUIRED_PLAYER_RANGE.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to reflect BaseSpawner fields (NeoForge mappings changed?)", e);
        }
    }

    private final BaseSpawner spawner = new BaseSpawner() {
        @Override
        public void broadcastEvent(Level level, BlockPos pos, int id) {
            level.blockEvent(pos, CosmicSpawnerBlockEntity.this.getBlockState().getBlock(), id, 0);
        }

        @Override
        protected void setNextSpawnData(@Nullable Level level, BlockPos pos, SpawnData nextSpawnData) {
            // Force Cosmic spawner to ignore vanilla spawn placement rules (including light)
            super.setNextSpawnData(level, pos, forceFullBrightRules(nextSpawnData));
        }
    };

    public CosmicSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COSMIC_SPAWNER.get(), pos, state);
    }

    public BaseSpawner getSpawner() {
        return this.spawner;
    }

    public float getRenderSpinDegrees(float partialTick) {
        return Mth.lerp(partialTick, this.renderSpinPrev, this.renderSpin);
    }

    public String getSpawnerEntityId() {
        return spawnerEntityId;
    }

    public void setSpawnerEntityId(String id) {
        this.spawnerEntityId = (id == null || id.isBlank()) ? "none" : id.trim();
        this.clientSpawnerDirty = true;
        this.setChanged();
    }

    /** Call after changing config on the server to push a BE update to clients. */
    public void markUpdated() {
        if (this.level == null) return;
        BlockState st = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, st, st, 3);
    }

    @Override
    public void setEntityId(EntityType<?> type, RandomSource random) {
        Level level = this.level;
        if (level != null) {
            this.spawner.setEntityId(type, level, random, this.worldPosition);
            this.spawnerEntityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type));
            this.clientSpawnerDirty = true;
            this.setChanged();
            if (!level.isClientSide()) markUpdated();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide()) {
            this.clientSpawnerDirty = true;
            ensureClientPreviewConfigured();
        }
    }

    // ----------------------------
    // Runtime config setters (server-side)
    // ----------------------------

    public void setSpawnerDelayTicks(int ticks) {
        setInt(F_SPAWN_DELAY, ticks);
    }

    public void setSpawnerDelayRange(int minTicks, int maxTicks) {
        if (maxTicks < minTicks) {
            int tmp = minTicks;
            minTicks = maxTicks;
            maxTicks = tmp;
        }
        setInt(F_MIN_SPAWN_DELAY, minTicks);
        setInt(F_MAX_SPAWN_DELAY, maxTicks);
    }

    public void setSpawnerSpawnRange(int rangeBlocks) {
        setInt(F_SPAWN_RANGE, rangeBlocks);
    }

    public void setSpawnerSpawnCount(int count) {
        setInt(F_SPAWN_COUNT, count);
    }

    /** requiredPlayerRange */
    public void setSpawnerRequiredPlayerRange(int blocks) {
        setInt(F_REQUIRED_PLAYER_RANGE, blocks);
    }

    /** maxNearbyEntities */
    public void setSpawnerMaxNearbyEntities(int cap) {
        setInt(F_MAX_NEARBY_ENTITIES, cap);
    }

    private void setInt(Field f, int value) {
        try {
            f.setInt(this.spawner, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) markUpdated();
    }

    // ----------------------------
    // Runtime config getters (for /spawner stats)
    // ----------------------------

    public int getSpawnerDelayTicks() { return getInt(F_SPAWN_DELAY, 0); }
    public int getSpawnerMinSpawnDelay() { return getInt(F_MIN_SPAWN_DELAY, 0); }
    public int getSpawnerMaxSpawnDelay() { return getInt(F_MAX_SPAWN_DELAY, 0); }
    public int getSpawnerSpawnCount() { return getInt(F_SPAWN_COUNT, 0); }
    public int getSpawnerSpawnRange() { return getInt(F_SPAWN_RANGE, 0); }
    public int getSpawnerMaxNearbyEntities() { return getInt(F_MAX_NEARBY_ENTITIES, 0); }
    public int getSpawnerRequiredPlayerRange() { return getInt(F_REQUIRED_PLAYER_RANGE, 0); }

    private int getInt(Field f, int fallback) {
        try {
            return f.getInt(this.spawner);
        } catch (IllegalAccessException e) {
            return fallback;
        }
    }

    // ----------------------------
    // Saving / Loading (Value I/O)
    // ----------------------------

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);

        this.spawnerEntityId = input.getString("SpawnerEntityId").orElse("none");
        this.clientSpawnerDirty = true;

        Level level = this.level;
        if (level != null) {
            this.spawner.load(level, this.worldPosition, input);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        output.putString("SpawnerEntityId", this.spawnerEntityId);
        this.spawner.save(output);
        super.saveAdditional(output);
    }

    // ----------------------------
    // Server -> client sync
    // ----------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        this.loadWithComponents(input);
        this.clientSpawnerDirty = true;
        if (this.level != null && this.level.isClientSide()) {
            ensureClientPreviewConfigured();
        }
    }

    @Override
    public void onDataPacket(Connection net, ValueInput valueInput) {
        this.loadWithComponents(valueInput);
        this.clientSpawnerDirty = true;
        if (this.level != null && this.level.isClientSide()) {
            ensureClientPreviewConfigured();
        }
    }

    // ----------------------------
    // Client preview bootstrap
    // ----------------------------

    private void ensureClientPreviewConfigured() {
        if (this.level == null || !this.level.isClientSide()) return;
        if (!this.clientSpawnerDirty) return;

        String id = (this.spawnerEntityId == null) ? "none" : this.spawnerEntityId.trim();
        if (id.isEmpty() || id.equalsIgnoreCase("none")) {
            this.clientSpawnerDirty = false;
            return;
        }

        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            LOGGER.warn("CosmicSpawner at {} has invalid entity id '{}'", this.worldPosition, id);
            this.clientSpawnerDirty = false;
            return;
        }

        var typeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
        if (typeOpt.isEmpty()) {
            LOGGER.warn("CosmicSpawner at {} references unknown entity type '{}'", this.worldPosition, rl);
            this.clientSpawnerDirty = false;
            return;
        }

        this.spawner.setEntityId(typeOpt.get(), this.level, this.level.getRandom(), this.worldPosition);
        this.clientSpawnerDirty = false;
    }

    // ----------------------------
    // Tick hooks
    // ----------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, CosmicSpawnerBlockEntity be) {
        be.ensureClientPreviewConfigured();
        be.spawner.clientTick(level, pos);

        be.renderSpinPrev = be.renderSpin;
        be.renderSpin += SPIN_DEG_PER_TICK;
        if (be.renderSpin >= 360.0f) be.renderSpin -= 360.0f;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CosmicSpawnerBlockEntity be) {
        if ("none".equals(be.spawnerEntityId)) return;
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            be.spawner.serverTick(sl, pos);
        }
    }

    // ----------------------------
    // Helpers for commands / UI
    // ----------------------------

    public void applySpawnerEntity(Level level, EntityType<?> type) {
        this.spawner.setEntityId(type, level, level.getRandom(), this.worldPosition);
        this.spawnerEntityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        this.clientSpawnerDirty = true;
        this.setChanged();
        if (!level.isClientSide()) markUpdated();
    }

    public void clearSpawnerEntity(Level level) {
        this.spawnerEntityId = "none";
        this.clientSpawnerDirty = true;
        this.setChanged();
        if (!level.isClientSide()) markUpdated();
    }

    private static SpawnData forceFullBrightRules(SpawnData original) {
        CompoundTag entityCopy = original.getEntityToSpawn().copy();
        var anyLight = new net.minecraft.util.InclusiveRange<>(0, 15);
        SpawnData.CustomSpawnRules rules = new SpawnData.CustomSpawnRules(anyLight, anyLight);
        return new SpawnData(entityCopy, Optional.of(rules), original.getEquipment());
    }
}
