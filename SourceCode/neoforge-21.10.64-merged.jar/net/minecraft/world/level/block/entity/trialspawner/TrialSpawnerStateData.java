package net.minecraft.world.level.block.entity.trialspawner;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class TrialSpawnerStateData {
    private static final String TAG_SPAWN_DATA = "spawn_data";
    private static final String TAG_NEXT_MOB_SPAWNS_AT = "next_mob_spawns_at";
    private static final int DELAY_BETWEEN_PLAYER_SCANS = 20;
    private static final int TRIAL_OMEN_PER_BAD_OMEN_LEVEL = 18000;
    final Set<UUID> detectedPlayers = new HashSet<>();
    final Set<UUID> currentMobs = new HashSet<>();
    long cooldownEndsAt;
    long nextMobSpawnsAt;
    int totalMobsSpawned;
    Optional<SpawnData> nextSpawnData = Optional.empty();
    Optional<ResourceKey<LootTable>> ejectingLootTable = Optional.empty();
    @Nullable
    private Entity displayEntity;
    @Nullable
    private WeightedList<ItemStack> dispensing;
    double spin;
    double oSpin;

    public TrialSpawnerStateData.Packed pack() {
        return new TrialSpawnerStateData.Packed(
            Set.copyOf(this.detectedPlayers),
            Set.copyOf(this.currentMobs),
            this.cooldownEndsAt,
            this.nextMobSpawnsAt,
            this.totalMobsSpawned,
            this.nextSpawnData,
            this.ejectingLootTable
        );
    }

    public void apply(TrialSpawnerStateData.Packed data) {
        this.detectedPlayers.clear();
        this.detectedPlayers.addAll(data.detectedPlayers);
        this.currentMobs.clear();
        this.currentMobs.addAll(data.currentMobs);
        this.cooldownEndsAt = data.cooldownEndsAt;
        this.nextMobSpawnsAt = data.nextMobSpawnsAt;
        this.totalMobsSpawned = data.totalMobsSpawned;
        this.nextSpawnData = data.nextSpawnData;
        this.ejectingLootTable = data.ejectingLootTable;
    }

    public void reset() {
        this.currentMobs.clear();
        this.nextSpawnData = Optional.empty();
        this.resetStatistics();
    }

    public void resetStatistics() {
        this.detectedPlayers.clear();
        this.totalMobsSpawned = 0;
        this.nextMobSpawnsAt = 0L;
        this.cooldownEndsAt = 0L;
    }

    public boolean hasMobToSpawn(TrialSpawner spawner, RandomSource random) {
        boolean flag = this.getOrCreateNextSpawnData(spawner, random).getEntityToSpawn().getString("id").isPresent();
        return flag || !spawner.activeConfig().spawnPotentialsDefinition().isEmpty();
    }

    public boolean hasFinishedSpawningAllMobs(TrialSpawnerConfig config, int players) {
        return this.totalMobsSpawned >= config.calculateTargetTotalMobs(players);
    }

    public boolean haveAllCurrentMobsDied() {
        return this.currentMobs.isEmpty();
    }

    public boolean isReadyToSpawnNextMob(ServerLevel level, TrialSpawnerConfig config, int players) {
        return level.getGameTime() >= this.nextMobSpawnsAt && this.currentMobs.size() < config.calculateTargetSimultaneousMobs(players);
    }

    public int countAdditionalPlayers(BlockPos pos) {
        if (this.detectedPlayers.isEmpty()) {
            Util.logAndPauseIfInIde("Trial Spawner at " + pos + " has no detected players");
        }

        return Math.max(0, this.detectedPlayers.size() - 1);
    }

    public void tryDetectPlayers(ServerLevel level, BlockPos pos, TrialSpawner spawner) {
        boolean flag = (pos.asLong() + level.getGameTime()) % 20L != 0L;
        if (!flag) {
            if (!spawner.getState().equals(TrialSpawnerState.COOLDOWN) || !spawner.isOminous()) {
                List<UUID> list = spawner.getPlayerDetector()
                    .detect(level, spawner.getEntitySelector(), pos, spawner.getRequiredPlayerRange(), true);
                boolean flag1;
                if (!spawner.isOminous() && !list.isEmpty()) {
                    Optional<Pair<Player, Holder<MobEffect>>> optional = findPlayerWithOminousEffect(level, list);
                    optional.ifPresent(p_427219_ -> {
                        Player player = p_427219_.getFirst();
                        if (p_427219_.getSecond() == MobEffects.BAD_OMEN) {
                            transformBadOmenIntoTrialOmen(player);
                        }

                        level.levelEvent(3020, BlockPos.containing(player.getEyePosition()), 0);
                        spawner.applyOminous(level, pos);
                    });
                    flag1 = optional.isPresent();
                } else {
                    flag1 = false;
                }

                if (!spawner.getState().equals(TrialSpawnerState.COOLDOWN) || flag1) {
                    boolean flag2 = spawner.getStateData().detectedPlayers.isEmpty();
                    List<UUID> list1 = flag2
                        ? list
                        : spawner.getPlayerDetector().detect(level, spawner.getEntitySelector(), pos, spawner.getRequiredPlayerRange(), false);
                    if (this.detectedPlayers.addAll(list1)) {
                        this.nextMobSpawnsAt = Math.max(level.getGameTime() + 40L, this.nextMobSpawnsAt);
                        if (!flag1) {
                            int i = spawner.isOminous() ? 3019 : 3013;
                            level.levelEvent(i, pos, this.detectedPlayers.size());
                        }
                    }
                }
            }
        }
    }

    private static Optional<Pair<Player, Holder<MobEffect>>> findPlayerWithOminousEffect(ServerLevel level, List<UUID> players) {
        Player player = null;

        for (UUID uuid : players) {
            Player player1 = level.getPlayerByUUID(uuid);
            if (player1 != null) {
                Holder<MobEffect> holder = MobEffects.TRIAL_OMEN;
                if (player1.hasEffect(holder)) {
                    return Optional.of(Pair.of(player1, holder));
                }

                if (player1.hasEffect(MobEffects.BAD_OMEN)) {
                    player = player1;
                }
            }
        }

        return Optional.ofNullable(player).map(p_421755_ -> Pair.of(p_421755_, MobEffects.BAD_OMEN));
    }

    public void resetAfterBecomingOminous(TrialSpawner spawner, ServerLevel level) {
        this.currentMobs.stream().map(level::getEntity).forEach(p_421890_ -> {
            if (p_421890_ != null) {
                level.levelEvent(3012, p_421890_.blockPosition(), TrialSpawner.FlameParticle.NORMAL.encode());
                if (p_421890_ instanceof Mob mob) {
                    mob.dropPreservedEquipment(level);
                }

                p_421890_.remove(Entity.RemovalReason.DISCARDED);
            }
        });
        if (!spawner.ominousConfig().spawnPotentialsDefinition().isEmpty()) {
            this.nextSpawnData = Optional.empty();
        }

        this.totalMobsSpawned = 0;
        this.currentMobs.clear();
        this.nextMobSpawnsAt = level.getGameTime() + spawner.ominousConfig().ticksBetweenSpawn();
        spawner.markUpdated();
        this.cooldownEndsAt = level.getGameTime() + spawner.ominousConfig().ticksBetweenItemSpawners();
    }

    private static void transformBadOmenIntoTrialOmen(Player player) {
        MobEffectInstance mobeffectinstance = player.getEffect(MobEffects.BAD_OMEN);
        if (mobeffectinstance != null) {
            int i = mobeffectinstance.getAmplifier() + 1;
            int j = 18000 * i;
            player.removeEffect(MobEffects.BAD_OMEN);
            player.addEffect(new MobEffectInstance(MobEffects.TRIAL_OMEN, j, 0));
        }
    }

    public boolean isReadyToOpenShutter(ServerLevel level, float delay, int targetCooldownLength) {
        long i = this.cooldownEndsAt - targetCooldownLength;
        return (float)level.getGameTime() >= (float)i + delay;
    }

    public boolean isReadyToEjectItems(ServerLevel level, float delay, int targetCooldownLength) {
        long i = this.cooldownEndsAt - targetCooldownLength;
        return (float)(level.getGameTime() - i) % delay == 0.0F;
    }

    public boolean isCooldownFinished(ServerLevel level) {
        return level.getGameTime() >= this.cooldownEndsAt;
    }

    protected SpawnData getOrCreateNextSpawnData(TrialSpawner spawner, RandomSource random) {
        if (this.nextSpawnData.isPresent()) {
            return this.nextSpawnData.get();
        } else {
            WeightedList<SpawnData> weightedlist = spawner.activeConfig().spawnPotentialsDefinition();
            Optional<SpawnData> optional = weightedlist.isEmpty() ? this.nextSpawnData : weightedlist.getRandom(random);
            this.nextSpawnData = Optional.of(optional.orElseGet(SpawnData::new));
            spawner.markUpdated();
            return this.nextSpawnData.get();
        }
    }

    @Nullable
    public Entity getOrCreateDisplayEntity(TrialSpawner spawner, Level level, TrialSpawnerState spawnerState) {
        if (!spawnerState.hasSpinningMob()) {
            return null;
        } else {
            if (this.displayEntity == null) {
                CompoundTag compoundtag = this.getOrCreateNextSpawnData(spawner, level.getRandom()).getEntityToSpawn();
                if (compoundtag.getString("id").isPresent()) {
                    this.displayEntity = EntityType.loadEntityRecursive(compoundtag, level, EntitySpawnReason.TRIAL_SPAWNER, Function.identity());
                }
            }

            return this.displayEntity;
        }
    }

    public CompoundTag getUpdateTag(TrialSpawnerState spawnerState) {
        CompoundTag compoundtag = new CompoundTag();
        if (spawnerState == TrialSpawnerState.ACTIVE) {
            compoundtag.putLong("next_mob_spawns_at", this.nextMobSpawnsAt);
        }

        this.nextSpawnData.ifPresent(p_422694_ -> compoundtag.store("spawn_data", SpawnData.CODEC, p_422694_));
        return compoundtag;
    }

    public double getSpin() {
        return this.spin;
    }

    public double getOSpin() {
        return this.oSpin;
    }

    WeightedList<ItemStack> getDispensingItems(ServerLevel level, TrialSpawnerConfig config, BlockPos pos) {
        if (this.dispensing != null) {
            return this.dispensing;
        } else {
            LootTable loottable = level.getServer().reloadableRegistries().getLootTable(config.itemsToDropWhenOminous());
            LootParams lootparams = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
            long i = lowResolutionPosition(level, pos);
            ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams, i);
            if (objectarraylist.isEmpty()) {
                return WeightedList.of();
            } else {
                WeightedList.Builder<ItemStack> builder = WeightedList.builder();

                for (ItemStack itemstack : objectarraylist) {
                    builder.add(itemstack.copyWithCount(1), itemstack.getCount());
                }

                this.dispensing = builder.build();
                return this.dispensing;
            }
        }
    }

    private static long lowResolutionPosition(ServerLevel level, BlockPos pos) {
        BlockPos blockpos = new BlockPos(Mth.floor(pos.getX() / 30.0F), Mth.floor(pos.getY() / 20.0F), Mth.floor(pos.getZ() / 30.0F));
        return level.getSeed() + blockpos.asLong();
    }

    public record Packed(
        Set<UUID> detectedPlayers,
        Set<UUID> currentMobs,
        long cooldownEndsAt,
        long nextMobSpawnsAt,
        int totalMobsSpawned,
        Optional<SpawnData> nextSpawnData,
        Optional<ResourceKey<LootTable>> ejectingLootTable
    ) {
        public static final MapCodec<TrialSpawnerStateData.Packed> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_421723_ -> p_421723_.group(
                    UUIDUtil.CODEC_SET.lenientOptionalFieldOf("registered_players", Set.of()).forGetter(TrialSpawnerStateData.Packed::detectedPlayers),
                    UUIDUtil.CODEC_SET.lenientOptionalFieldOf("current_mobs", Set.of()).forGetter(TrialSpawnerStateData.Packed::currentMobs),
                    Codec.LONG.lenientOptionalFieldOf("cooldown_ends_at", 0L).forGetter(TrialSpawnerStateData.Packed::cooldownEndsAt),
                    Codec.LONG.lenientOptionalFieldOf("next_mob_spawns_at", 0L).forGetter(TrialSpawnerStateData.Packed::nextMobSpawnsAt),
                    Codec.intRange(0, Integer.MAX_VALUE)
                        .lenientOptionalFieldOf("total_mobs_spawned", 0)
                        .forGetter(TrialSpawnerStateData.Packed::totalMobsSpawned),
                    SpawnData.CODEC.lenientOptionalFieldOf("spawn_data").forGetter(TrialSpawnerStateData.Packed::nextSpawnData),
                    LootTable.KEY_CODEC.lenientOptionalFieldOf("ejecting_loot_table").forGetter(TrialSpawnerStateData.Packed::ejectingLootTable)
                )
                .apply(p_421723_, TrialSpawnerStateData.Packed::new)
        );
    }
}
