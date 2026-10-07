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

/** Native entity utilities and compatible retired-command responses; no cross-run wolf delivery. */
public final class BogatyrRecovery {
    private record Loading(String dimension,long position,long started,CompletableFuture<?> future){}
    private static final Map<UUID,Loading> LOADS=new LinkedHashMap<>();
    private static final Map<UUID,Long> LAST_ACTION=new HashMap<>();
    private static long budgetTick=Long.MIN_VALUE;
    private static int snapshots;
    private BogatyrRecovery(){}
    public static void clear(){LOADS.clear();LAST_ACTION.clear();BogatyrIdentity.clear();BogatyrReview.clear();BogatyrRunLifecycle.clear();budgetTick=Long.MIN_VALUE;snapshots=0;}
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
    public static boolean ready(MinecraftServer server,UUID id,ServerLevel level,long position){
        budget(server);
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
        return BogatyrRunLifecycle.retire(server,run);
    }
    static void pin(Wolf wolf,WolfArchive archive){
        wolf.getPersistentData().putBoolean(WolfArchive.MARKER+"_noai",archive.originalNoAi());
        wolf.getPersistentData().putBoolean(WolfArchive.MARKER+"_invulnerable",archive.originalInvulnerable());
        wolf.getPersistentData().putString(WolfArchive.MARKER,archive.transaction().toString());
        wolf.setNoAi(true);wolf.setInvulnerable(true);wolf.setOrderedToSit(true);
        wolf.setTarget(null);wolf.getNavigation().stop();wolf.setDeltaMovement(Vec3.ZERO);
    }
    public static void onObserved(Wolf wolf){
        if(!(wolf.level() instanceof ServerLevel level)||!wolf.isAddedToLevel()
                ||!BogatyrWolfEvents.managed(wolf)||!BogatyrRunLifecycle.admit(wolf)||!BogatyrIdentity.observe(wolf))return;
        // The actual active native entity is authoritative, including an interrupted old delivery.
        var data=BogatyrCompanionData.get(level.getServer());
        if(wolf.getPersistentData().contains(WolfArchive.MARKER+"_noai")){
            wolf.setNoAi(wolf.getPersistentData().getBooleanOr(WolfArchive.MARKER+"_noai",false));
            wolf.setInvulnerable(wolf.getPersistentData().getBooleanOr(WolfArchive.MARKER+"_invulnerable",false));
            clearMarker(wolf);
        }
        data.retireArchive(BogatyrIdentity.id(wolf));
    }
    private static void clearMarker(Wolf wolf){
        wolf.getPersistentData().remove(WolfArchive.MARKER);
        wolf.getPersistentData().remove(WolfArchive.MARKER+"_noai");
        wolf.getPersistentData().remove(WolfArchive.MARKER+"_invulnerable");
    }
    public static boolean held(Wolf wolf){
        return BogatyrIdentity.held(wolf)||BogatyrCommands.held(wolf);
    }
    public static boolean sourceCleared(MinecraftServer server,long run){
        return BogatyrRunLifecycle.retire(server,DungeonRunRegistryData.get(server).getRun(run).orElse(null));
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
    public static int call(ServerPlayer player){return call(player,null);}
    public static int call(ServerPlayer player,UUID selected){return retiredMessage(player);}
    public static int recover(ServerPlayer player){return recover(player,null);}
    public static int recover(ServerPlayer player,UUID selected){return retiredMessage(player);}
    private static int retiredMessage(ServerPlayer player){
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "Wolfpacks last for one dungeon run. Use Skills > Wolfpack > Regroup for living loaded wolves in this dungeon."));
        return 0;
    }
}
