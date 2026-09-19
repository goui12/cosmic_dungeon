package net.goui.cosmicdungeon.dungeon;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

/** One concurrent personal Chop, including dropped/stored items and the temporary return state. */
public final class ChopOwnershipData extends SavedData {
    public record Entry(String token,long runId,boolean deliver) {
        static final Codec<Entry> CODEC=RecordCodecBuilder.create(i->i.group(
                Codec.STRING.fieldOf("token").forGetter(Entry::token),
                Codec.LONG.optionalFieldOf("run_id",0L).forGetter(Entry::runId),
                Codec.BOOL.optionalFieldOf("deliver",false).forGetter(Entry::deliver)).apply(i,Entry::new));
    }
    private static final Codec<ChopOwnershipData> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.unboundedMap(Codec.STRING,Entry.CODEC).optionalFieldOf("owners",Map.of())
                    .forGetter((ChopOwnershipData d)->d.owners)).apply(i,ChopOwnershipData::load));
    private static final SavedDataType<ChopOwnershipData> TYPE=new SavedDataType<>("cosmicdungeon_chop_owners_v1",ChopOwnershipData::new,CODEC);
    private final Map<String,Entry> owners=new HashMap<>();
    private ChopOwnershipData() {}
    private static ChopOwnershipData load(Map<String,Entry> owners){var d=new ChopOwnershipData();d.owners.putAll(owners);return d;}
    private MinecraftServer server;
    private static final Set<MinecraftServer> VALIDATED=Collections.newSetFromMap(new WeakHashMap<>());
    private static java.nio.file.Path path(MinecraftServer s){return s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/cosmicdungeon_chop_owners_v1.dat");}
    private static ChopOwnershipData read(MinecraftServer s)throws java.io.IOException{
        var root=net.minecraft.nbt.NbtIo.readCompressed(path(s),net.minecraft.nbt.NbtAccounter.create(64L*1024*1024));
        return CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,root.getCompound("data").orElseThrow()).getOrThrow();
    }
    public static ChopOwnershipData get(MinecraftServer server){
        if(!VALIDATED.contains(server)){try{if(java.nio.file.Files.exists(path(server)))read(server);}catch(Exception e){throw new IllegalStateException("Chop ownership save needs review; original preserved",e);}VALIDATED.add(server);}
        var data=server.overworld().getDataStorage().computeIfAbsent(TYPE);data.server=server;return data;
    }
    public net.minecraft.nbt.CompoundTag image(UUID owner){var e=entry(owner);return e==null?new net.minecraft.nbt.CompoundTag():(net.minecraft.nbt.CompoundTag)Entry.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,e).getOrThrow();}
    public boolean compareAndSetVerified(UUID owner,net.minecraft.nbt.CompoundTag before,net.minecraft.nbt.CompoundTag after){
        if(server==null)return false;
        try{
            var current=image(owner);if(!current.equals(before)&&!current.equals(after))return false;
            if(!current.equals(after)){if(after.isEmpty())owners.remove(owner.toString());else owners.put(owner.toString(),Entry.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,after).getOrThrow());}
            setDirty();server.overworld().getDataStorage().saveAndJoin();return owners.equals(read(server).owners);
        }catch(Exception failure){setDirty();return false;}
    }
    public static net.minecraft.nbt.CompoundTag issuedImage(UUID token){return (net.minecraft.nbt.CompoundTag)Entry.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,new Entry(token.toString(),0,false)).getOrThrow();}
    public Entry entry(UUID owner){return owners.get(owner.toString());}
    public void issue(UUID owner,UUID token){owners.put(owner.toString(),new Entry(token.toString(),0,false));setDirty();}
    public void bindRun(UUID owner,long run){var e=entry(owner);if(e!=null){owners.put(owner.toString(),new Entry(e.token(),run,e.deliver()));setDirty();}}
    public void release(UUID owner,UUID token){var e=entry(owner);if(e!=null&&e.token().equals(token.toString())){owners.remove(owner.toString());setDirty();}}
    public void delivered(UUID owner){var e=entry(owner);if(e!=null&&e.deliver()){owners.put(owner.toString(),new Entry(e.token(),0,false));setDirty();}}
    public void finishOwnerRun(UUID owner,long runId){
        var e=entry(owner);
        if(e!=null&&e.runId()==runId){owners.put(owner.toString(),new Entry(UUID.randomUUID().toString(),0,true));setDirty();}
    }
    public void finishRun(long runId){
        owners.replaceAll((owner,e)->e.runId()==runId?new Entry(UUID.randomUUID().toString(),0,true):e);
        setDirty();
    }
}
