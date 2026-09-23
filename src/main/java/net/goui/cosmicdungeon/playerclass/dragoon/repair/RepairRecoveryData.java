package net.goui.cosmicdungeon.playerclass.dragoon.repair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Readable legacy repair archive; new custody lives with its owner's inventory. */
public final class RepairRecoveryData extends SavedData {
    // TODO(M25, legacy evidence review): Repair 2.0 (2026-08-18) requires owner-safe replay.
    // Older entries lack a matching player-file receipt and may already have been delivered.
    // Keep their exact item images for full player/save-backup comparison; do not silently
    // auto-award, discard or infer uniqueness from lore/name. New sessions never write this map.

    private record Entry(String owner,ItemStack item) {
        static final Codec<Entry> CODEC=RecordCodecBuilder.create(i->i.group(
                Codec.STRING.fieldOf("owner").forGetter(Entry::owner),
                ItemStack.CODEC.fieldOf("item").forGetter(Entry::item)).apply(i,Entry::new));
    }
    private static final Codec<RepairRecoveryData> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.unboundedMap(Codec.STRING,Entry.CODEC).optionalFieldOf("pending",Map.of())
                    .forGetter((RepairRecoveryData d)->d.pending)).apply(i,RepairRecoveryData::load));
    private static final SavedDataType<RepairRecoveryData> TYPE=new SavedDataType<>(
            "cosmicdungeon_repair_recovery_v1",RepairRecoveryData::new,CODEC);
    private final Map<String,Entry> pending=new LinkedHashMap<>();
    private RepairRecoveryData() {}
    private static RepairRecoveryData load(Map<String,Entry> pending) { var d=new RepairRecoveryData();d.pending.putAll(pending);return d; }
    private static final Set<MinecraftServer> VALIDATED=Collections.newSetFromMap(new WeakHashMap<>());
    public static RepairRecoveryData get(MinecraftServer server) {
        if(!VALIDATED.contains(server)){
            var file=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("data/cosmicdungeon_repair_recovery_v1.dat");
            try{
                if(java.nio.file.Files.exists(file)){
                    var root=net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.create(64L*1024*1024));
                    var body=root.getCompound("data").orElseThrow(()->new IllegalArgumentException("Missing legacy repair data"));
                    CODEC.parse(server.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),body).getOrThrow();
                }
            }catch(Exception error){throw new IllegalStateException("Legacy repair data needs review; original file preserved",error);}
            VALIDATED.add(server);
        }
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public void update(UUID session,UUID owner,ItemStack stack) {
        if(stack.isEmpty()) pending.remove(session.toString());
        else pending.put(session.toString(),new Entry(owner.toString(),stack.copy()));
        setDirty();
    }
    public boolean hasLegacy(UUID owner){return pending.values().stream().anyMatch(e->e.owner().equals(owner.toString()));}
    public void notifyLegacy(ServerPlayer player){
        if(hasLegacy(player.getUUID()))player.sendSystemMessage(Component.literal(
                "An older repair recovery record is preserved for developer review; /repair claim recovers current owned items."));
    }
    /** Legacy records have no player inventory receipt; automatic replay cannot prove absence of duplicates. */
    public int claim(ServerPlayer player){notifyLegacy(player);return RepairCustody.claim(player);}
    // TODO(M25, legacy evidence review): old pending entries remain byte-for-byte item copies in
    // cosmicdungeon_repair_recovery_v1. Compare the complete player save and pre-upgrade backup;
    // do not guess from display names, delete records, or automatically award an unproven duplicate.
}
