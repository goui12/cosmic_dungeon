// file: src/main/java/net/goui/cosmicdungeon/block/entity/CosmicSpawnerBlockEntity.java
package net.goui.cosmicdungeon.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
import java.util.List;
import java.util.Optional;

public class CosmicSpawnerBlockEntity extends BlockEntity implements Spawner {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Toggle extra spawner debug logging with:
     * -Dcosmicdungeon.debugSpawner=true
     */
    private static final boolean DEBUG = Boolean.getBoolean("cosmicdungeon.debugSpawner");

    // Persisted: what this spawner is set to spawn (label + commands + client preview bootstrap)
    private String spawnerEntityId = "none";
    private CosmicSpawnerPreset spawnerPreset;

    // Persisted: if true, this spawner self-destructs after it successfully spawns once.
    // Default: false
    private boolean bossOneShot = false;

    // Persisted: whether the one-shot spawner has already successfully spawned.
    // Default: false
    private boolean bossHasSpawned = false;

    // Persisted: max living mobs that this individual spawner may have active at one time.
    // A value <= 0 means uncapped. Spawned mobs are tracked with this spawner's unique entity tag.
    private int spawnerMobCap = 0;

    // Client-side spin
    private float renderSpin;
    private float renderSpinPrev;
    private static final float SPIN_DEG_PER_TICK = 2f;

    // Client preview depends on BaseSpawner internal state
    private boolean clientSpawnerDirty = true;

    // Coalesce server->client block updates (avoid spamming within same tick)
    private long lastUpdateGameTime = Long.MIN_VALUE;

    // One-shot spawn detection cache (tag-filtered, not total entities).
    private int oneShotTaggedCount = -1;

    // -------- Reflection access to BaseSpawner private config fields --------
    // These are mapping-sensitive. We treat missing fields as "feature disabled" instead of hard-crashing.
    private static final @Nullable Field F_SPAWN_DELAY;
    private static final @Nullable Field F_MIN_SPAWN_DELAY;
    private static final @Nullable Field F_MAX_SPAWN_DELAY;
    private static final @Nullable Field F_SPAWN_COUNT;
    private static final @Nullable Field F_SPAWN_RANGE;
    private static final @Nullable Field F_MAX_NEARBY_ENTITIES;
    private static final @Nullable Field F_REQUIRED_PLAYER_RANGE;

    /**
     * BaseSpawner caches a "display" entity instance for the spinning mob preview.
     * If this cache isn't invalidated when the mob is changed, the spawner can keep rendering the OLD mob.
     */
    private static final @Nullable Field F_DISPLAY_ENTITY;

    static {
        // Mojang name set (commonly stable), but can change with mappings.
        F_SPAWN_DELAY = reflectField(BaseSpawner.class, "spawnDelay");
        F_MIN_SPAWN_DELAY = reflectField(BaseSpawner.class, "minSpawnDelay");
        F_MAX_SPAWN_DELAY = reflectField(BaseSpawner.class, "maxSpawnDelay");
        F_SPAWN_COUNT = reflectField(BaseSpawner.class, "spawnCount");
        F_SPAWN_RANGE = reflectField(BaseSpawner.class, "spawnRange");
        F_MAX_NEARBY_ENTITIES = reflectField(BaseSpawner.class, "maxNearbyEntities");
        F_REQUIRED_PLAYER_RANGE = reflectField(BaseSpawner.class, "requiredPlayerRange");

        // Mojang name is typically "displayEntity" but we fall back to a type scan to be resilient.
        F_DISPLAY_ENTITY = findDisplayEntityField();

        // Warn (once) if any knob fields are missing, but do NOT crash.
        if (F_SPAWN_DELAY == null
                || F_MIN_SPAWN_DELAY == null
                || F_MAX_SPAWN_DELAY == null
                || F_SPAWN_COUNT == null
                || F_SPAWN_RANGE == null
                || F_MAX_NEARBY_ENTITIES == null
                || F_REQUIRED_PLAYER_RANGE == null) {
            LOGGER.warn("CosmicSpawner: One or more BaseSpawner config fields could not be reflected. " +
                    "Spawner knob commands (delay/range/count/players/cap) may be limited on this mapping.");
        }
    }

