package net.goui.cosmicdungeon.playerclass.bogatyr;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

public final class BogatyrArchiveChecks {
    private static int checks;
    private static void check(boolean yes,String label){checks++;if(!yes)throw new AssertionError(label);}
    private static void rejects(Runnable action,String label){boolean rejected=false;try{action.run();}catch(RuntimeException e){rejected=true;}check(rejected,label);}
    @SuppressWarnings("unchecked") private static Codec<BogatyrCompanionData> codec()throws Exception{
        var field=BogatyrCompanionData.class.getDeclaredField("CODEC");field.setAccessible(true);
        return (Codec<BogatyrCompanionData>)field.get(null);
    }
    private static CompoundTag entity(UUID wolf,UUID owner){
        var tag=new CompoundTag();tag.putString("id","minecraft:wolf");
        tag.store("UUID",UUIDUtil.CODEC,wolf);tag.store("Owner",UUIDUtil.CODEC,owner);
        tag.putFloat("Health",13.25f);tag.putInt("Age",-1234);tag.putInt("CollarColor",11);
        tag.putString("CustomName","Beloved companion");
        var armor=new CompoundTag();armor.putString("id","minecraft:wolf_armor");
        armor.putInt("damage",19);armor.putInt("custom_color",0x336699);
        var equipment=new CompoundTag();equipment.put("body",armor);tag.put("equipment",equipment);
        var custom=new CompoundTag();custom.putString("authored","preserve me");tag.put("NeoForgeData",custom);
        return tag;
    }
    private static Path write(Path folder,int chunkX,int chunkZ,CompoundTag body,RegionFileVersion version,boolean external)throws Exception{
        var bytes=new ByteArrayOutputStream();
        try(var output=new DataOutputStream(version.wrap(bytes))){NbtIo.write(body,output);}
        byte[] compressed=bytes.toByteArray();
        check(compressed.length<4091,"Fixture fits one region sector");
        byte[] region=new byte[12288];var buffer=ByteBuffer.wrap(region);
        buffer.putInt(4*((chunkX&31)+32*(chunkZ&31)),(2<<8)|1);
        buffer.position(8192);buffer.putInt(external?1:compressed.length+1);
        buffer.put((byte)(version.getId()|(external?128:0)));
        if(external)Files.write(folder.resolve("c."+chunkX+"."+chunkZ+".mcc"),compressed);
        else buffer.put(compressed);
        Path path=folder.resolve("r."+(chunkX>>5)+"."+(chunkZ>>5)+".mca");
        Files.write(path,region);return path;
    }
    public static void main(String[] args)throws Exception{
        UUID wolf=UUID.randomUUID(),owner=UUID.randomUUID(),other=UUID.randomUUID();
        var source=entity(wolf,owner);
        var prepared=new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),71,"cosmicdungeon:d1_instance",123,"",0,source);
        check(prepared.valid(wolf,owner),"Live owned source image validates");
        check(!prepared.valid(wolf,other),"Another owner cannot claim image");
        check(!prepared.valid(other,owner),"Another UUID cannot claim image");
        check(!prepared.valid(wolf,null),"Missing owner is rejected");
        rejects(()->prepared.releasing("minecraft:overworld",456),"Cannot claim before source world reset");
        rejects(prepared::done,"Cannot mark an unclaimed source delivered");
        source.putFloat("Health",1);
        check(prepared.entity().getFloatOr("Health",0)==13.25f,"Archive copies source data before mutation");
        var leaked=prepared.entity();leaked.remove("equipment");
        check(prepared.entity().contains("equipment"),"Archive access cannot mutate stored armor");
        var stored=prepared.stored();
        check(stored.phase().equals(WolfArchive.STORED),"Successful reset releases archive for owner");
        rejects(stored::stored,"Source completion cannot be repeated as a new transfer");
        var releasing=stored.releasing("minecraft:overworld",456);
        check(releasing.valid(wolf,owner),"Prepared destination remains owner bound");
        check(releasing.entity().equals(prepared.entity()),"All source equipment, name, age and health preserved");
        rejects(()->releasing.releasing("minecraft:overworld",789),"A pending delivery cannot retarget");
        var done=releasing.done();
        check(done.valid(wolf,owner),"Small completed receipt keeps identity");
        check(!done.entity().contains("equipment")&&!done.entity().contains("Health"),"Completed receipt contains no stale recreatable pet image");
        rejects(done::stored,"Completed delivery cannot become a stored pet");
        rejects(()->done.releasing("minecraft:overworld",789),"Completed receipt cannot issue another wolf");
        rejects(()->new WolfArchive("unknown",UUID.randomUUID(),71,"source",0,"",0,source),"Unknown phase rejected");
        for(String invalid:List.of("id","UUID","Owner","Health")){
            var bad=prepared.entity();bad.remove(invalid);
            check(!new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),71,"source",0,"",0,bad).valid(wolf,owner),"Missing required "+invalid+" rejected");
        }
        for(float health:new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY}){
            var bad=prepared.entity();bad.putFloat("Health",health);
            check(!new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),71,"source",0,"",0,bad).valid(wolf,owner),"Dead/invalid health cannot be archived");
        }
        var passenger=prepared.entity();passenger.put("Passengers",new ListTag());
        check(!new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),71,"source",0,"",0,passenger).valid(wolf,owner),"Passenger trees cannot be duplicated");
        var codec=codec();var data=codec.parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow();
        data.remember(new BogatyrCompanionData.Companion(wolf,owner,71,"cosmicdungeon:d1_instance",123,true));
        data.putArchive(wolf,prepared);
        check(data.readyForSourceReset(wolf),"Verified prepared image permits source purge");
        rejects(()->data.remember(new BogatyrCompanionData.Companion(wolf,other,71,"source",0,true)),"Pending ownership cannot be stolen");
        for(String phase:List.of(WolfArchive.PREPARED,WolfArchive.STORED,WolfArchive.RELEASING,WolfArchive.DONE)){
            var stage=switch(phase){case "prepared"->prepared;case "stored"->stored;case "releasing"->releasing;default->done;};
            data.putArchive(wolf,stage);
            // The production save is NBT. JSON conversion changes numeric tag widths and is not a lossless entity format.
            var loaded=codec.parse(NbtOps.INSTANCE,codec.encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
            check(loaded.archive(wolf).orElseThrow().equals(stage),"Restart retains transfer stage "+phase);
            check(loaded.count(owner)==1,"Transfer occupies exactly one roster slot "+phase);
        }
        data.putArchive(wolf,prepared);data.sourceCleared(70,Set.of("cosmicdungeon:d1_instance"));
        check(data.archive(wolf).orElseThrow().phase().equals(WolfArchive.PREPARED),"Unrelated run cannot release archive");
        data.remember(new BogatyrCompanionData.Companion(wolf,owner,71,"minecraft:overworld",999,true));
        data.sourceCleared(71,Set.of("cosmicdungeon:d1_instance"));
        check(data.archive(wolf).orElseThrow().phase().equals(WolfArchive.PREPARED),"Wolf moved outside cannot be recreated by another world's reset");
        check(data.inDimension("minecraft:overworld").size()==1,"Moved original retains its real location hold");
        data.remember(new BogatyrCompanionData.Companion(wolf,owner,71,"cosmicdungeon:d1_instance",123,true));
        data.sourceCleared(71,Set.of("cosmicdungeon:unrelated"));
        check(data.archive(wolf).orElseThrow().phase().equals(WolfArchive.PREPARED),"Run ID alone cannot prove the source dimension was cleared");
        data.sourceCleared(71,Set.of("cosmicdungeon:d1_instance"));
        check(data.archive(wolf).orElseThrow().phase().equals(WolfArchive.STORED),"Correct reset releases exact archive");
        check(data.inDimension("cosmicdungeon:d1_instance").isEmpty(),"Stored pet no longer blocks source slot");
        check(data.count(owner)==1,"Stored pet remains permanent owner property");
        data.sourceCleared(71,Set.of("cosmicdungeon:d1_instance"));
        check(data.archive(wolf).orElseThrow().phase().equals(WolfArchive.STORED),"Repeated reset does not replay delivery");
        data.putArchive(wolf,done);
        data.remember(new BogatyrCompanionData.Companion(wolf,other,72,"minecraft:overworld",456,true));
        check(data.archive(wolf).isEmpty()&&data.count(other)==1,"Later observed ownership change retires old completed receipt");
        data.putArchive(wolf,new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),72,"source",456,"",0,entity(wolf,other)));
        data.died(wolf,other);
        check(data.archive(wolf).isEmpty()&&data.count(other)==0,"Actual death removes archive and prevents resurrection");

        UUID bond=UUID.randomUUID();
        var separated=entity(wolf,owner);var identity=separated.getCompoundOrEmpty("NeoForgeData");
        WolfIdentity.set(identity,bond);
        var bonded=new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),99,"cosmicdungeon:source",0,"",0,separated);
        check(bonded.valid(bond,owner),"Archive binds permanent ID independently from cloned physical UUID");
        check(!bonded.valid(wolf,owner),"Physical UUID cannot impersonate an explicit bond");
        check(bonded.entity().read("UUID",UUIDUtil.CODEC).orElseThrow().equals(wolf),"Source physical UUID retained for save proof");
        var completed=bonded.stored().releasing("minecraft:overworld",100).done();
        check(completed.valid(bond,owner),"Delivered receipt retains permanent identity");
        check(completed.entity().read("UUID",UUIDUtil.CODEC).orElseThrow().equals(bond),"Restored native UUID becomes globally unique bond ID");
        check(!completed.entity().contains("equipment"),"Separate identity receipt still cannot recreate armor");
        var nativeSave=WolfArchive.CODEC.parse(NbtOps.INSTANCE, WolfArchive.CODEC.encodeStart(NbtOps.INSTANCE,bonded).getOrThrow()).getOrThrow();
        check(nativeSave.equals(bonded),"Separate identity archive round-trips exact NBT");
        var invalid=separated.copy();invalid.getCompoundOrEmpty("NeoForgeData").putString(WolfIdentity.KEY,"broken");
        check(!new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),99,"source",0,"",0,invalid).valid(wolf,owner),
                "Malformed explicit bond cannot fall back to source physical UUID");
        var physicalDirectory=codec.parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow();
        physicalDirectory.remember(new BogatyrCompanionData.Companion(bond,owner,99,"cosmicdungeon:source",0,true,wolf));
        physicalDirectory.putArchive(bond,bonded);physicalDirectory.sourceCleared(99,Set.of("cosmicdungeon:source"));
        check(physicalDirectory.find(bond).orElseThrow().entityUuid().equals(wolf),"Stored source retains original native identity");
        check(physicalDirectory.archive(bond).orElseThrow().valid(bond,owner),"Reset retains permanent identity archive");
        var runDelivery=bonded.stored().releasing("cosmicdungeon:next_slot",900,100);
        check(runDelivery.targets("cosmicdungeon:next_slot",100),"Recall binds exact dungeon run and dimension");
        check(!runDelivery.targets("cosmicdungeon:next_slot",101),"Reused instance slot is not the previous delivery");
        check(!runDelivery.targets("cosmicdungeon:other_slot",100),"Another party dimension is not accepted");
        check(!runDelivery.targets("cosmicdungeon:next_slot",0),"Main-world claim cannot retarget a dungeon delivery");
        check(runDelivery.done().targets("cosmicdungeon:next_slot",100),"Done receipt retains destination run identity");
        check(stored.releasing("minecraft:overworld",5).targetRun()==0,"Existing main-world recovery keeps zero scope");
        var oldFormat=(CompoundTag)WolfArchive.CODEC.encodeStart(NbtOps.INSTANCE,releasing).getOrThrow();
        oldFormat.remove("target_run");
        check(WolfArchive.CODEC.parse(NbtOps.INSTANCE,oldFormat).getOrThrow().targetRun()==0,"Old delivery saves default to main-world scope");
        var runReload=WolfArchive.CODEC.parse(NbtOps.INSTANCE,WolfArchive.CODEC.encodeStart(NbtOps.INSTANCE,runDelivery).getOrThrow()).getOrThrow();
        check(runReload.equals(runDelivery),"Interrupted dungeon delivery survives native save");
        rejects(()->bonded.stored().releasing("destination",5,-1),"Negative destination run is invalid");
        rejects(()->new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),99,"source",0,"destination",1,separated,100),
                "Source preparation cannot masquerade as destination delivery");
        var pendingData=codec.parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow();
        pendingData.remember(new BogatyrCompanionData.Companion(bond,owner,99,"cosmicdungeon:stored_companions",0,false,wolf));
        pendingData.putArchive(bond,runDelivery);
        check(pendingData.pendingInDimension("cosmicdungeon:next_slot").size()==1,"Pending target blocks reset before entity insertion");
        check(pendingData.inDimension("cosmicdungeon:next_slot").isEmpty(),"Target guard does not rely on source directory location");
        var pendingReload=codec.parse(NbtOps.INSTANCE,codec.encodeStart(NbtOps.INSTANCE,pendingData).getOrThrow()).getOrThrow();
        check(pendingReload.pendingInDimension("cosmicdungeon:next_slot").size()==1,"Pending target index rebuilds after restart");
        check(pendingReload.pendingInDimension("cosmicdungeon:other_slot").isEmpty(),"Unrelated instance target is not blocked");
        pendingReload.putArchive(bond,runDelivery.done());
        check(pendingReload.pendingInDimension("cosmicdungeon:next_slot").isEmpty(),"Completed delivery removes only pending target hold");
        pendingReload.putArchive(bond,runDelivery);pendingReload.died(bond,owner);
        check(pendingReload.pendingInDimension("cosmicdungeon:next_slot").isEmpty(),"Actual death removes pending target index");
        pendingData.remember(new BogatyrCompanionData.Companion(bond,owner,100,"cosmicdungeon:next_slot",900,true,bond));
        var secondImage=separated.copy();secondImage.store("UUID",UUIDUtil.CODEC,bond);
        var resetAgain=new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),100,"cosmicdungeon:next_slot",900,"",0,secondImage);
        pendingData.putArchive(bond,resetAgain);
        check(pendingData.pendingInDimension("cosmicdungeon:next_slot").isEmpty(),"Exact observed destination can become the next source archive");
        check(pendingData.readyForSourceReset(bond),"Interrupted but observed destination is protected through reset");
        pendingData.sourceCleared(100,Set.of("cosmicdungeon:next_slot"));
        check(pendingData.archive(bond).orElseThrow().phase().equals(WolfArchive.STORED),"Second verified reset returns one stored bond");
        check(pendingData.count(owner)==1,"Repeated run archive retains one lifetime cap slot");
        var liveEntry=new BogatyrCompanionData.Companion(bond,owner,99,"minecraft:overworld",77,true,wolf);
        var reserved=new WolfArchive(WolfArchive.RESERVED,UUID.randomUUID(),99,"minecraft:overworld",77,"",0,separated);
        check(reserved.sourceMatches(liveEntry),"Reserved archive binds exact source owner/native UUID/location");
        check(!reserved.sourceMatches(new BogatyrCompanionData.Companion(bond,owner,99,"minecraft:overworld",78,true,wolf)),
                "A moved source cannot authorize absence at the old location");
        check(!reserved.sourceMatches(new BogatyrCompanionData.Companion(bond,owner,99,"minecraft:the_nether",77,true,wolf)),
                "Another dimension cannot prove source removal");
        check(!reserved.sourceMatches(new BogatyrCompanionData.Companion(bond,owner,99,"minecraft:overworld",77,false,wolf)),
                "Unknown location cannot authorize source removal");
        check(!reserved.sourceMatches(new BogatyrCompanionData.Companion(bond,other,99,"minecraft:overworld",77,true,wolf)),
                "Another owner cannot arm source removal");
        check(!reserved.sourceMatches(new BogatyrCompanionData.Companion(bond,owner,99,"minecraft:overworld",77,true,other)),
                "Another physical wolf cannot fulfill source reservation");
        rejects(reserved::stored,"World reset acknowledgement cannot release an outside reservation");
        rejects(reserved::sourceRemoved,"Reservation alone cannot accept absence or create a pet");
        rejects(()->reserved.releasing("destination",1,100),"Reservation cannot skip durable source removal");
        var removing=reserved.removing();
        check(removing.sourceMatches(liveEntry),"Armed removal retains exact original identity");
        rejects(removing::removing,"Armed removal cannot become another reservation");
        rejects(removing::stored,"Dungeon reset receipt cannot substitute for source removal proof");
        rejects(()->removing.releasing("destination",1,100),"Armed removal alone cannot start destination creation");
        var withdrawn=removing.sourceRemoved();
        check(withdrawn.phase().equals(WolfArchive.STORED),"Verified source removal releases one stored image");
        check(withdrawn.entity().equals(reserved.entity()),"Withdrawal preserves exact native armor/name/age/health");
        rejects(withdrawn::sourceRemoved,"Completed withdrawal cannot replay its source transition");
        check(withdrawn.releasing("cosmicdungeon:next_slot",5,100).valid(bond,owner),"Withdrawn pet remains owner-bound through D1 recall");
        for(var stage:List.of(reserved,removing,withdrawn)){
            var savedStage=WolfArchive.CODEC.parse(NbtOps.INSTANCE,WolfArchive.CODEC.encodeStart(NbtOps.INSTANCE,stage).getOrThrow()).getOrThrow();
            check(savedStage.equals(stage),"Restart retains exact outside transfer stage "+stage.phase());
        }
        var outsideData=codec.parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow();
        outsideData.remember(liveEntry);outsideData.putArchive(bond,removing);
        outsideData.sourceCleared(99,Set.of("minecraft:overworld"));
        check(outsideData.archive(bond).orElseThrow().phase().equals(WolfArchive.REMOVING),"Bulk reset cannot release armed outside removal");
        check(!outsideData.readyForSourceReset(bond),"Outside removal does not authorize destructive world reset");
        outsideData.died(bond,owner);
        check(outsideData.archive(bond).isEmpty(),"Observed final death cancels pending outside transfer");
        var rejectedDelivery=runDelivery.rejectedDelivery();
        check(rejectedDelivery.phase().equals(WolfArchive.STORED),"Confirmed never-added destination returns pet to stored");
        check(rejectedDelivery.entity().equals(runDelivery.entity()),"Rejected insertion preserves exact original image");
        check(rejectedDelivery.targetRun()==0&&rejectedDelivery.targetDimension().isEmpty(),"Confirmed rejection removes destination reservation");
        check(rejectedDelivery.releasing("cosmicdungeon:other_slot",300,101).targets("cosmicdungeon:other_slot",101),
                "Known canceled insertion can be retried at another authorized destination");
        rejects(reserved::rejectedDelivery,"Source reservation cannot masquerade as canceled destination");
        rejects(done::rejectedDelivery,"Completed pet cannot become a stored duplicate");
        var reviewState=new BogatyrReview.State(liveEntry,reserved);
        var preview=new net.goui.cosmicdungeon.item.identity.ItemAuthoringPlan<BogatyrReview.State>("token",200,reviewState,reviewState);
        check(preview.accepts("token",100,true,reviewState,false,Objects::equals),"Exact source review can apply");
        check(!preview.accepts("wrong",100,true,reviewState,false,Objects::equals),"Another preview token cannot apply");
        check(!preview.accepts("token",200,true,reviewState,false,Objects::equals),"Expired source preview cannot apply");
        check(!preview.accepts("token",100,false,reviewState,false,Objects::equals),"Source moving after preview invalidates it");
        check(!preview.accepts("token",100,true,new BogatyrReview.State(liveEntry,removing),false,Objects::equals),
                "Concurrent transaction stage change invalidates review");
        check(!preview.accepts("token",100,true,new BogatyrReview.State(
                new BogatyrCompanionData.Companion(bond,other,99,"minecraft:overworld",77,true,wolf),reserved),false,Objects::equals),
                "Concurrent owner change invalidates review");
        preview.committed(false);
        check(!preview.accepts("token",100,true,reviewState,false,Objects::equals),"Applied review token cannot replay");
        var pack=codec.parse(JsonOps.INSTANCE,new JsonObject()).getOrThrow();
        var bonds=new ArrayList<UUID>();
        for(int i=0;i<7;i++){
            UUID id=UUID.randomUUID();bonds.add(id);
            pack.remember(new BogatyrCompanionData.Companion(id,owner,201,"cosmicdungeon:pack_source",i,true));
            if(i>=5)pack.putArchive(id,new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),201,
                    "cosmicdungeon:pack_source",i,"",0,entity(id,owner)).stored());
        }
        check(pack.count(owner)==7&&pack.activeCount(owner)==5,"Stored lifetime bonds do not consume active slots");
        var packReload=codec.parse(NbtOps.INSTANCE,codec.encodeStart(NbtOps.INSTANCE,pack).getOrThrow()).getOrThrow();
        check(packReload.activeCount(owner)==5&&packReload.count(owner)==7,"Restart rebuilds active and lifetime indices separately");
        var retiringSource=new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),201,"cosmicdungeon:pack_source",0,"",0,entity(bonds.get(0),owner));
        pack.putArchive(bonds.get(0),retiringSource);
        check(pack.activeCount(owner)==5,"Prepared source still counts while the actual wolf exists");
        pack.sourceCleared(201,Set.of("cosmicdungeon:pack_source"));
        check(pack.activeCount(owner)==4&&pack.count(owner)==7,"Verified reset frees active slot without losing any bond");
        var activating=pack.archive(bonds.get(5)).orElseThrow().releasing("minecraft:overworld",77);
        pack.putArchive(bonds.get(5),activating);
        check(pack.activeCount(owner)==5,"Prepared destination reserves active slot before entity insertion");
        pack.putArchive(bonds.get(5),activating);
        check(pack.activeCount(owner)==5,"Repeated delivery record cannot double-count a slot");
        pack.putArchive(bonds.get(5),activating.rejectedDelivery());
        check(pack.activeCount(owner)==4,"Verified canceled insertion frees only its reserved active slot");
        pack.putArchive(bonds.get(5),activating.done());
        check(pack.activeCount(owner)==5,"Completed actual pet remains active");
        pack.died(bonds.get(1),owner);
        check(pack.activeCount(owner)==4&&pack.count(owner)==6,"Observed death removes one active slot and bond");
        pack.retired(bonds.get(2),owner);
        check(pack.activeCount(owner)==3&&pack.count(owner)==5,"Configured expiry retires exact active bond");
        pack.remember(new BogatyrCompanionData.Companion(bonds.get(3),other,201,"minecraft:overworld",3,true));
        check(pack.activeCount(owner)==2&&pack.activeCount(other)==1,"Verified owner transfer moves active index");
        check(pack.count(owner)==4&&pack.count(other)==1,"Owner transfer also preserves correct all-owned index");
        var outsideReserve=new WolfArchive(WolfArchive.RESERVED,UUID.randomUUID(),201,"minecraft:overworld",5,"",0,entity(bonds.get(5),owner));
        pack.putArchive(bonds.get(5),outsideReserve);
        check(pack.activeCount(owner)==2,"Reserved live source stays active");
        pack.putArchive(bonds.get(5),outsideReserve.removing());
        check(pack.activeCount(owner)==2,"Armed but unverified removal keeps its slot");
        pack.putArchive(bonds.get(5),outsideReserve.removing().sourceRemoved());
        check(pack.activeCount(owner)==1&&pack.count(owner)==4,"Only verified withdrawal frees the active slot");
        UUID unknown=UUID.randomUUID();pack.preserveLegacy(unknown,owner,201,"cosmicdungeon:pack_source");
        check(pack.activeCount(owner)==2,"Unlocated legacy actual pet counts conservatively");
        var rebuiltPack=codec.parse(NbtOps.INSTANCE,codec.encodeStart(NbtOps.INSTANCE,pack).getOrThrow()).getOrThrow();
        check(rebuiltPack.activeCount(owner)==2&&rebuiltPack.activeCount(other)==1,"All transition indices rebuild exactly");
        check(rebuiltPack.forOwner(other).stream().noneMatch(e->e.owner().equals(owner)),
                "Owner-filtered selection cannot return another player's stored or active bond");
        Path folder=Files.createTempDirectory("cosmicdungeon-wolf-region-checks-");
        try{
            UUID review=UUID.randomUUID();var evidence=new CompoundTag();
            evidence.put("original",WolfArchive.CODEC.encodeStart(NbtOps.INSTANCE,reserved).getOrThrow());
            evidence.put("proposed",WolfArchive.CODEC.encodeStart(NbtOps.INSTANCE,removing).getOrThrow());
            var preparedFile=CompanionReviewFiles.write(folder,review,false,evidence);
            check(NbtIo.readCompressed(preparedFile,NbtAccounter.unlimitedHeap()).equals(evidence),
                    "Review preserves exact original and proposed source NBT before mutation");
            byte[] originalBytes=Files.readAllBytes(preparedFile);
            boolean overwriteRejected=false;
            try{CompanionReviewFiles.write(folder,review,false,new CompoundTag());}
            catch(IOException expected){overwriteRejected=true;}
            check(overwriteRejected&&Arrays.equals(originalBytes,Files.readAllBytes(preparedFile)),
                    "Review evidence cannot overwrite an earlier record");
            var appliedFile=CompanionReviewFiles.write(folder,review,true,evidence);
            check(!appliedFile.equals(preparedFile)&&Files.exists(preparedFile),"Applied receipt retains original prepared evidence");
            check(NbtIo.readCompressed(appliedFile,NbtAccounter.unlimitedHeap()).equals(evidence),
                    "Applied receipt preserves exact reviewed identities");
            int chunkX=-34,chunkZ=97;var chunk=new CompoundTag();chunk.putIntArray("Position",new int[]{chunkX,chunkZ});
            var entities=new ListTag();entities.add(prepared.entity());chunk.put("Entities",entities);
            for(var version:List.of(RegionFileVersion.VERSION_GZIP,RegionFileVersion.VERSION_DEFLATE,
                    RegionFileVersion.VERSION_NONE,RegionFileVersion.VERSION_LZ4)){
                for(boolean external:new boolean[]{false,true}){
                    var path=write(folder,chunkX,chunkZ,chunk,version,external);
                    byte[] before=Files.readAllBytes(path);
                    var actual=ReadOnlyEntityRegion.read(folder,chunkX,chunkZ);
                    check(actual.equals(chunk),"Read saved entity compression "+version.getId()+" external="+external);
                    check(ReadOnlyEntityRegion.find(actual,wolf).orElseThrow().equals(prepared.entity()),"Exact armor and owner recovered from region");
                    check(Arrays.equals(before,Files.readAllBytes(path)),"Verification never mutates region bytes");
                    check(ReadOnlyEntityRegion.find(actual,other).isEmpty(),"Unrelated UUID is never accepted");
                }
            }
            var missing=ReadOnlyEntityRegion.read(folder,10000,10000);
            check(missing==null,"Missing region creates no file or invented entity");
            var duplicate=chunk.copy();duplicate.getListOrEmpty("Entities").add(prepared.entity());
            rejects(()->ReadOnlyEntityRegion.find(duplicate,wolf),"Duplicate saved UUID blocks verification");
            var wrong=chunk.copy();wrong.putIntArray("Position",new int[]{1,1});
            write(folder,chunkX,chunkZ,wrong,RegionFileVersion.VERSION_NONE,false);
            boolean rejected=false;try{ReadOnlyEntityRegion.read(folder,chunkX,chunkZ);}catch(IOException expected){rejected=true;}
            check(rejected,"Misplaced entity chunk is rejected");
            var path=write(folder,chunkX,chunkZ,chunk,RegionFileVersion.VERSION_NONE,false);
            Files.write(path,new byte[12]);
            rejected=false;try{ReadOnlyEntityRegion.read(folder,chunkX,chunkZ);}catch(IOException expected){rejected=true;}
            check(rejected,"Truncated region cannot verify delivery");
        }finally{try(var paths=Files.walk(folder)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
        System.out.println(checks+" Bogatyr archive and entity-region checks passed");
    }
}
