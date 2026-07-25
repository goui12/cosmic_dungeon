package net.goui.cosmicdungeon.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Persistent two-inventory escrow used while a run member temporarily visits Main Village. */
public final class DungeonInventoryEscrowData extends SavedData {
    private static final String SAVE_ID = "cosmicdungeon_dungeon_inventory_escrow_v1";
    public record Entry(long runId, UUID playerId, CompoundTag dungeonInventory,
                        CompoundTag outsideInventory, boolean outsideActive) {
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("run_id").forGetter(Entry::runId),
                UUID_CODEC.fieldOf("player_id").forGetter(Entry::playerId),
                CompoundTag.CODEC.fieldOf("dungeon_inventory").forGetter(Entry::dungeonInventory),
                CompoundTag.CODEC.fieldOf("outside_inventory").forGetter(Entry::outsideInventory),
                Codec.BOOL.optionalFieldOf("outside_active", false).forGetter(Entry::outsideActive)
        ).apply(instance, Entry::new));

        Entry withOutsideInventory(CompoundTag inventory, boolean outside) {
            return new Entry(runId, playerId, dungeonInventory.copy(), inventory.copy(), outside);
        }
    }

    private record Key(long runId, UUID playerId) {}
    private record Persisted(List<Entry> entries) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(Persisted::entries)
        ).apply(instance, Persisted::new));
    }

    private static final Codec<DungeonInventoryEscrowData> CODEC = Persisted.CODEC.xmap(
            DungeonInventoryEscrowData::fromPersisted, DungeonInventoryEscrowData::toPersisted);
    public static final SavedDataType<DungeonInventoryEscrowData> TYPE =
            new SavedDataType<>(SAVE_ID, DungeonInventoryEscrowData::new, CODEC);

    private final Map<Key, Entry> entries = new HashMap<>();

    private DungeonInventoryEscrowData() {}

    public static DungeonInventoryEscrowData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<Entry> get(long runId, UUID playerId) {
        return Optional.ofNullable(entries.get(new Key(runId, playerId)));
    }

    public void put(Entry entry) {
        entries.put(new Key(entry.runId(), entry.playerId()), entry);
        setDirty();
    }

    public Optional<Entry> remove(long runId, UUID playerId) {
        Entry removed = entries.remove(new Key(runId, playerId));
        if (removed != null) setDirty();
        return Optional.ofNullable(removed);
    }

    private static DungeonInventoryEscrowData fromPersisted(Persisted persisted) {
        DungeonInventoryEscrowData data = new DungeonInventoryEscrowData();
        for (Entry entry : persisted.entries()) data.entries.put(new Key(entry.runId(), entry.playerId()), entry);
        return data;
    }

    private Persisted toPersisted() {
        return new Persisted(new ArrayList<>(entries.values()));
    }
}