    @Nullable
    private static Field reflectField(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Forces the BaseSpawner to rebuild its client-side preview entity.
     * Best-effort: if mappings differ, we just skip without crashing.
     */
    private void invalidatePreviewEntityCache() {
        if (F_DISPLAY_ENTITY == null) return;
        try {
            if (F_DISPLAY_ENTITY.getType() == Optional.class) {
                F_DISPLAY_ENTITY.set(this.spawner, Optional.empty());
            } else {
                F_DISPLAY_ENTITY.set(this.spawner, null);
            }
        } catch (Throwable t) {
            // No crash; preview may just not refresh on some mappings.
            if (DEBUG) LOGGER.debug("Failed to invalidate BaseSpawner preview entity cache", t);
        }
    }

    @Nullable
    private static Field findDisplayEntityField() {
        // Preferred name
        try {
            Field f = BaseSpawner.class.getDeclaredField("displayEntity");
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException ignored) {
            // fall through
        }

        // Fallback: find the first field that can hold an Entity reference (or Optional cache).
        for (Field f : BaseSpawner.class.getDeclaredFields()) {
            Class<?> t = f.getType();
            if (Entity.class.isAssignableFrom(t) || t == Optional.class) {
                try {
                    f.setAccessible(true);
                    return f;
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        }
        return null;
    }

    private final CosmicBaseSpawner spawner = new CosmicBaseSpawner();

    private final class CosmicBaseSpawner extends BaseSpawner {
        @Override
        public void broadcastEvent(Level level, BlockPos pos, int id) {
            level.blockEvent(pos, CosmicSpawnerBlockEntity.this.getBlockState().getBlock(), id, 0);
        }

        @Override
        protected void setNextSpawnData(@Nullable Level level, BlockPos pos, SpawnData nextSpawnData) {
            // Force Cosmic spawner to ignore vanilla spawn placement rules (including light)
            super.setNextSpawnData(level, pos, forceFullBrightRules(nextSpawnData));
        }
        public void setNextSpawnDataPublic(@Nullable Level level, BlockPos pos, SpawnData nextSpawnData) {
            this.setNextSpawnData(level, pos, nextSpawnData);
        }
    }

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

    public String getSpawnerDisplayEntityId() {
        return this.spawnerPreset != null ? this.spawnerPreset.getDisplayEntityTypeId().toString() : spawnerEntityId;
    }

    public void setSpawnerEntityId(String id) {
        this.spawnerEntityId = (id == null || id.isBlank()) ? "none" : id.trim();
        this.clientSpawnerDirty = true;
        invalidatePreviewEntityCache();
        this.setChanged();
    }

    // ----------------------------
    // Boss one-shot API (command + debug)
    // ----------------------------

    public boolean isBossOneShot() {
        return bossOneShot;
    }

    public boolean hasBossSpawned() {
        return bossHasSpawned;
    }

    public int getSpawnerMobCap() {
        return spawnerMobCap;
    }

    public void setSpawnerMobCap(int cap) {
        this.spawnerMobCap = Math.max(0, cap);
        if (this.spawnerMobCap > 0) {
            // Disable vanilla's nearby-entity cap so only this spawner's tag-based count controls spawning.
            this.setSpawnerMaxNearbyEntities(Short.MAX_VALUE);
        }
        this.oneShotTaggedCount = -1;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            markUpdated();
        }
    }

    /**
     * Enables/disables one-shot self-destruct behavior.
     * Enabling also rearms the spawner (BossHasSpawned=false).
     */
    public void setBossOneShot(boolean enabled) {
        this.bossOneShot = enabled;
        if (enabled) {
            // Rearm on enable so you can turn it on after setting mob/delay.
            this.bossHasSpawned = false;
            // Boss spawns should not be blocked by unrelated nearby mobs of the same class.
            this.setSpawnerMaxNearbyEntities(Short.MAX_VALUE);
        }
        this.oneShotTaggedCount = -1;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            markUpdated();
        }
    }

    /** Call after changing config on the server to push a BE update to clients. */
    public void markUpdated() {
        if (this.level == null) return;

        // Only meaningful server-side; client calling this is a no-op.
        if (this.level.isClientSide()) return;

        long now = this.level.getGameTime();
        if (now == this.lastUpdateGameTime) return; // coalesce within same tick
        this.lastUpdateGameTime = now;

        BlockState st = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, st, st, 3);

        if (DEBUG) {
            LOGGER.debug("CosmicSpawner BE update sent @ {}", this.worldPosition);
        }
    }

