package net.goui.cosmicdungeon.playerclass.bogatyr;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.*;

/**
 * Run identity/location directory with compatible legacy reads and durable retirement tombstones.
 * Active native entities survive saves; retired archive audit images can never recreate pets.
 */
public final class BogatyrCompanionData extends SavedData {
    public record Companion(UUID wolf, UUID owner, long run, String dimension, long position, boolean located, UUID entityUuid) {
        public Companion(UUID wolf,UUID owner,long run,String dimension,long position,boolean located){
            this(wolf,owner,run,dimension,position,located,wolf);
        }
        static final Codec<Companion> CODEC=RecordCodecBuilder.create(i->i.group(
                UUIDUtil.STRING_CODEC.fieldOf("wolf").forGetter(Companion::wolf),
                UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(Companion::owner),
                Codec.LONG.fieldOf("run").forGetter(Companion::run),
                Codec.STRING.fieldOf("dimension").forGetter(Companion::dimension),
                Codec.LONG.optionalFieldOf("position",0L).forGetter(Companion::position),
                Codec.BOOL.optionalFieldOf("located",false).forGetter(Companion::located),
                UUIDUtil.STRING_CODEC.optionalFieldOf("entity_uuid").forGetter(c->Optional.of(c.entityUuid()))
        ).apply(i,(wolf,owner,run,dimension,position,located,physical)->
                new Companion(wolf,owner,run,dimension,position,located,physical.orElse(wolf))));
    }
    private static final Codec<BogatyrCompanionData> KNOWN_CODEC=RecordCodecBuilder.create(i->i.group(
            Companion.CODEC.listOf().optionalFieldOf("companions",List.of()).forGetter((BogatyrCompanionData d)->List.copyOf(d.companions.values())),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC,WolfArchive.CODEC).optionalFieldOf("archives",Map.of()).forGetter((BogatyrCompanionData d)->d.archives),
            Codec.STRING.listOf().optionalFieldOf("identity_holds",List.of()).forGetter((BogatyrCompanionData d)->List.copyOf(d.identityHolds))
    ).apply(i,BogatyrCompanionData::load));
    private net.minecraft.nbt.CompoundTag original=new net.minecraft.nbt.CompoundTag();
    private final Set<Long> retiredRuns=new LinkedHashSet<>();
    static final Codec<BogatyrCompanionData> CODEC=net.minecraft.nbt.CompoundTag.CODEC.comapFlatMap(tag->{
        try{
            var data=KNOWN_CODEC.parse(NbtOps.INSTANCE,tag).getOrThrow();data.original=tag.copy();
            if(tag.contains("retired_runs")){
                if(!(tag.get("retired_runs") instanceof net.minecraft.nbt.ListTag list))throw new IllegalArgumentException("Invalid retired runs");
                for(var value:list){
                    if(!(value instanceof net.minecraft.nbt.LongTag id)||id.longValue()<=0)throw new IllegalArgumentException("Invalid retired run");
                    data.retiredRuns.add(id.longValue());
                }
            }
            return com.mojang.serialization.DataResult.success(data);
        }catch(Exception error){return com.mojang.serialization.DataResult.error(()->error.toString());}
    },BogatyrCompanionData::image);
    /** Overlay known values on the original image, including unknown per-record/archive fields. */
    net.minecraft.nbt.CompoundTag image(){
        var next=original.copy();var known=(net.minecraft.nbt.CompoundTag)KNOWN_CODEC.encodeStart(NbtOps.INSTANCE,this).getOrThrow();
        var oldEntries=new HashMap<String,net.minecraft.nbt.CompoundTag>();
        for(var value:original.getListOrEmpty("companions"))if(value instanceof net.minecraft.nbt.CompoundTag entry)
            oldEntries.put(entry.getStringOr("wolf",""),entry);
        var entries=new net.minecraft.nbt.ListTag();
        for(var value:known.getListOrEmpty("companions")){
            var entry=(net.minecraft.nbt.CompoundTag)value;
            var merged=oldEntries.getOrDefault(entry.getStringOr("wolf",""),new net.minecraft.nbt.CompoundTag()).copy();
            for(var key:List.of("wolf","owner","run","dimension","position","located","entity_uuid"))merged.remove(key);
            merged.merge(entry);entries.add(merged);
        }
        next.put("companions",entries);
        var archives=new net.minecraft.nbt.CompoundTag();var oldArchives=original.getCompoundOrEmpty("archives");
        var newArchives=known.getCompoundOrEmpty("archives");
        for(var key:newArchives.keySet()){
            var merged=oldArchives.getCompoundOrEmpty(key).copy();
            for(var field:List.of("phase","transaction","run","source_dimension","source_position","target_dimension","target_position","entity","target_run"))merged.remove(field);
            merged.merge(newArchives.getCompoundOrEmpty(key));archives.put(key,merged);
        }
        next.put("archives",archives);next.put("identity_holds",known.getListOrEmpty("identity_holds").copy());
        var retired=new net.minecraft.nbt.ListTag();retiredRuns.forEach(id->retired.add(net.minecraft.nbt.LongTag.valueOf(id)));
        next.put("retired_runs",retired);return next;
    }
    public List<Companion> entries(){return List.copyOf(companions.values());}
    public boolean runRetired(long run){return retiredRuns.contains(run);}
    public void markRunRetired(long run){if(run>0&&retiredRuns.add(run))setDirty();}
    /** Retained audit images can never be delivered and do not participate in active indices. */
    public void retireRecord(Companion entry){
        if(!companions.containsKey(entry.wolf()))return;
        var archive=archives.get(entry.wolf());
        {
            var audit=original.getCompoundOrEmpty("retired_records").copy();var record=new net.minecraft.nbt.CompoundTag();
            var full=image();
            for(var value:full.getListOrEmpty("companions"))if(value instanceof net.minecraft.nbt.CompoundTag c&&c.getStringOr("wolf","").equals(entry.wolf().toString()))record.put("companion",c.copy());
            record.put("archive",full.getCompoundOrEmpty("archives").getCompoundOrEmpty(entry.wolf().toString()).copy());
            audit.put(entry.wolf().toString(),record);original.put("retired_records",audit);
        }
        identityHolds.remove(entry.dimension()+"|"+entry.entityUuid());
        if(archive!=null)identityHolds.remove(archive.targetDimension()+"|"+entry.wolf());
        release(entry.wolf(),entry.owner());setDirty();
    }
    public void retireArchive(UUID wolf){
        if(!archives.containsKey(wolf))return;
        var audit=original.getCompoundOrEmpty("retired_records").copy();var record=audit.getCompoundOrEmpty(wolf.toString()).copy();
        record.put("archive",image().getCompoundOrEmpty("archives").getCompoundOrEmpty(wolf.toString()).copy());
        audit.put(wolf.toString(),record);original.put("retired_records",audit);
        setArchive(wolf,null);setDirty();
    }
    private static final SavedDataType<BogatyrCompanionData> TYPE=new SavedDataType<>(
            "cosmicdungeon_bogatyr_companions_v1",BogatyrCompanionData::new,CODEC);
    private static final Set<MinecraftServer> VALIDATED=Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<UUID,Companion> companions=new LinkedHashMap<>();
    private final Map<UUID,Set<UUID>> owners=new HashMap<>();
    private final Map<UUID,Set<UUID>> activeOwners=new HashMap<>();
    private void removeActive(UUID owner,UUID wolf){
        var ids=activeOwners.get(owner);if(ids==null)return;
        ids.remove(wolf);if(ids.isEmpty())activeOwners.remove(owner);
    }
    private void updateActive(Companion entry){
        var archive=archives.get(entry.wolf());
        if(archive!=null&&archive.phase().equals(WolfArchive.STORED))removeActive(entry.owner(),entry.wolf());
        else activeOwners.computeIfAbsent(entry.owner(),key->new LinkedHashSet<>()).add(entry.wolf());
    }
    private final Map<String,Set<UUID>> dimensions=new HashMap<>();
    private final Map<UUID,WolfArchive> archives=new LinkedHashMap<>();
    private final Set<String> identityHolds=new LinkedHashSet<>();
    private final Map<String,Set<UUID>> destinations=new HashMap<>();
    private void setArchive(UUID wolf,WolfArchive next){
        var old=archives.get(wolf);
        if(old!=null&&old.phase().equals(WolfArchive.RELEASING)){
            var ids=destinations.get(old.targetDimension());
            if(ids!=null){ids.remove(wolf);if(ids.isEmpty())destinations.remove(old.targetDimension());}
        }
        if(next==null)archives.remove(wolf);else{
            archives.put(wolf,next);
            if(next.phase().equals(WolfArchive.RELEASING))
                destinations.computeIfAbsent(next.targetDimension(),key->new LinkedHashSet<>()).add(wolf);
        }
        var companion=companions.get(wolf);if(companion!=null)updateActive(companion);
    }
    public List<Companion> pendingInDimension(String dimension){
        return destinations.getOrDefault(dimension,Set.of()).stream().map(companions::get).toList();
    }
    public void identityHold(String dimension,UUID entityUuid){
        if(identityHolds.add(dimension+"|"+entityUuid))setDirty();
    }
    public boolean identityHeld(String dimension,UUID entityUuid){return identityHolds.contains(dimension+"|"+entityUuid);}
    public boolean dimensionHeld(String dimension){return identityHolds.stream().anyMatch(s->s.startsWith(dimension+"|"));}
    public Set<String> identityHolds(){return Set.copyOf(identityHolds);}
    private static Path path(MinecraftServer server){
        return server.getWorldPath(LevelResource.ROOT).resolve("data/cosmicdungeon_bogatyr_companions_v1.dat");
    }
    private static BogatyrCompanionData read(MinecraftServer server)throws java.io.IOException{
        var tag=NbtIo.readCompressed(path(server),NbtAccounter.create(64L*1024*1024));
        var body=tag.getCompound("data").orElseThrow(()->new java.io.IOException("Missing companion data body"));
        return CODEC.parse(NbtOps.INSTANCE,body).getOrThrow();
    }
    public static BogatyrCompanionData get(MinecraftServer server){
        // Vanilla computeIfAbsent replaces a failed decode with an empty save. Do not overwrite
        // an unreadable ownership directory: preserve the original and stop automatic mutation.
        if(!VALIDATED.contains(server)){
            try{if(Files.exists(path(server)))read(server);}
            catch(Exception error){throw new IllegalStateException("Companion directory requires recovery; original file preserved",error);}
            VALIDATED.add(server);
        }
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public boolean flushVerified(MinecraftServer server){
        try{
            setDirty();server.overworld().getDataStorage().saveAndJoin();
            var saved=read(server);return image().equals(saved.image());
        }catch(Exception error){
            com.mojang.logging.LogUtils.getLogger().error("Companion directory save not verified; retaining run roster",error);
            setDirty();return false;
        }
    }
    private static BogatyrCompanionData load(List<Companion> entries,Map<UUID,WolfArchive> archives,List<String> identityHolds){
        var data=new BogatyrCompanionData();
        data.identityHolds.addAll(identityHolds);
        for(var entry:entries){
            if(data.companions.containsKey(entry.wolf()))
                throw new IllegalArgumentException("Duplicate saved companion UUID: "+entry.wolf());
            data.insert(entry);
        }
        for(var entry:archives.entrySet()){
            var companion=data.companions.get(entry.getKey());
            if(companion==null||!entry.getValue().valid(entry.getKey(),companion.owner()))
                throw new IllegalArgumentException("Invalid/orphaned companion archive: "+entry.getKey());
            data.setArchive(entry.getKey(),entry.getValue());
        }
        return data;
    }
    private void insert(Companion entry){
        var old=companions.put(entry.wolf(),entry);
        if(old!=null&&!old.owner().equals(entry.owner()))removeActive(old.owner(),entry.wolf());
        if(old!=null&&!old.owner().equals(entry.owner())){
            var previous=owners.get(old.owner());if(previous!=null){previous.remove(entry.wolf());if(previous.isEmpty())owners.remove(old.owner());}
        }
        if(old!=null&&!old.dimension().equals(entry.dimension())){
            var previous=dimensions.get(old.dimension());
            if(previous!=null){previous.remove(entry.wolf());if(previous.isEmpty())dimensions.remove(old.dimension());}
        }
        dimensions.computeIfAbsent(entry.dimension(),key->new LinkedHashSet<>()).add(entry.wolf());
        owners.computeIfAbsent(entry.owner(),key->new LinkedHashSet<>()).add(entry.wolf());
        updateActive(entry);
    }
    public Optional<Companion> find(UUID wolf){return Optional.ofNullable(companions.get(wolf));}
    /** All preserved bonds, including stored images. */
    public int count(UUID owner){return owners.getOrDefault(owner,Set.of()).size();}
    /** Loaded and unloaded existing pets, plus uncertain/prepared activations; never STORED images. */
    public int activeCount(UUID owner){return activeOwners.getOrDefault(owner,Set.of()).size();}
    public List<Companion> forOwner(UUID owner){
        return owners.getOrDefault(owner,Set.of()).stream().map(companions::get).toList();
    }
    public List<Companion> inDimension(String dimension){
        return dimensions.getOrDefault(dimension,Set.of()).stream().map(companions::get).toList();
    }
    public boolean remember(Companion entry){
        var transfer=archives.get(entry.wolf());
        if(transfer!=null&&!transfer.phase().equals(WolfArchive.DONE)){
            var previous=companions.get(entry.wolf());
            if(previous!=null&&!previous.owner().equals(entry.owner()))
                throw new IllegalStateException("Cannot transfer an archived companion's ownership");
        }
        if(transfer!=null&&transfer.phase().equals(WolfArchive.DONE)){
            var previous=companions.get(entry.wolf());
            if(previous!=null&&!previous.owner().equals(entry.owner()))setArchive(entry.wolf(),null);
        }
        if(entry.equals(companions.get(entry.wolf())))return false;
        insert(entry);setDirty();return true;
    }
    /** Legacy saved UUID is a hold, not proof of a live wolf, owner change or guessed position. */
    public boolean preserveLegacy(UUID wolf,UUID owner,long run,String dimension){
        if(companions.containsKey(wolf))return false;
        return remember(new Companion(wolf,owner,run,dimension,0,false));
    }
    /** Only an observed final removal for this exact owner releases its slot. */
    public boolean died(UUID wolf,UUID owner){return release(wolf,owner);}
    /** Called only after an expired bond returns armor and relinquishes its actual native owner. */
    public boolean retired(UUID wolf,UUID owner){return release(wolf,owner);}
    private boolean release(UUID wolf,UUID owner){
        var entry=companions.get(wolf);
        if(entry==null||!entry.owner().equals(owner))return false;
        companions.remove(wolf);removeActive(owner,wolf);setArchive(wolf,null);
        var dimension=dimensions.get(entry.dimension());dimension.remove(wolf);
        if(dimension.isEmpty())dimensions.remove(entry.dimension());
        var list=owners.get(owner);list.remove(wolf);if(list.isEmpty())owners.remove(owner);
        setDirty();return true;
    }
    public Optional<WolfArchive> archive(UUID wolf){return Optional.ofNullable(archives.get(wolf));}
    public void putArchive(UUID wolf,WolfArchive archive){
        var companion=companions.get(wolf);
        if(companion==null||!archive.valid(wolf,companion.owner()))throw new IllegalArgumentException("Archive identity mismatch");
        setArchive(wolf,archive);setDirty();
    }
    public void sourceCleared(long run,Set<String> resetDimensions){
        for(var entry:new ArrayList<>(archives.entrySet())){
            var archive=entry.getValue();
            var old=companions.get(entry.getKey());
            if(archive.run()!=run||!archive.phase().equals(WolfArchive.PREPARED)||old==null
                    ||!resetDimensions.contains(archive.sourceDimension())
                    ||!old.dimension().equals(archive.sourceDimension()))continue;
            setArchive(entry.getKey(),archive.stored());
            insert(new Companion(old.wolf(),old.owner(),run,"cosmicdungeon:stored_companions",0,false,old.entityUuid()));
            setDirty();
        }
    }
    public boolean readyForSourceReset(UUID wolf){
        var entry=companions.get(wolf);var archive=archives.get(wolf);
        return entry!=null&&archive!=null&&archive.phase().equals(WolfArchive.PREPARED)
                &&archive.valid(wolf,entry.owner())&&archive.sourceDimension().equals(entry.dimension());
    }

}
