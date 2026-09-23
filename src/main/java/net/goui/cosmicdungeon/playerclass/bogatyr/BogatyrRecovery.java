package net.goui.cosmicdungeon.playerclass.bogatyr;

import net.goui.cosmicdungeon.Config;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1Members;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Exact wolf state crosses destructive instance reset through a verified archive, never a stale directory. */
public final class BogatyrRecovery {
    private record Loading(String dimension,long position,long started,CompletableFuture<?> future){}
    private static final Map<UUID,Loading> LOADS=new LinkedHashMap<>();
    private static final Map<UUID,Long> LAST_ACTION=new HashMap<>();
    private static long budgetTick=Long.MIN_VALUE;
    private static int snapshots;
    private BogatyrRecovery(){}
    public static void clear(){LOADS.clear();LAST_ACTION.clear();BogatyrIdentity.clear();BogatyrReview.clear();budgetTick=Long.MIN_VALUE;snapshots=0;}
    public static void clearPlayer(UUID owner){LAST_ACTION.remove(owner);BogatyrReview.clearPlayer(owner);}
    static ServerLevel level(MinecraftServer server,String id){
        var location=ResourceLocation.tryParse(id);
        return location==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,location));
    }
    private static long now(MinecraftServer server){return server.overworld().getGameTime();}
    private static void budget(MinecraftServer server){
        long tick=now(server);if(tick!=budgetTick){budgetTick=tick;snapshots=0;}
        LOADS.entrySet().removeIf(e->tick-e.getValue().started()>Config.WOLF_RECOVERY_LOAD_TICKS.get());
    }
    static Wolf loaded(MinecraftServer server,BogatyrCompanionData.Companion entry){
        var archive=BogatyrCompanionData.get(server).archive(entry.wolf()).orElse(null);
        var addresses=new LinkedHashMap<String,UUID>();addresses.put(entry.dimension(),entry.entityUuid());
        if(archive!=null&&archive.phase().equals(WolfArchive.RELEASING))
            addresses.put(archive.targetDimension(),entry.wolf());
        Wolf found=null;
        for(var address:addresses.entrySet()){
            var world=level(server,address.getKey());if(world==null)continue;
            var entity=world.getEntity(address.getValue());if(entity==null)continue;
            if(!(entity instanceof Wolf wolf)||!BogatyrIdentity.id(wolf).equals(entry.wolf())
                    ||!entry.owner().equals(BogatyrCompanions.owner(wolf))||(found!=null&&found!=wolf))
                throw new IllegalStateException("Conflicting live companion identity "+entry.wolf());
            found=wolf;
        }
        return found;
    }
    private static boolean liveNativeUuid(MinecraftServer server,UUID id){
        for(var world:server.getAllLevels())if(world.getEntity(id)!=null)return true;
        return false;
    }
    /** Uses a short-lived vanilla loading ticket; no force-loaded chunks or blocking future join. */
    static boolean ready(MinecraftServer server,UUID id,ServerLevel level,long position){
        var chunk=new ChunkPos(BlockPos.of(position));
        if(level.areEntitiesLoaded(chunk.toLong())){LOADS.remove(id);return true;}
        var pending=LOADS.get(id);
        if(pending!=null&&!pending.dimension().equals(level.dimension().location().toString())){LOADS.remove(id);pending=null;}
        if(pending==null){
            if(LOADS.size()>=Config.WOLF_RECOVERY_CHUNKS.get())return false;
            var future=level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PLAYER_SPAWN,chunk,0);
            LOADS.put(id,new Loading(level.dimension().location().toString(),position,now(server),future));
        }else if(pending.future().isCompletedExceptionally())return false;
        else level.getChunkSource().addTicketWithRadius(TicketType.PLAYER_SPAWN,chunk,0);
        return false;
    }
    public static CompoundTag image(Wolf wolf){
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,wolf.registryAccess());
        if(!wolf.save(output))throw new IllegalStateException("Companion could not be serialized");
        return output.buildResult();
    }
    static CompoundTag sourceImage(Wolf wolf){
        var image=image(wolf);
        var persistent=image.getCompoundOrEmpty("NeoForgeData").copy();
        if(persistent.contains(WolfArchive.MARKER+"_noai")){
            image.putBoolean("NoAI",persistent.getBooleanOr(WolfArchive.MARKER+"_noai",false));
            image.putBoolean("Invulnerable",persistent.getBooleanOr(WolfArchive.MARKER+"_invulnerable",false));
        }
        persistent.remove(WolfArchive.MARKER);persistent.remove(WolfArchive.MARKER+"_noai");
        persistent.remove(WolfArchive.MARKER+"_invulnerable");image.put("NeoForgeData",persistent);
        return image;
    }
    public static boolean prepareReset(MinecraftServer server,DungeonRunRegistryData.RunRecord run){
        if(run==null||!run.dungeonId().equals("dungeon_1"))return true;
        budget(server);BogatyrCompanions.preserveRun(server,run);
        var data=BogatyrCompanionData.get(server);
        boolean ready=true,changed=false;
        for(String dimension:run.dungeonDimensionIds()){
            if(data.dimensionHeld(dimension))return false;
            // An interrupted delivery may still be indexed as stored. Its exact target must be
            // resolved before world replacement, even if its owner has left the run roster.
            for(var entry:data.pendingInDimension(dimension)){
                var archive=data.archive(entry.wolf()).orElseThrow();
                if(!archive.targets(dimension,run.runId()))return false;
                var destination=level(server,dimension);if(destination==null)return false;
                Wolf wolf=loaded(server,entry);
                if(wolf==null){
                    ready(server,entry.wolf(),destination,archive.targetPosition());return false;
                }
                if(wolf.level()!=destination||!ownsDelivery(wolf,entry.owner(),archive))return false;
                BogatyrCompanions.track(wolf,true);
                if(!data.find(entry.wolf()).orElseThrow().dimension().equals(dimension))return false;
            }
        }
        for(String dimension:run.dungeonDimensionIds())for(var entry:data.inDimension(dimension)){
            if(data.readyForSourceReset(entry.wolf()))continue;
            if(!entry.located()){ready=false;continue;}
            var source=level(server,entry.dimension());
            if(source==null){ready=false;continue;}
            Wolf wolf=loaded(server,entry);
            if(wolf==null){
                ready(server,entry.wolf(),source,entry.position());
                ready=false;continue;
            }
            LOADS.remove(entry.wolf());
            if(!entry.owner().equals(BogatyrCompanions.owner(wolf))||!wolf.isAlive()
                    ||wolf.isPassenger()||wolf.isVehicle()){ready=false;continue;}
            if(!run.containsDimension(((ServerLevel)wolf.level()).dimension())){
                BogatyrCompanions.track(wolf,true);changed=true;continue;
            }
            if(snapshots>=Config.WOLF_RECOVERY_SNAPSHOTS.get()){ready=false;continue;}
            var archive=new WolfArchive(WolfArchive.PREPARED,UUID.randomUUID(),run.runId(),
                    wolf.level().dimension().location().toString(),wolf.blockPosition().asLong(),"",0,sourceImage(wolf));
            data.putArchive(BogatyrIdentity.id(wolf),archive);snapshots++;changed=true;
            pin(wolf,archive);
        }
        // A failed save leaves originals present and the world guarded; a later attempt retries.
        if(changed||ready)return data.flushVerified(server)&&ready;
        return false;
    }
    static void pin(Wolf wolf,WolfArchive archive){
        wolf.getPersistentData().putBoolean(WolfArchive.MARKER+"_noai",archive.originalNoAi());
        wolf.getPersistentData().putBoolean(WolfArchive.MARKER+"_invulnerable",archive.originalInvulnerable());
        wolf.getPersistentData().putString(WolfArchive.MARKER,archive.transaction().toString());
        wolf.setNoAi(true);wolf.setInvulnerable(true);wolf.setOrderedToSit(true);
        wolf.setTarget(null);wolf.getNavigation().stop();wolf.setDeltaMovement(Vec3.ZERO);
    }
    public static void onObserved(Wolf wolf){
        if(!(wolf.level() instanceof ServerLevel level)||!wolf.isAddedToLevel())return;
        if(!BogatyrWolfEvents.managed(wolf)||!BogatyrIdentity.observe(wolf))return;
        var data=BogatyrCompanionData.get(level.getServer());
        var archive=data.archive(BogatyrIdentity.id(wolf)).orElse(null);
        if(archive==null){
            // If archive writing failed before a crash, the source wolf remains authoritative.
            if(wolf.getPersistentData().contains(WolfArchive.MARKER+"_noai")){
                wolf.setNoAi(wolf.getPersistentData().getBooleanOr(WolfArchive.MARKER+"_noai",false));
                wolf.setInvulnerable(wolf.getPersistentData().getBooleanOr(WolfArchive.MARKER+"_invulnerable",false));
                clearMarker(wolf);
            }
            return;
        }
        if(data.find(BogatyrIdentity.id(wolf)).filter(c->c.owner().equals(BogatyrCompanions.owner(wolf))).isEmpty())return;
        if(!archive.phase().equals(WolfArchive.DONE)){pin(wolf,archive);return;}
        if(wolf.getPersistentData().getStringOr(WolfArchive.MARKER,"").equals(archive.transaction().toString())){
            wolf.setNoAi(archive.originalNoAi());
            wolf.setInvulnerable(archive.originalInvulnerable());
            wolf.setOrderedToSit(true);clearMarker(wolf);
        }
    }
    private static void clearMarker(Wolf wolf){
        wolf.getPersistentData().remove(WolfArchive.MARKER);
        wolf.getPersistentData().remove(WolfArchive.MARKER+"_noai");
        wolf.getPersistentData().remove(WolfArchive.MARKER+"_invulnerable");
    }
    public static boolean held(Wolf wolf){
        if(!(wolf.level() instanceof ServerLevel level))return false;
        if(BogatyrIdentity.held(wolf))return true;
        return BogatyrCompanionData.get(level.getServer()).archive(BogatyrIdentity.id(wolf))
                .filter(a->!a.phase().equals(WolfArchive.DONE)).isPresent();
    }
    public static boolean sourceCleared(MinecraftServer server,long run){
        var data=BogatyrCompanionData.get(server);
        var previous=new LinkedHashMap<UUID,BogatyrCompanionData.Companion>();
        var archives=new LinkedHashMap<UUID,WolfArchive>();
        for(var record:DungeonRunRegistryData.get(server).getRun(run).stream().toList())
            for(String dimension:record.dungeonDimensionIds())for(var entry:data.inDimension(dimension)){
                var archive=data.archive(entry.wolf()).orElse(null);
                if(archive!=null&&archive.run()==run&&archive.phase().equals(WolfArchive.PREPARED)){
                    previous.put(entry.wolf(),entry);archives.put(entry.wolf(),archive);
                }
            }
        var resetDimensions=DungeonRunRegistryData.get(server).getRun(run)
                .map(record->Set.copyOf(record.dungeonDimensionIds())).orElse(Set.of());
        data.sourceCleared(run,resetDimensions);
        if(data.flushVerified(server))return true;
        previous.forEach((id,entry)->{data.remember(entry);data.putArchive(id,archives.get(id));});
        return false;
    }
    static Path entityFolder(ServerLevel level){
        var root=level.getServer().getWorldPath(LevelResource.ROOT);
        if(level.dimension().equals(Level.OVERWORLD))return root.resolve("entities");
        if(level.dimension().equals(Level.NETHER))return root.resolve("DIM-1/entities");
        if(level.dimension().equals(Level.END))return root.resolve("DIM1/entities");
        var id=level.dimension().location();
        return root.resolve("dimensions").resolve(id.getNamespace()).resolve(id.getPath()).resolve("entities");
    }
    static boolean saved(Wolf wolf,CompoundTag expected)throws Exception{
        var level=(ServerLevel)wolf.level();
        level.save(null,true,false); // mapped saveAll includes entity-worker flush(true).
        var actual=ReadOnlyEntityRegion.find(ReadOnlyEntityRegion.read(entityFolder(level),
                wolf.blockPosition().getX()>>4,wolf.blockPosition().getZ()>>4),wolf.getUUID());
        return actual.filter(expected::equals).isPresent();
    }
    private static boolean ownsDelivery(Wolf wolf,UUID owner,WolfArchive archive){
        return owner.equals(BogatyrCompanions.owner(wolf))
                &&wolf.getPersistentData().getStringOr(WolfArchive.MARKER,"").equals(archive.transaction().toString());
    }
    public static int call(ServerPlayer player){return call(player,null);}
    public static int call(ServerPlayer player,UUID selected){
        var run=D1Members.run(player.level()).orElse(null);
        if(run==null||!D1Members.inside(player,run)||!"bogatyr".equals(ClassData.getClassId(player))){
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Companion recall requires your active D1 Bogatyr run."));
            return 0;
        }
        return deliver(player,run.runId(),selected);
    }
    public static int recover(ServerPlayer player){return recover(player,null);}
    public static int recover(ServerPlayer player,UUID selected){
        var run=DungeonRunRegistryData.get(player.level().getServer()).findRunForPlayer(player.getUUID()).orElse(null);
        if(run!=null||!player.level().dimension().equals(Level.OVERWORLD)||!player.isAlive()||player.isSpectator()){
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Finish the dungeon reset and recover companions in the main world."));
            return 0;
        }
        return deliver(player,0,selected);
    }
    private static boolean destinationExists(MinecraftServer server,WolfArchive archive,UUID owner){
        var destination=level(server,archive.targetDimension());
        if(destination==null)return false;
        if(archive.targetRun()==0)return destination.dimension().equals(Level.OVERWORLD);
        var run=DungeonRunRegistryData.get(server).getRun(archive.targetRun()).orElse(null);
        return run!=null&&run.stateEnum()==DungeonRunState.ACTIVE&&run.dungeonId().equals("dungeon_1")
                &&run.containsPlayer(owner)&&!run.isCompletionExited(owner)&&run.containsDimension(destination.dimension());
    }
    private static int deliver(ServerPlayer player,long targetRun,UUID selected){
        var server=player.level().getServer();budget(server);
        long tick=now(server);
        if(!WolfBehaviourRules.refresh(tick,LAST_ACTION.getOrDefault(player.getUUID(),Long.MIN_VALUE),Config.WOLF_COMMAND_TICKS.get())){
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Wait a moment before requesting another companion."));
            return 0;
        }
        LAST_ACTION.put(player.getUUID(),tick);
        var data=BogatyrCompanionData.get(server);
        for(var entry:data.forOwner(player.getUUID())){
            if(selected!=null&&!selected.equals(entry.wolf()))continue;
            var archive=data.archive(entry.wolf()).orElse(null);
            if(archive!=null&&archive.phase().equals(WolfArchive.PREPARED))continue;
            boolean existing=archive==null||archive.phase().equals(WolfArchive.DONE);
            if(existing&&(targetRun==0||!entry.dimension().equals("minecraft:overworld")))continue;
            if(archive!=null&&archive.phase().equals(WolfArchive.RELEASING)&&!archive.targets(
                    player.level().dimension().location().toString(),targetRun))continue;
            try{
                if(snapshots>=Config.WOLF_RECOVERY_SNAPSHOTS.get()){
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Companion transfers are busy. Retry shortly."));
                    return 0;
                }
                snapshots++;
                if(existing&&data.activeCount(entry.owner())>Config.WOLF_CAP.get())
                    throw new IllegalStateException("Your active pack exceeds the current limit; the existing wolf stays where it is");
                if(existing||archive.phase().equals(WolfArchive.RESERVED)||archive.phase().equals(WolfArchive.REMOVING)){
                    if(!BogatyrWithdrawal.prepare(server,entry)){
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Loading your companion's source location. Repeat the command shortly."));
                        return 0;
                    }
                    entry=data.find(entry.wolf()).orElseThrow();
                    archive=data.archive(entry.wolf()).orElseThrow();
                }
                if(archive.phase().equals(WolfArchive.STORED)){
                    if(data.activeCount(entry.owner())>=Config.WOLF_CAP.get())
                        throw new IllegalStateException("Your active pack is full. This companion remains safely stored");
                    if(loaded(server,entry)!=null||liveNativeUuid(server,entry.wolf()))
                        throw new IllegalStateException("Original companion or destination UUID still exists; claim held");
                    var restored=archive.entity();
                    restored.store("UUID",net.minecraft.core.UUIDUtil.CODEC,entry.wolf());
                    var persistent=restored.getCompoundOrEmpty("NeoForgeData").copy();
                    WolfIdentity.set(persistent,entry.wolf());
                    if(targetRun>0){
                        persistent.putLong(BogatyrWolfEvents.RUN,targetRun);
                        persistent.putString(BogatyrWolfEvents.OWNER,entry.owner().toString());
                    }
                    restored.put("NeoForgeData",persistent);
                    var candidate=EntityType.loadEntityRecursive(restored,player.level(),EntitySpawnReason.LOAD,e->e);
                    if(!(candidate instanceof Wolf wolf)||!entry.owner().equals(BogatyrCompanions.owner(wolf)))
                        throw new IllegalStateException("Archived wolf identity could not be loaded");
                    var pos=player.blockPosition();
                    wolf.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,player.getYRot(),0);
                    if(!player.level().noCollision(wolf)||!player.level().getFluidState(pos).isEmpty()
                            ||!player.level().getBlockState(pos.below()).isSolid())
                        throw new IllegalStateException("Stand on a clear solid floor before recovering a companion");
                    var pending=archive.releasing(player.level().dimension().location().toString(),pos.asLong(),targetRun);
                    data.putArchive(entry.wolf(),pending);
                    if(!data.flushVerified(server)){data.putArchive(entry.wolf(),archive);throw new IllegalStateException("Delivery preparation was not saved");}
                    archive=pending;
                    // Source location is no longer meaningful in another world.
                    wolf.removeLeash();wolf.setDeltaMovement(Vec3.ZERO);wolf.fallDistance=0;
                    pin(wolf,archive);
                    if(!player.level().addFreshEntity(wolf)){
                        // A synchronous false return with no accepted entity is a known rejection.
                        // Exceptions or any observed UUID keep the prepared destination for review.
                        if(!wolf.isAddedToLevel()&&!liveNativeUuid(server,entry.wolf())
                                &&data.find(entry.wolf()).filter(entry::equals).isPresent()){
                            var rejected=archive.rejectedDelivery();data.putArchive(entry.wolf(),rejected);
                            if(data.flushVerified(server))
                                throw new IllegalStateException("Companion spawn was canceled; pet remains stored for another attempt");
                            data.putArchive(entry.wolf(),archive);
                        }
                        throw new IllegalStateException("Companion spawn was rejected; saved delivery remains pending");
                    }
                }
                var destination=level(server,archive.targetDimension());
                if(destination==null||!destinationExists(server,archive,entry.owner()))
                    throw new IllegalStateException("Saved recovery destination needs developer review");
                Wolf wolf=loaded(server,data.find(entry.wolf()).orElseThrow());
                if(wolf==null){
                    if(!ready(server,entry.wolf(),destination,entry.located()&&entry.dimension().equals(archive.targetDimension())
                            ?entry.position():archive.targetPosition())){
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Loading the saved companion location. Repeat /d1 wolves "+(targetRun>0?"call":"recover")+" shortly."));
                        return 0;
                    }
                    throw new IllegalStateException("Prepared delivery has no verified live wolf. The archive is retained for developer review.");
                }
                if(wolf.level()!=destination||!ownsDelivery(wolf,entry.owner(),archive))
                    throw new IllegalStateException("Conflicting companion or delivery identity; recovery held");
                pin(wolf,archive);
                var proof=image(wolf);
                if(!saved(wolf,proof))throw new IllegalStateException("Destination entity save was not verified; retry recovery");
                var done=archive.done();data.putArchive(entry.wolf(),done);
                if(!data.flushVerified(server)){data.putArchive(entry.wolf(),archive);throw new IllegalStateException("Delivery receipt was not verified; retry recovery");}
                onObserved(wolf);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Your companion has returned, seated beside its saved recovery location."));
                return 1;
            }catch(Exception error){
                com.mojang.logging.LogUtils.getLogger().warn("Companion recovery held for "+entry.wolf(),error);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(Objects.toString(error.getMessage(),"Companion recovery is paused.")));
                return 0;
            }
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("No stored or main-world companion can return here. Use /d1 wolves to inspect your roster."));
        return 0;
    }
}

// TODO(M44, recovery review tools): Wolf Internal 10-3IgopUqHKyPHuZDKlpa64JMYXmq-_8GgKtQFhLX3c
// (2026-04-25) requires permanent ownership. Unknown legacy locations and RELEASING images with no
// verified live UUID currently stay held. Add developer preview/apply recovery only after proving
// absence across the recorded source/destination entity files and checking exact owner/transaction.
// Keep the original archive and review receipt; never offer blind forget, duplicate spawn or bulk adoption.
