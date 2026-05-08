package net.minecraft.stats;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

/**
 * Server-side implementation of {@link net.minecraft.stats.StatsCounter}; handles counting, serialising, and de-serialising statistics, as well as sending them to connected clients via the {@linkplain net.minecraft.network.protocol.game.ClientboundAwardStatsPacket award stats packet}.
 */
public class ServerStatsCounter extends StatsCounter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Codec<Map<Stat<?>, Integer>> STATS_CODEC = Codec.dispatchedMap(
            BuiltInRegistries.STAT_TYPE.byNameCodec(), Util.memoize(ServerStatsCounter::createTypedStatsCodec)
        )
        .xmap(p_404237_ -> {
            Map<Stat<?>, Integer> map = new HashMap<>();
            p_404237_.forEach((p_404229_, p_404230_) -> map.putAll((Map<? extends Stat<?>, ? extends Integer>)p_404230_));
            return map;
        }, p_404234_ -> p_404234_.entrySet().stream().collect(Collectors.groupingBy(p_404238_ -> p_404238_.getKey().getType(), Util.toMap())));
    private final MinecraftServer server;
    private final File file;
    private final Set<Stat<?>> dirty = Sets.newHashSet();

    private static <T> Codec<Map<Stat<?>, Integer>> createTypedStatsCodec(StatType<T> type) {
        Codec<T> codec = type.getRegistry().byNameCodec();
        Codec<Stat<?>> codec1 = codec.flatComapMap(
            type::get,
            p_404236_ -> p_404236_.getType() == type
                ? DataResult.success((T)p_404236_.getValue())
                : DataResult.error(() -> "Expected type " + type + ", but got " + p_404236_.getType())
        );
        return Codec.unboundedMap(codec1, Codec.INT);
    }

    public ServerStatsCounter(MinecraftServer server, File file) {
        this.server = server;
        this.file = file;
        if (file.isFile()) {
            try {
                this.parseLocal(server.getFixerUpper(), FileUtils.readFileToString(file));
            } catch (IOException ioexception) {
                LOGGER.error("Couldn't read statistics file {}", file, ioexception);
            } catch (JsonParseException jsonparseexception) {
                LOGGER.error("Couldn't parse statistics file {}", file, jsonparseexception);
            }
        }
    }

    public void save() {
        try {
            FileUtils.writeStringToFile(this.file, this.toJson());
        } catch (IOException ioexception) {
            LOGGER.error("Couldn't save stats", (Throwable)ioexception);
        }
    }

    @Override
    public void setValue(Player player, Stat<?> stat, int value) {
        super.setValue(player, stat, value);
        this.dirty.add(stat);
    }

    private Set<Stat<?>> getDirty() {
        Set<Stat<?>> set = Sets.newHashSet(this.dirty);
        this.dirty.clear();
        return set;
    }

    public void parseLocal(DataFixer fixerUpper, String json) {
        try {
            JsonElement jsonelement = StrictJsonParser.parse(json);
            if (jsonelement.isJsonNull()) {
                LOGGER.error("Unable to parse Stat data from {}", this.file);
                return;
            }

            Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, jsonelement);
            dynamic = DataFixTypes.STATS.updateToCurrentVersion(fixerUpper, dynamic, NbtUtils.getDataVersion(dynamic, 1343));
            this.stats
                .putAll(
                    STATS_CODEC.parse(dynamic.get("stats").orElseEmptyMap())
                        .resultOrPartial(p_404233_ -> LOGGER.error("Failed to parse statistics for {}: {}", this.file, p_404233_))
                        .orElse(Map.of())
                );
        } catch (JsonParseException jsonparseexception) {
            LOGGER.error("Unable to parse Stat data from {}", this.file, jsonparseexception);
        }
    }

    protected String toJson() {
        JsonObject jsonobject = new JsonObject();
        jsonobject.add("stats", STATS_CODEC.encodeStart(JsonOps.INSTANCE, this.stats).getOrThrow());
        jsonobject.addProperty("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
        return jsonobject.toString();
    }

    public void markAllDirty() {
        this.dirty.addAll(this.stats.keySet());
    }

    public void sendStats(ServerPlayer player) {
        Object2IntMap<Stat<?>> object2intmap = new Object2IntOpenHashMap<>();

        for (Stat<?> stat : this.getDirty()) {
            object2intmap.put(stat, this.getValue(stat));
        }

        player.connection.send(new ClientboundAwardStatsPacket(object2intmap));
    }
}