    @Override
    public void setEntityId(EntityType<?> type, RandomSource random) {
        Level level = this.level;
        if (level != null) {
            this.spawner.setEntityId(type, level, random, this.worldPosition);
            this.spawnerEntityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type));
            this.clientSpawnerDirty = true;
            invalidatePreviewEntityCache();

            // If boss one-shot is enabled, changing entity should rearm it.
            if (this.bossOneShot) {
                this.bossHasSpawned = false;
            }

            this.setChanged();
            if (!level.isClientSide()) markUpdated();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();

        // Client: rebuild preview spawner state (rendered mob) whenever we load or receive updates.
        if (this.level != null && this.level.isClientSide()) {
            this.clientSpawnerDirty = true;
            ensureClientPreviewConfigured();
            return;
        }

        // Server: after chunk load, re-apply the configured entity type into the BaseSpawner.
        if (this.level instanceof net.minecraft.server.level.ServerLevel sl) {
            if (this.spawnerEntityId != null && !this.spawnerEntityId.equalsIgnoreCase("none")) {
                ResourceLocation key = ResourceLocation.tryParse(this.spawnerEntityId);
                if (key == null) return;

                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
                if (type == null) return;

                this.spawner.setEntityId(type, sl, sl.getRandom(), this.worldPosition);
                invalidatePreviewEntityCache();
            }
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

    private void setInt(@Nullable Field f, int value) {
        if (f == null) {
            if (DEBUG) LOGGER.debug("CosmicSpawner: config field missing; cannot set value {}", value);
            return;
        }
        try {
            f.setInt(this.spawner, value);
        } catch (Throwable t) {
            LOGGER.warn("CosmicSpawner: failed to set spawner field {}", f.getName(), t);
            return;
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

    private int getInt(@Nullable Field f, int fallback) {
        if (f == null) return fallback;
        try {
            return f.getInt(this.spawner);
        } catch (Throwable t) {
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

        input.child("SpawnerPreset").ifPresent(child -> this.setSpawnerPresetInternal(CosmicSpawnerPreset.load(child)));

        // Boss one-shot persisted flags
// Boss one-shot persisted flags
        this.bossOneShot = input.getBooleanOr("BossOneShot", false);
        this.bossHasSpawned = input.getBooleanOr("BossHasSpawned", false);
        this.spawnerMobCap = Math.max(0, input.getIntOr("SpawnerMobCap", 0));
        this.clientSpawnerDirty = true;

        restoreSpawnerFieldsFromSavedNbt(input);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);

        output.putString("SpawnerEntityId", this.spawnerEntityId);
        if (this.spawnerPreset != null) {
            this.spawnerPreset.save(output.child("SpawnerPreset"));
        }

        // Boss one-shot persisted flags
        output.putBoolean("BossOneShot", this.bossOneShot);
        output.putBoolean("BossHasSpawned", this.bossHasSpawned);
        output.putInt("SpawnerMobCap", this.spawnerMobCap);

        // Vanilla/BaseSpawner persisted fields (Delay/MinSpawnDelay/MaxSpawnDelay/SpawnCount/etc + SpawnData)
        this.spawner.save(output);
    }

    private void restoreSpawnerFieldsFromSavedNbt(net.minecraft.world.level.storage.ValueInput input) {
        // If keys are missing, keep whatever the spawner currently has (vanilla defaults).
        setIntRaw(F_SPAWN_DELAY, input.getInt("Delay").orElse(getIntRaw(F_SPAWN_DELAY)));
        setIntRaw(F_MIN_SPAWN_DELAY, input.getInt("MinSpawnDelay").orElse(getIntRaw(F_MIN_SPAWN_DELAY)));
        setIntRaw(F_MAX_SPAWN_DELAY, input.getInt("MaxSpawnDelay").orElse(getIntRaw(F_MAX_SPAWN_DELAY)));
        setIntRaw(F_SPAWN_COUNT, input.getInt("SpawnCount").orElse(getIntRaw(F_SPAWN_COUNT)));
        setIntRaw(F_MAX_NEARBY_ENTITIES, input.getInt("MaxNearbyEntities").orElse(getIntRaw(F_MAX_NEARBY_ENTITIES)));
        setIntRaw(F_REQUIRED_PLAYER_RANGE, input.getInt("RequiredPlayerRange").orElse(getIntRaw(F_REQUIRED_PLAYER_RANGE)));
        setIntRaw(F_SPAWN_RANGE, input.getInt("SpawnRange").orElse(getIntRaw(F_SPAWN_RANGE)));
    }

    private int getIntRaw(@Nullable Field field) {
        if (field == null) return 0;
        try {
            return (int) field.get(this.spawner);
        } catch (Throwable t) {
            if (DEBUG) LOGGER.debug("Failed to read spawner field {}", field.getName(), t);
            return 0;
        }
    }

    /** Sets a BaseSpawner int field without marking the chunk dirty (used only while loading). */
    private void setIntRaw(@Nullable Field field, int value) {
        if (field == null) return;
        try {
            field.setInt(this.spawner, value);
        } catch (Throwable t) {
            if (DEBUG) LOGGER.debug("Failed to set spawner field {}", field.getName(), t);
        }
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
    public void handleUpdateTag(net.minecraft.world.level.storage.ValueInput input) {
        this.loadWithComponents(input);
        this.clientSpawnerDirty = true;
        if (this.level != null && this.level.isClientSide()) {
            ensureClientPreviewConfigured();
        }
    }

    @Override
    public void onDataPacket(Connection net, net.minecraft.world.level.storage.ValueInput valueInput) {
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
        invalidatePreviewEntityCache();
        this.clientSpawnerDirty = false;
    }

    
    private String oneShotSpawnTag() {
        return "cosmic_spawner_" + this.worldPosition.getX() + "_" + this.worldPosition.getY() + "_" + this.worldPosition.getZ();
    }

    private void applySpawnTagToSpawnerData() {
        CompoundTag base = new CompoundTag();
        ResourceLocation entityId = this.spawnerPreset != null ? this.spawnerPreset.getEntityTypeId() : ResourceLocation.tryParse(this.spawnerEntityId);
        if (entityId != null) {
            base.putString("id", entityId.toString());
        }

        List<String> tags = new java.util.ArrayList<>();
        base.getList("Tags").ifPresent(in -> {
            for (int i = 0; i < in.size(); i++) {
                in.getString(i).ifPresent(tags::add);
            }
        });
        String marker = oneShotSpawnTag();
        if (!tags.contains(marker)) {
            var out = new ListTag();
            for (String t : tags) out.add(net.minecraft.nbt.StringTag.valueOf(t));
            out.add(net.minecraft.nbt.StringTag.valueOf(marker));
            base.put("Tags", out);
        }

        this.spawner.setNextSpawnDataPublic(this.level, this.worldPosition, forceFullBrightRules(new SpawnData(base, Optional.empty(), Optional.empty())));
        invalidatePreviewEntityCache();
    }

    private int countTaggedEntities(net.minecraft.server.level.ServerLevel sl) {
        String marker = oneShotSpawnTag();
        int count = 0;
        for (Entity e : sl.getAllEntities()) {
            if (e.getTags().contains(marker) && e.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private void applyPresetToTaggedEntities(net.minecraft.server.level.ServerLevel sl) {
        if (this.spawnerPreset == null) return;
        String marker = oneShotSpawnTag();
        ResourceLocation rl = this.spawnerPreset.getEntityTypeId();
        for (Entity e : sl.getAllEntities()) {
            if (e.getTags().contains(marker) && BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).equals(rl)) {
                this.spawnerPreset.applyToEntity(e);
            }
        }
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

        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return;

        // Boss one-shot: if already spawned once, do nothing (prevents repeat spawns on reload edge cases).
        if (be.bossOneShot && be.bossHasSpawned) {
            return;
        }

        // Tag all Cosmic Spawner mobs so boss/cap logic counts only mobs born from this block.
        if (be.bossOneShot || be.spawnerMobCap > 0 || be.spawnerPreset != null) {
            be.applySpawnTagToSpawnerData();
            if (be.oneShotTaggedCount < 0) be.oneShotTaggedCount = be.countTaggedEntities(sl);
        }

        int before = be.countTaggedEntities(sl);
        if (be.spawnerMobCap > 0 && before >= be.spawnerMobCap) {
            be.applyPresetToTaggedEntities(sl);
            return;
        }

        int originalSpawnCount = be.getSpawnerSpawnCount();
        boolean limitedSpawnCount = false;
        int spawnLimit = originalSpawnCount;
        if (be.bossOneShot) {
            spawnLimit = Math.min(spawnLimit, 1);
        }
        if (be.spawnerMobCap > 0 && originalSpawnCount > 0) {
            int remaining = Math.max(0, be.spawnerMobCap - before);
            if (remaining <= 0) {
                be.applyPresetToTaggedEntities(sl);
                return;
            }
            spawnLimit = Math.min(spawnLimit, remaining);
        }
        if (spawnLimit > 0 && spawnLimit < originalSpawnCount) {
            be.setSpawnerSpawnCount(spawnLimit);
            limitedSpawnCount = true;
        }

        be.spawner.serverTick(sl, pos);

        if (limitedSpawnCount) {
            be.setSpawnerSpawnCount(originalSpawnCount);
        }

        be.applyPresetToTaggedEntities(sl);

        if (be.bossOneShot) {
            int after = be.countTaggedEntities(sl);
            if (after > Math.max(0, be.oneShotTaggedCount)) {
                be.bossHasSpawned = true;
                be.setChanged();
                be.markUpdated();

                // Self-destruct after the first successful spawn.
                sl.removeBlock(pos, false);
            } else {
                be.oneShotTaggedCount = after;
            }
        }
    }

    // ----------------------------
    // Helpers for commands / UI
    // ----------------------------

    public void applySpawnerEntity(Level level, EntityType<?> type) {
        this.spawner.setEntityId(type, level, level.getRandom(), this.worldPosition);
        this.spawnerEntityId = String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(type));
        this.clientSpawnerDirty = true;
        invalidatePreviewEntityCache();

        // If boss one-shot is enabled, changing entity should rearm it.
        if (this.bossOneShot) {
            this.bossHasSpawned = false;
        }

        this.setChanged();
        if (!level.isClientSide()) markUpdated();
    }

    public void clearSpawnerEntity(Level level) {
        this.spawnerEntityId = "none";
        this.clientSpawnerDirty = true;
        invalidatePreviewEntityCache();

        // Clearing also rearms (safe default).
        if (this.bossOneShot) {
            this.bossHasSpawned = false;
        }

        this.setChanged();
        if (!level.isClientSide()) markUpdated();
    }


    public CosmicSpawnerPreset getSpawnerPreset() { return this.spawnerPreset; }

    public void setSpawnerPreset(CosmicSpawnerPreset preset) {
        setSpawnerPresetInternal(preset);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) markUpdated();
    }

    private void setSpawnerPresetInternal(CosmicSpawnerPreset preset) {
        this.spawnerPreset = preset;
        this.spawnerEntityId = preset.getEntityTypeId().toString();
        if (this.level != null) {
            BuiltInRegistries.ENTITY_TYPE.getOptional(preset.getEntityTypeId()).ifPresent(type -> this.spawner.setEntityId(type, this.level, this.level.getRandom(), this.worldPosition));
        }
        this.clientSpawnerDirty = true;
        invalidatePreviewEntityCache();
    }

    public void clearSpawnerPreset() {
        this.spawnerPreset = null;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) markUpdated();
    }
    private static SpawnData forceFullBrightRules(SpawnData original) {
        CompoundTag entityCopy = original.getEntityToSpawn().copy();
        var anyLight = new net.minecraft.util.InclusiveRange<>(0, 15);
        SpawnData.CustomSpawnRules rules = new SpawnData.CustomSpawnRules(anyLight, anyLight);
        return new SpawnData(entityCopy, Optional.of(rules), original.getEquipment());
    }
}