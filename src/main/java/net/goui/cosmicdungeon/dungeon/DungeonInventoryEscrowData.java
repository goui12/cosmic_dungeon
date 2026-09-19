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
    private record Persisted(List<Entry> entries, Map<String,ChopTravelPlan> transitions) {
        private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(Persisted::entries),
                Codec.unboundedMap(Codec.STRING,ChopTravelPlan.CODEC).optionalFieldOf("transitions",Map.of()).forGetter(Persisted::transitions)
        ).apply(instance, Persisted::new));
    }

    private static final Codec<DungeonInventoryEscrowData> CODEC = Persisted.CODEC.xmap(
            DungeonInventoryEscrowData::fromPersisted, DungeonInventoryEscrowData::toPersisted);
    public static final SavedDataType<DungeonInventoryEscrowData> TYPE =
            new SavedDataType<>(SAVE_ID, DungeonInventoryEscrowData::new, CODEC);

    private final Map<Key, Entry> entries = new HashMap<>();
    private final Map<String,ChopTravelPlan> transitions = new HashMap<>();
    private MinecraftServer server;

    private DungeonInventoryEscrowData() {}

    public static DungeonInventoryEscrowData get(MinecraftServer server) {
        net.goui.cosmicdungeon.transaction.SavedDataProof.validate(server,SAVE_ID,CODEC);
        var data=server.overworld().getDataStorage().computeIfAbsent(TYPE);data.server=server;return data;
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

    public boolean flushVerified(){return net.goui.cosmicdungeon.transaction.SavedDataProof.save(server,SAVE_ID,CODEC,this);}
    public boolean pendingDimension(String dimension){
        return transitions.values().stream().anyMatch(plan->plan.tag("source").getStringOr("dimension","").equals(dimension)
                ||plan.tag("destination").getStringOr("dimension","").equals(dimension));
    }
    public ChopTravelPlan transition(UUID owner){return transitions.get(owner.toString());}
    public void reserve(ChopTravelPlan plan){
        if(plan.committed()||transition(plan.owner())!=null)throw new IllegalStateException("Chop transition already pending");
        transitions.put(plan.owner().toString(),plan);setDirty();
    }
    public void commit(UUID owner,UUID id){
        var plan=transition(owner);if(plan==null||!plan.id().equals(id))throw new IllegalStateException("Missing Chop transition");
        transitions.put(owner.toString(),plan.commit());setDirty();
    }
    public void acknowledge(UUID owner,UUID id){
        var plan=transition(owner);if(plan==null)return;
        if(!plan.id().equals(id))throw new IllegalStateException("Wrong Chop transition acknowledgement");
        transitions.remove(owner.toString());setDirty();
    }
    public static CompoundTag image(Entry entry){
        return entry==null?new CompoundTag():(CompoundTag)Entry.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,entry).getOrThrow();
    }
    public void applyEscrow(ChopTravelPlan plan){
        if(plan.run()==0)return;
        var current=image(get(plan.run(),plan.owner()).orElse(null));
        var before=plan.tag("escrow_before");var after=plan.tag("escrow_after");
        if(!current.equals(before)&&!current.equals(after))throw new IllegalStateException("Escrow differs from frozen Chop plan");
        if(after.isEmpty())remove(plan.run(),plan.owner());
        else put(Entry.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,after).getOrThrow());
    }

    private static DungeonInventoryEscrowData fromPersisted(Persisted persisted) {
        DungeonInventoryEscrowData data = new DungeonInventoryEscrowData();
        for (Entry entry : persisted.entries()) {
            if(entry.runId()<=0||data.entries.put(new Key(entry.runId(), entry.playerId()),entry)!=null)
                throw new IllegalArgumentException("Duplicate or invalid Chop inventory escrow");
        }
        persisted.transitions().forEach((owner,plan)->{
            if(!UUID.fromString(owner).equals(plan.owner()))throw new IllegalArgumentException("Foreign Chop transition");
            data.transitions.put(owner,plan);
        });
        return data;
    }

    private Persisted toPersisted() {
        return new Persisted(entries.values().stream().sorted(java.util.Comparator.comparingLong(Entry::runId).thenComparing(e->e.playerId().toString())).toList(),Map.copyOf(transitions));
    }
}
