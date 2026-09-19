package net.goui.cosmicdungeon.trade;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.network.chat.Component;
import java.util.*;
/** Offer ownership survives normal world saves; full inventories never spill offered items. */
public final class TradeRecoveryData extends SavedData {
    private record Entry(String owner,long run,List<ItemStack> items){
        static final Codec<Entry> CODEC=RecordCodecBuilder.create(i->i.group(
                Codec.STRING.fieldOf("owner").forGetter(Entry::owner),Codec.LONG.optionalFieldOf("run",0L).forGetter(Entry::run),
                ItemStack.CODEC.listOf().fieldOf("items").forGetter(Entry::items)).apply(i,Entry::new));
    }
    private static final Codec<TradeRecoveryData> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.unboundedMap(Codec.STRING,Entry.CODEC).optionalFieldOf("offers",Map.of()).forGetter(d->d.offers)).apply(i,TradeRecoveryData::new));
    private static final SavedDataType<TradeRecoveryData> TYPE=new SavedDataType<>("cosmicdungeon_trade_recovery_v1",TradeRecoveryData::new,CODEC);
    private final Map<String,Entry> offers=new LinkedHashMap<>();
    private TradeRecoveryData(){}
    private TradeRecoveryData(Map<String,Entry> data){offers.putAll(data);}
    private static final Set<MinecraftServer> VALIDATED=Collections.newSetFromMap(new WeakHashMap<>());
    public static TradeRecoveryData get(MinecraftServer server) {
        if(!VALIDATED.contains(server)){
            var file=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("data/cosmicdungeon_trade_recovery_v1.dat");
            try{
                if(java.nio.file.Files.exists(file)){
                    var root=net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.create(64L*1024*1024));
                    var body=root.getCompound("data").orElseThrow(()->new IllegalArgumentException("Missing legacy trade data"));
                    CODEC.parse(server.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),body).getOrThrow();
                }
            }catch(Exception error){throw new IllegalStateException("Legacy trade data needs review; original file preserved",error);}
            VALIDATED.add(server);
        }
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public void update(UUID session,ServerPlayer owner,SimpleContainer container){
        var stacks=new ArrayList<ItemStack>();
        for(int i=0;i<container.getContainerSize();i++)if(!container.getItem(i).isEmpty())stacks.add(container.getItem(i).copy());
        long run=D1Members.run(owner.level()).filter(r->r.containsPlayer(owner.getUUID())).map(r->r.runId()).orElse(0L);
        put(session,owner.getUUID(),run,stacks);
    }
    private void put(UUID session,UUID owner,long run,List<ItemStack> stacks){
        String key=session+"|"+owner;
        if(stacks.isEmpty())offers.remove(key);else offers.put(key,new Entry(owner.toString(),run,List.copyOf(stacks)));
        setDirty();
    }
    public void remaining(UUID session,ServerPlayer owner,List<ItemStack> stacks){
        var old=offers.get(session+"|"+owner.getUUID());
        long run=old==null?D1Members.run(owner.level()).map(r->r.runId()).orElse(0L):old.run();
        put(session,owner.getUUID(),run,stacks);
    }
    public void finished(UUID session){if(offers.keySet().removeIf(key->key.startsWith(session+"|")))setDirty();}
    public void clearRun(long run){ /* Preserve unreceipted legacy evidence for review. */ }
    public void clearOwnerRun(UUID owner,long run){ /* Preserve unreceipted legacy evidence for review. */ }
    public void notifyLegacy(ServerPlayer player){
        if(offers.values().stream().anyMatch(e->e.owner().equals(player.getUUID().toString())))
            player.sendSystemMessage(Component.literal("An older trade record is preserved for developer review; /trade claim recovers current owned items."));
    }
    public int claim(ServerPlayer player){return TradeCustody.claim(player);}
    // TODO(M115, legacy evidence review): Gear Trading 2.0 (2026-08-18),
    // 1byHfuC0G_lb0IRrgO3kblLYP06AY8gJWm9bJOMlrFIc: these old world-file copies lack
    // paired owner-inventory receipts. Preserve full NBT and compare complete save backups;
    // automatic replay/deletion cannot establish whether an entry was already delivered.
}
