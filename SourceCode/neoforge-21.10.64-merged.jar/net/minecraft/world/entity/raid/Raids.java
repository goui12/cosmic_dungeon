package net.minecraft.world.entity.raid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

public class Raids extends SavedData {
    private static final String RAID_FILE_ID = "raids";
    public static final Codec<Raids> CODEC = RecordCodecBuilder.create(
        p_400930_ -> p_400930_.group(
                Raids.RaidWithId.CODEC
                    .listOf()
                    .optionalFieldOf("raids", List.of())
                    .forGetter(p_400932_ -> p_400932_.raidMap.int2ObjectEntrySet().stream().map(Raids.RaidWithId::from).toList()),
                Codec.INT.fieldOf("next_id").forGetter(p_400933_ -> p_400933_.nextId),
                Codec.INT.fieldOf("tick").forGetter(p_400931_ -> p_400931_.tick)
            )
            .apply(p_400930_, Raids::new)
    );
    public static final SavedDataType<Raids> TYPE = new SavedDataType<>("raids", Raids::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);
    public static final SavedDataType<Raids> TYPE_END = new SavedDataType<>("raids_end", Raids::new, CODEC, DataFixTypes.SAVED_DATA_RAIDS);
    private final Int2ObjectMap<Raid> raidMap = new Int2ObjectOpenHashMap<>();
    private int nextId = 1;
    private int tick;

    public static SavedDataType<Raids> getType(Holder<DimensionType> dimension) {
        return dimension.is(BuiltinDimensionTypes.END) ? TYPE_END : TYPE;
    }

    public Raids() {
        this.setDirty();
    }

    private Raids(List<Raids.RaidWithId> raids, int nextId, int tick) {
        for (Raids.RaidWithId raids$raidwithid : raids) {
            this.raidMap.put(raids$raidwithid.id, raids$raidwithid.raid);
        }

        this.nextId = nextId;
        this.tick = tick;
    }

    @Nullable
    public Raid get(int id) {
        return this.raidMap.get(id);
    }

    public OptionalInt getId(Raid raid) {
        for (Entry<Raid> entry : this.raidMap.int2ObjectEntrySet()) {
            if (entry.getValue() == raid) {
                return OptionalInt.of(entry.getIntKey());
            }
        }

        return OptionalInt.empty();
    }

    public void tick(ServerLevel level) {
        this.tick++;
        Iterator<Raid> iterator = this.raidMap.values().iterator();

        while (iterator.hasNext()) {
            Raid raid = iterator.next();
            if (level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
                raid.stop();
            }

            if (raid.isStopped()) {
                iterator.remove();
                this.setDirty();
            } else {
                raid.tick(level);
            }
        }

        if (this.tick % 200 == 0) {
            this.setDirty();
        }
    }

    public static boolean canJoinRaid(Raider raider) {
        return raider.isAlive() && raider.canJoinRaid() && raider.getNoActionTime() <= 2400;
    }

    @Nullable
    public Raid createOrExtendRaid(ServerPlayer player, BlockPos pos) {
        if (player.isSpectator()) {
            return null;
        } else {
            ServerLevel serverlevel = player.level();
            if (serverlevel.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
                return null;
            } else {
                DimensionType dimensiontype = serverlevel.dimensionType();
                if (!dimensiontype.hasRaids()) {
                    return null;
                } else {
                    List<PoiRecord> list = serverlevel.getPoiManager()
                        .getInRange(p_219845_ -> p_219845_.is(PoiTypeTags.VILLAGE), pos, 64, PoiManager.Occupancy.IS_OCCUPIED)
                        .toList();
                    int i = 0;
                    Vec3 vec3 = Vec3.ZERO;

                    for (PoiRecord poirecord : list) {
                        BlockPos blockpos = poirecord.getPos();
                        vec3 = vec3.add(blockpos.getX(), blockpos.getY(), blockpos.getZ());
                        i++;
                    }

                    BlockPos blockpos1;
                    if (i > 0) {
                        vec3 = vec3.scale(1.0 / i);
                        blockpos1 = BlockPos.containing(vec3);
                    } else {
                        blockpos1 = pos;
                    }

                    Raid raid = this.getOrCreateRaid(serverlevel, blockpos1);
                    if (!raid.isStarted() && !this.raidMap.containsValue(raid)) {
                        this.raidMap.put(this.getUniqueId(), raid);
                    }

                    if (!raid.isStarted() || raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel()) {
                        raid.absorbRaidOmen(player);
                    }

                    this.setDirty();
                    return raid;
                }
            }
        }
    }

    private Raid getOrCreateRaid(ServerLevel serverLevel, BlockPos pos) {
        Raid raid = serverLevel.getRaidAt(pos);
        return raid != null ? raid : new Raid(pos, serverLevel.getDifficulty());
    }

    public static Raids load(CompoundTag tag) {
        return CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial().orElseGet(Raids::new);
    }

    private int getUniqueId() {
        return ++this.nextId;
    }

    @Nullable
    public Raid getNearbyRaid(BlockPos pos, int distance) {
        Raid raid = null;
        double d0 = distance;

        for (Raid raid1 : this.raidMap.values()) {
            double d1 = raid1.getCenter().distSqr(pos);
            if (raid1.isActive() && d1 < d0) {
                raid = raid1;
                d0 = d1;
            }
        }

        return raid;
    }

    @VisibleForDebug
    public List<BlockPos> getRaidCentersInChunk(ChunkPos chunkPos) {
        return this.raidMap.values().stream().map(Raid::getCenter).filter(chunkPos::contains).toList();
    }

    record RaidWithId(int id, Raid raid) {
        public static final Codec<Raids.RaidWithId> CODEC = RecordCodecBuilder.create(
            p_401087_ -> p_401087_.group(Codec.INT.fieldOf("id").forGetter(Raids.RaidWithId::id), Raid.MAP_CODEC.forGetter(Raids.RaidWithId::raid))
                .apply(p_401087_, Raids.RaidWithId::new)
        );

        public static Raids.RaidWithId from(Entry<Raid> entry) {
            return new Raids.RaidWithId(entry.getIntKey(), entry.getValue());
        }
    }
}
