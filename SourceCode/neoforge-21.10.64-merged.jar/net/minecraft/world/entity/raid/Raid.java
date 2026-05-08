package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.SectionPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class Raid {
    public static final SpawnPlacementType RAVAGER_SPAWN_PLACEMENT_TYPE = SpawnPlacements.getPlacementType(EntityType.RAVAGER);
    public static final MapCodec<Raid> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_400925_ -> p_400925_.group(
                Codec.BOOL.fieldOf("started").forGetter(p_400913_ -> p_400913_.started),
                Codec.BOOL.fieldOf("active").forGetter(p_400917_ -> p_400917_.active),
                Codec.LONG.fieldOf("ticks_active").forGetter(p_400918_ -> p_400918_.ticksActive),
                Codec.INT.fieldOf("raid_omen_level").forGetter(p_400924_ -> p_400924_.raidOmenLevel),
                Codec.INT.fieldOf("groups_spawned").forGetter(p_400921_ -> p_400921_.groupsSpawned),
                Codec.INT.fieldOf("cooldown_ticks").forGetter(p_400926_ -> p_400926_.raidCooldownTicks),
                Codec.INT.fieldOf("post_raid_ticks").forGetter(p_400914_ -> p_400914_.postRaidTicks),
                Codec.FLOAT.fieldOf("total_health").forGetter(p_400919_ -> p_400919_.totalHealth),
                Codec.INT.fieldOf("group_count").forGetter(p_400915_ -> p_400915_.numGroups),
                Raid.RaidStatus.CODEC.fieldOf("status").forGetter(p_400927_ -> p_400927_.status),
                BlockPos.CODEC.fieldOf("center").forGetter(p_400923_ -> p_400923_.center),
                UUIDUtil.CODEC_SET.fieldOf("heroes_of_the_village").forGetter(p_400920_ -> p_400920_.heroesOfTheVillage)
            )
            .apply(p_400925_, Raid::new)
    );
    private static final int ALLOW_SPAWNING_WITHIN_VILLAGE_SECONDS_THRESHOLD = 7;
    private static final int SECTION_RADIUS_FOR_FINDING_NEW_VILLAGE_CENTER = 2;
    private static final int VILLAGE_SEARCH_RADIUS = 32;
    private static final int RAID_TIMEOUT_TICKS = 48000;
    private static final int NUM_SPAWN_ATTEMPTS = 5;
    private static final Component OMINOUS_BANNER_PATTERN_NAME = Component.translatable("block.minecraft.ominous_banner");
    private static final String RAIDERS_REMAINING = "event.minecraft.raid.raiders_remaining";
    public static final int VILLAGE_RADIUS_BUFFER = 16;
    private static final int POST_RAID_TICK_LIMIT = 40;
    private static final int DEFAULT_PRE_RAID_TICKS = 300;
    public static final int MAX_NO_ACTION_TIME = 2400;
    public static final int MAX_CELEBRATION_TICKS = 600;
    private static final int OUTSIDE_RAID_BOUNDS_TIMEOUT = 30;
    public static final int TICKS_PER_DAY = 24000;
    public static final int DEFAULT_MAX_RAID_OMEN_LEVEL = 5;
    private static final int LOW_MOB_THRESHOLD = 2;
    private static final Component RAID_NAME_COMPONENT = Component.translatable("event.minecraft.raid");
    private static final Component RAID_BAR_VICTORY_COMPONENT = Component.translatable("event.minecraft.raid.victory.full");
    private static final Component RAID_BAR_DEFEAT_COMPONENT = Component.translatable("event.minecraft.raid.defeat.full");
    private static final int HERO_OF_THE_VILLAGE_DURATION = 48000;
    private static final int VALID_RAID_RADIUS = 96;
    public static final int VALID_RAID_RADIUS_SQR = 9216;
    public static final int RAID_REMOVAL_THRESHOLD_SQR = 12544;
    private final Map<Integer, Raider> groupToLeaderMap = Maps.newHashMap();
    private final Map<Integer, Set<Raider>> groupRaiderMap = Maps.newHashMap();
    private final Set<UUID> heroesOfTheVillage = Sets.newHashSet();
    private long ticksActive;
    private BlockPos center;
    private boolean started;
    private float totalHealth;
    private int raidOmenLevel;
    private boolean active;
    private int groupsSpawned;
    private final ServerBossEvent raidEvent = new ServerBossEvent(RAID_NAME_COMPONENT, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private int postRaidTicks;
    private int raidCooldownTicks;
    private final RandomSource random = RandomSource.create();
    private final int numGroups;
    private Raid.RaidStatus status;
    private int celebrationTicks;
    private Optional<BlockPos> waveSpawnPos = Optional.empty();

    public Raid(BlockPos center, Difficulty difficulty) {
        this.active = true;
        this.raidCooldownTicks = 300;
        this.raidEvent.setProgress(0.0F);
        this.center = center;
        this.numGroups = this.getNumGroups(difficulty);
        this.status = Raid.RaidStatus.ONGOING;
    }

    private Raid(
        boolean started,
        boolean active,
        long ticksActive,
        int raidOmenLevel,
        int groupsSpawned,
        int raidCooldownTicks,
        int postRaidTicks,
        float totalHealth,
        int numGroups,
        Raid.RaidStatus status,
        BlockPos center,
        Set<UUID> heroesOfTheVillage
    ) {
        this.started = started;
        this.active = active;
        this.ticksActive = ticksActive;
        this.raidOmenLevel = raidOmenLevel;
        this.groupsSpawned = groupsSpawned;
        this.raidCooldownTicks = raidCooldownTicks;
        this.postRaidTicks = postRaidTicks;
        this.totalHealth = totalHealth;
        this.center = center;
        this.numGroups = numGroups;
        this.status = status;
        this.heroesOfTheVillage.addAll(heroesOfTheVillage);
    }

    public boolean isOver() {
        return this.isVictory() || this.isLoss();
    }

    public boolean isBetweenWaves() {
        return this.hasFirstWaveSpawned() && this.getTotalRaidersAlive() == 0 && this.raidCooldownTicks > 0;
    }

    public boolean hasFirstWaveSpawned() {
        return this.groupsSpawned > 0;
    }

    public boolean isStopped() {
        return this.status == Raid.RaidStatus.STOPPED;
    }

    public boolean isVictory() {
        return this.status == Raid.RaidStatus.VICTORY;
    }

    public boolean isLoss() {
        return this.status == Raid.RaidStatus.LOSS;
    }

    public float getTotalHealth() {
        return this.totalHealth;
    }

    public Set<Raider> getAllRaiders() {
        Set<Raider> set = Sets.newHashSet();

        for (Set<Raider> set1 : this.groupRaiderMap.values()) {
            set.addAll(set1);
        }

        return set;
    }

    public boolean isStarted() {
        return this.started;
    }

    public int getGroupsSpawned() {
        return this.groupsSpawned;
    }

    private Predicate<ServerPlayer> validPlayer() {
        return p_427142_ -> {
            BlockPos blockpos = p_427142_.blockPosition();
            return p_427142_.isAlive() && p_427142_.level().getRaidAt(blockpos) == this;
        };
    }

    private void updatePlayers(ServerLevel level) {
        Set<ServerPlayer> set = Sets.newHashSet(this.raidEvent.getPlayers());
        List<ServerPlayer> list = level.getPlayers(this.validPlayer());

        for (ServerPlayer serverplayer : list) {
            if (!set.contains(serverplayer)) {
                this.raidEvent.addPlayer(serverplayer);
            }
        }

        for (ServerPlayer serverplayer1 : set) {
            if (!list.contains(serverplayer1)) {
                this.raidEvent.removePlayer(serverplayer1);
            }
        }
    }

    public int getMaxRaidOmenLevel() {
        return 5;
    }

    public int getRaidOmenLevel() {
        return this.raidOmenLevel;
    }

    public void setRaidOmenLevel(int raidOmenLevel) {
        this.raidOmenLevel = raidOmenLevel;
    }

    public boolean absorbRaidOmen(ServerPlayer player) {
        MobEffectInstance mobeffectinstance = player.getEffect(MobEffects.RAID_OMEN);
        if (mobeffectinstance == null) {
            return false;
        } else {
            this.raidOmenLevel = this.raidOmenLevel + mobeffectinstance.getAmplifier() + 1;
            this.raidOmenLevel = Mth.clamp(this.raidOmenLevel, 0, this.getMaxRaidOmenLevel());
            if (!this.hasFirstWaveSpawned()) {
                player.awardStat(Stats.RAID_TRIGGER);
                CriteriaTriggers.RAID_OMEN.trigger(player);
            }

            return true;
        }
    }

    public void stop() {
        this.active = false;
        this.raidEvent.removeAllPlayers();
        this.status = Raid.RaidStatus.STOPPED;
    }

    public void tick(ServerLevel level) {
        if (!this.isStopped()) {
            if (this.status == Raid.RaidStatus.ONGOING) {
                boolean flag = this.active;
                this.active = level.hasChunkAt(this.center);
                if (level.getDifficulty() == Difficulty.PEACEFUL) {
                    this.stop();
                    return;
                }

                if (flag != this.active) {
                    this.raidEvent.setVisible(this.active);
                }

                if (!this.active) {
                    return;
                }

                if (!level.isVillage(this.center)) {
                    this.moveRaidCenterToNearbyVillageSection(level);
                }

                if (!level.isVillage(this.center)) {
                    if (this.groupsSpawned > 0) {
                        this.status = Raid.RaidStatus.LOSS;
                    } else {
                        this.stop();
                    }
                }

                this.ticksActive++;
                if (this.ticksActive >= 48000L) {
                    this.stop();
                    return;
                }

                int i = this.getTotalRaidersAlive();
                if (i == 0 && this.hasMoreWaves()) {
                    if (this.raidCooldownTicks <= 0) {
                        if (this.raidCooldownTicks == 0 && this.groupsSpawned > 0) {
                            this.raidCooldownTicks = 300;
                            this.raidEvent.setName(RAID_NAME_COMPONENT);
                            return;
                        }
                    } else {
                        boolean flag1 = this.waveSpawnPos.isPresent();
                        boolean flag2 = !flag1 && this.raidCooldownTicks % 5 == 0;
                        if (flag1 && !level.isPositionEntityTicking(this.waveSpawnPos.get())) {
                            flag2 = true;
                        }

                        if (flag2) {
                            this.waveSpawnPos = this.getValidSpawnPos(level);
                        }

                        if (this.raidCooldownTicks == 300 || this.raidCooldownTicks % 20 == 0) {
                            this.updatePlayers(level);
                        }

                        this.raidCooldownTicks--;
                        this.raidEvent.setProgress(Mth.clamp((300 - this.raidCooldownTicks) / 300.0F, 0.0F, 1.0F));
                    }
                }

                if (this.ticksActive % 20L == 0L) {
                    this.updatePlayers(level);
                    this.updateRaiders(level);
                    if (i > 0) {
                        if (i <= 2) {
                            this.raidEvent
                                .setName(RAID_NAME_COMPONENT.copy().append(" - ").append(Component.translatable("event.minecraft.raid.raiders_remaining", i)));
                        } else {
                            this.raidEvent.setName(RAID_NAME_COMPONENT);
                        }
                    } else {
                        this.raidEvent.setName(RAID_NAME_COMPONENT);
                    }
                }

                if (SharedConstants.DEBUG_RAIDS) {
                    this.raidEvent
                        .setName(
                            RAID_NAME_COMPONENT.copy()
                                .append(" wave: ")
                                .append(this.groupsSpawned + "")
                                .append(CommonComponents.SPACE)
                                .append("Raiders alive: ")
                                .append(this.getTotalRaidersAlive() + "")
                                .append(CommonComponents.SPACE)
                                .append(this.getHealthOfLivingRaiders() + "")
                                .append(" / ")
                                .append(this.totalHealth + "")
                                .append(" Is bonus? ")
                                .append((this.hasBonusWave() && this.hasSpawnedBonusWave()) + "")
                                .append(" Status: ")
                                .append(this.status.getSerializedName())
                        );
                }

                boolean flag3 = false;
                int j = 0;

                while (this.shouldSpawnGroup()) {
                    BlockPos blockpos = this.waveSpawnPos.orElseGet(() -> this.findRandomSpawnPos(level, 20));
                    if (blockpos != null) {
                        this.started = true;
                        this.spawnGroup(level, blockpos);
                        if (!flag3) {
                            this.playSound(level, blockpos);
                            flag3 = true;
                        }
                    } else {
                        j++;
                    }

                    if (j > 5) {
                        this.stop();
                        break;
                    }
                }

                if (this.isStarted() && !this.hasMoreWaves() && i == 0) {
                    if (this.postRaidTicks < 40) {
                        this.postRaidTicks++;
                    } else {
                        this.status = Raid.RaidStatus.VICTORY;

                        for (UUID uuid : this.heroesOfTheVillage) {
                            Entity entity = level.getEntity(uuid);
                            if (entity instanceof LivingEntity livingentity && !entity.isSpectator()) {
                                livingentity.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 48000, this.raidOmenLevel - 1, false, false, true));
                                if (livingentity instanceof ServerPlayer serverplayer) {
                                    serverplayer.awardStat(Stats.RAID_WIN);
                                    CriteriaTriggers.RAID_WIN.trigger(serverplayer);
                                }
                            }
                        }
                    }
                }

                this.setDirty(level);
            } else if (this.isOver()) {
                this.celebrationTicks++;
                if (this.celebrationTicks >= 600) {
                    this.stop();
                    return;
                }

                if (this.celebrationTicks % 20 == 0) {
                    this.updatePlayers(level);
                    this.raidEvent.setVisible(true);
                    if (this.isVictory()) {
                        this.raidEvent.setProgress(0.0F);
                        this.raidEvent.setName(RAID_BAR_VICTORY_COMPONENT);
                    } else {
                        this.raidEvent.setName(RAID_BAR_DEFEAT_COMPONENT);
                    }
                }
            }
        }
    }

    private void moveRaidCenterToNearbyVillageSection(ServerLevel level) {
        Stream<SectionPos> stream = SectionPos.cube(SectionPos.of(this.center), 2);
        stream.filter(level::isVillage)
            .map(SectionPos::center)
            .min(Comparator.comparingDouble(p_37766_ -> p_37766_.distSqr(this.center)))
            .ifPresent(this::setCenter);
    }

    private Optional<BlockPos> getValidSpawnPos(ServerLevel level) {
        BlockPos blockpos = this.findRandomSpawnPos(level, 8);
        return blockpos != null ? Optional.of(blockpos) : Optional.empty();
    }

    private boolean hasMoreWaves() {
        return this.hasBonusWave() ? !this.hasSpawnedBonusWave() : !this.isFinalWave();
    }

    private boolean isFinalWave() {
        return this.getGroupsSpawned() == this.numGroups;
    }

    private boolean hasBonusWave() {
        return this.raidOmenLevel > 1;
    }

    private boolean hasSpawnedBonusWave() {
        return this.getGroupsSpawned() > this.numGroups;
    }

    private boolean shouldSpawnBonusGroup() {
        return this.isFinalWave() && this.getTotalRaidersAlive() == 0 && this.hasBonusWave();
    }

    private void updateRaiders(ServerLevel level) {
        Iterator<Set<Raider>> iterator = this.groupRaiderMap.values().iterator();
        Set<Raider> set = Sets.newHashSet();

        while (iterator.hasNext()) {
            Set<Raider> set1 = iterator.next();

            for (Raider raider : set1) {
                BlockPos blockpos = raider.blockPosition();
                if (raider.isRemoved() || raider.level().dimension() != level.dimension() || this.center.distSqr(blockpos) >= 12544.0) {
                    set.add(raider);
                } else if (raider.tickCount > 600) {
                    if (level.getEntity(raider.getUUID()) == null) {
                        set.add(raider);
                    }

                    if (!level.isVillage(blockpos) && raider.getNoActionTime() > 2400) {
                        raider.setTicksOutsideRaid(raider.getTicksOutsideRaid() + 1);
                    }

                    if (raider.getTicksOutsideRaid() >= 30) {
                        set.add(raider);
                    }
                }
            }
        }

        for (Raider raider1 : set) {
            this.removeFromRaid(level, raider1, true);
            if (raider1.isPatrolLeader()) {
                this.removeLeader(raider1.getWave());
            }
        }
    }

    private void playSound(ServerLevel level, BlockPos pos) {
        float f = 13.0F;
        int i = 64;
        Collection<ServerPlayer> collection = this.raidEvent.getPlayers();
        long j = this.random.nextLong();

        for (ServerPlayer serverplayer : level.players()) {
            Vec3 vec3 = serverplayer.position();
            Vec3 vec31 = Vec3.atCenterOf(pos);
            double d0 = Math.sqrt((vec31.x - vec3.x) * (vec31.x - vec3.x) + (vec31.z - vec3.z) * (vec31.z - vec3.z));
            double d1 = vec3.x + 13.0 / d0 * (vec31.x - vec3.x);
            double d2 = vec3.z + 13.0 / d0 * (vec31.z - vec3.z);
            if (d0 <= 64.0 || collection.contains(serverplayer)) {
                serverplayer.connection
                    .send(new ClientboundSoundPacket(SoundEvents.RAID_HORN, SoundSource.NEUTRAL, d1, serverplayer.getY(), d2, 64.0F, 1.0F, j));
            }
        }
    }

    private void spawnGroup(ServerLevel level, BlockPos pos) {
        boolean flag = false;
        int i = this.groupsSpawned + 1;
        this.totalHealth = 0.0F;
        DifficultyInstance difficultyinstance = level.getCurrentDifficultyAt(pos);
        boolean flag1 = this.shouldSpawnBonusGroup();

        for (Raid.RaiderType raid$raidertype : Raid.RaiderType.VALUES) {
            int j = this.getDefaultNumSpawns(raid$raidertype, i, flag1)
                + this.getPotentialBonusSpawns(raid$raidertype, this.random, i, difficultyinstance, flag1);
            int k = 0;

            for (int l = 0; l < j; l++) {
                Raider raider = raid$raidertype.entityTypeSupplier.get().create(level, EntitySpawnReason.EVENT);
                if (raider == null) {
                    break;
                }

                if (!flag && raider.canBeLeader()) {
                    raider.setPatrolLeader(true);
                    this.setLeader(i, raider);
                    flag = true;
                }

                this.joinRaid(level, i, raider, pos, false);
                if (raid$raidertype.entityTypeSupplier.get() == EntityType.RAVAGER) {
                    Raider raider1 = null;
                    if (i == this.getNumGroups(Difficulty.NORMAL)) {
                        raider1 = EntityType.PILLAGER.create(level, EntitySpawnReason.EVENT);
                    } else if (i >= this.getNumGroups(Difficulty.HARD)) {
                        if (k == 0) {
                            raider1 = EntityType.EVOKER.create(level, EntitySpawnReason.EVENT);
                        } else {
                            raider1 = EntityType.VINDICATOR.create(level, EntitySpawnReason.EVENT);
                        }
                    }

                    k++;
                    if (raider1 != null) {
                        this.joinRaid(level, i, raider1, pos, false);
                        raider1.snapTo(pos, 0.0F, 0.0F);
                        raider1.startRiding(raider, false, false);
                    }
                }
            }
        }

        this.waveSpawnPos = Optional.empty();
        this.groupsSpawned++;
        this.updateBossbar();
        this.setDirty(level);
    }

    public void joinRaid(ServerLevel level, int wave, Raider raider, @Nullable BlockPos pos, boolean isRecruited) {
        boolean flag = this.addWaveMob(level, wave, raider);
        if (flag) {
            raider.setCurrentRaid(this);
            raider.setWave(wave);
            raider.setCanJoinRaid(true);
            raider.setTicksOutsideRaid(0);
            if (!isRecruited && pos != null) {
                raider.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                raider.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
                raider.applyRaidBuffs(level, wave, false);
                raider.setOnGround(true);
                level.addFreshEntityWithPassengers(raider);
            }
        }
    }

    public void updateBossbar() {
        this.raidEvent.setProgress(Mth.clamp(this.getHealthOfLivingRaiders() / this.totalHealth, 0.0F, 1.0F));
    }

    public float getHealthOfLivingRaiders() {
        float f = 0.0F;

        for (Set<Raider> set : this.groupRaiderMap.values()) {
            for (Raider raider : set) {
                f += raider.getHealth();
            }
        }

        return f;
    }

    private boolean shouldSpawnGroup() {
        return this.raidCooldownTicks == 0 && (this.groupsSpawned < this.numGroups || this.shouldSpawnBonusGroup()) && this.getTotalRaidersAlive() == 0;
    }

    public int getTotalRaidersAlive() {
        return this.groupRaiderMap.values().stream().mapToInt(Set::size).sum();
    }

    public void removeFromRaid(ServerLevel level, Raider raider, boolean wanderedOutOfRaid) {
        Set<Raider> set = this.groupRaiderMap.get(raider.getWave());
        if (set != null) {
            boolean flag = set.remove(raider);
            if (flag) {
                if (wanderedOutOfRaid) {
                    this.totalHealth = this.totalHealth - raider.getHealth();
                }

                raider.setCurrentRaid(null);
                this.updateBossbar();
                this.setDirty(level);
            }
        }
    }

    private void setDirty(ServerLevel level) {
        level.getRaids().setDirty();
    }

    public static ItemStack getOminousBannerInstance(HolderGetter<BannerPattern> patternRegistry) {
        ItemStack itemstack = new ItemStack(Items.WHITE_BANNER);
        BannerPatternLayers bannerpatternlayers = new BannerPatternLayers.Builder()
            .addIfRegistered(patternRegistry, BannerPatterns.RHOMBUS_MIDDLE, DyeColor.CYAN)
            .addIfRegistered(patternRegistry, BannerPatterns.STRIPE_BOTTOM, DyeColor.LIGHT_GRAY)
            .addIfRegistered(patternRegistry, BannerPatterns.STRIPE_CENTER, DyeColor.GRAY)
            .addIfRegistered(patternRegistry, BannerPatterns.BORDER, DyeColor.LIGHT_GRAY)
            .addIfRegistered(patternRegistry, BannerPatterns.STRIPE_MIDDLE, DyeColor.BLACK)
            .addIfRegistered(patternRegistry, BannerPatterns.HALF_HORIZONTAL, DyeColor.LIGHT_GRAY)
            .addIfRegistered(patternRegistry, BannerPatterns.CIRCLE_MIDDLE, DyeColor.LIGHT_GRAY)
            .addIfRegistered(patternRegistry, BannerPatterns.BORDER, DyeColor.BLACK)
            .build();
        itemstack.set(DataComponents.BANNER_PATTERNS, bannerpatternlayers);
        itemstack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.BANNER_PATTERNS, true));
        itemstack.set(DataComponents.ITEM_NAME, OMINOUS_BANNER_PATTERN_NAME);
        itemstack.set(DataComponents.RARITY, Rarity.UNCOMMON);
        return itemstack;
    }

    @Nullable
    public Raider getLeader(int wave) {
        return this.groupToLeaderMap.get(wave);
    }

    @Nullable
    private BlockPos findRandomSpawnPos(ServerLevel level, int attempts) {
        int i = this.raidCooldownTicks / 20;
        float f = 0.22F * i - 0.24F;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        float f1 = level.random.nextFloat() * (float) (Math.PI * 2);

        for (int i1 = 0; i1 < attempts; i1++) {
            float f2 = f1 + (float) Math.PI * i1 / 8.0F;
            int j = this.center.getX() + Mth.floor(Mth.cos(f2) * 32.0F * f) + level.random.nextInt(3) * Mth.floor(f);
            int l = this.center.getZ() + Mth.floor(Mth.sin(f2) * 32.0F * f) + level.random.nextInt(3) * Mth.floor(f);
            int k = level.getHeight(Heightmap.Types.WORLD_SURFACE, j, l);
            if (Mth.abs(k - this.center.getY()) <= 96) {
                blockpos$mutableblockpos.set(j, k, l);
                if (!level.isVillage(blockpos$mutableblockpos) || i <= 7) {
                    int j1 = 10;
                    if (level.hasChunksAt(
                            blockpos$mutableblockpos.getX() - 10,
                            blockpos$mutableblockpos.getZ() - 10,
                            blockpos$mutableblockpos.getX() + 10,
                            blockpos$mutableblockpos.getZ() + 10
                        )
                        && level.isPositionEntityTicking(blockpos$mutableblockpos)
                        && (
                            RAVAGER_SPAWN_PLACEMENT_TYPE.isSpawnPositionOk(level, blockpos$mutableblockpos, EntityType.RAVAGER)
                                || level.getBlockState(blockpos$mutableblockpos.below()).is(Blocks.SNOW)
                                    && level.getBlockState(blockpos$mutableblockpos).isAir()
                        )) {
                        return blockpos$mutableblockpos;
                    }
                }
            }
        }

        return null;
    }

    private boolean addWaveMob(ServerLevel level, int wave, Raider raider) {
        return this.addWaveMob(level, wave, raider, true);
    }

    public boolean addWaveMob(ServerLevel level, int wave, Raider p_raider, boolean isRecruited) {
        this.groupRaiderMap.computeIfAbsent(wave, p_37746_ -> Sets.newHashSet());
        Set<Raider> set = this.groupRaiderMap.get(wave);
        Raider raider = null;

        for (Raider raider1 : set) {
            if (raider1.getUUID().equals(p_raider.getUUID())) {
                raider = raider1;
                break;
            }
        }

        if (raider != null) {
            set.remove(raider);
            set.add(p_raider);
        }

        set.add(p_raider);
        if (isRecruited) {
            this.totalHealth = this.totalHealth + p_raider.getHealth();
        }

        this.updateBossbar();
        this.setDirty(level);
        return true;
    }

    public void setLeader(int wave, Raider raider) {
        this.groupToLeaderMap.put(wave, raider);
        raider.setItemSlot(EquipmentSlot.HEAD, getOminousBannerInstance(raider.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
        raider.setDropChance(EquipmentSlot.HEAD, 2.0F);
    }

    public void removeLeader(int wave) {
        this.groupToLeaderMap.remove(wave);
    }

    public BlockPos getCenter() {
        return this.center;
    }

    private void setCenter(BlockPos center) {
        this.center = center;
    }

    private int getDefaultNumSpawns(Raid.RaiderType raiderType, int wave, boolean shouldSpawnBonusGroup) {
        return shouldSpawnBonusGroup ? raiderType.spawnsPerWaveBeforeBonus[this.numGroups] : raiderType.spawnsPerWaveBeforeBonus[wave];
    }

    private int getPotentialBonusSpawns(Raid.RaiderType raiderType, RandomSource random, int wave, DifficultyInstance p_difficulty, boolean shouldSpawnBonusGroup) {
        Difficulty difficulty = p_difficulty.getDifficulty();
        boolean flag = difficulty == Difficulty.EASY;
        boolean flag1 = difficulty == Difficulty.NORMAL;
        int i;
        switch (raiderType) {
            case VINDICATOR:
            case PILLAGER:
                if (flag) {
                    i = random.nextInt(2);
                } else if (flag1) {
                    i = 1;
                } else {
                    i = 2;
                }
                break;
            case EVOKER:
            default:
                return 0;
            case WITCH:
                if (flag || wave <= 2 || wave == 4) {
                    return 0;
                }

                i = 1;
                break;
            case RAVAGER:
                i = !flag && shouldSpawnBonusGroup ? 1 : 0;
        }

        return i > 0 ? random.nextInt(i + 1) : 0;
    }

    public boolean isActive() {
        return this.active;
    }

    public int getNumGroups(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> 0;
            case EASY -> 3;
            case NORMAL -> 5;
            case HARD -> 7;
        };
    }

    public float getEnchantOdds() {
        int i = this.getRaidOmenLevel();
        if (i == 2) {
            return 0.1F;
        } else if (i == 3) {
            return 0.25F;
        } else if (i == 4) {
            return 0.5F;
        } else {
            return i == 5 ? 0.75F : 0.0F;
        }
    }

    public void addHeroOfTheVillage(Entity player) {
        this.heroesOfTheVillage.add(player.getUUID());
    }

    static enum RaidStatus implements StringRepresentable {
        ONGOING("ongoing"),
        VICTORY("victory"),
        LOSS("loss"),
        STOPPED("stopped");

        public static final Codec<Raid.RaidStatus> CODEC = StringRepresentable.fromEnum(Raid.RaidStatus::values);
        private final String name;

        private RaidStatus(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static enum RaiderType implements net.neoforged.fml.common.asm.enumextension.IExtensibleEnum {
        VINDICATOR(EntityType.VINDICATOR, new int[]{0, 0, 2, 0, 1, 4, 2, 5}),
        EVOKER(EntityType.EVOKER, new int[]{0, 0, 0, 0, 0, 1, 1, 2}),
        PILLAGER(EntityType.PILLAGER, new int[]{0, 4, 3, 3, 4, 4, 4, 2}),
        WITCH(EntityType.WITCH, new int[]{0, 0, 0, 0, 3, 0, 0, 1}),
        RAVAGER(EntityType.RAVAGER, new int[]{0, 0, 0, 1, 0, 1, 0, 2});

        static final Raid.RaiderType[] VALUES = values();
        @Deprecated // Neo: null for custom types, use the supplier instead
        final EntityType<? extends Raider> entityType;
        final int[] spawnsPerWaveBeforeBonus;
        final java.util.function.Supplier<EntityType<? extends Raider>> entityTypeSupplier;

        @net.neoforged.fml.common.asm.enumextension.ReservedConstructor
        private RaiderType(EntityType<? extends Raider> entityType, int[] spawnsPerWaveBeforeBonus) {
            this.entityType = entityType;
            this.spawnsPerWaveBeforeBonus = spawnsPerWaveBeforeBonus;
            this.entityTypeSupplier = () -> entityType;
        }

        private RaiderType(java.util.function.Supplier<EntityType<? extends Raider>> entityTypeSupplier, int[] spawnsPerWave) {
            this.entityType = null;
            this.spawnsPerWaveBeforeBonus = spawnsPerWave;
            this.entityTypeSupplier = entityTypeSupplier;
        }

        public static net.neoforged.fml.common.asm.enumextension.ExtensionInfo getExtensionInfo() {
            return net.neoforged.fml.common.asm.enumextension.ExtensionInfo.nonExtended(Raid.RaiderType.class);
        }
    }
}
