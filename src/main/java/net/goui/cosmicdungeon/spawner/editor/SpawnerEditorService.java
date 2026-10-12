package net.goui.cosmicdungeon.spawner.editor;

import java.util.*;
import net.goui.cosmicdungeon.auth.Authority;
import net.goui.cosmicdungeon.block.entity.*;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.goui.cosmicdungeon.command.*;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads.*;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.Origin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** One bounded server draft per Developer. Library actions never apply draft data to the world. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class SpawnerEditorService {
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Integer> HOVER_TICKS=new WeakHashMap<>();
    private static final class Session {
        UUID id=UUID.randomUUID();CosmicSpawnerBlockEntity be;SpawnerEditorDraft draft;CompoundTag baseline,working;
        int expires,lastLibrary=Integer.MIN_VALUE;List<ItemStack> equipment;ItemStack held;SpawnerLootDraft.Snapshot loot;
        List<String> names=List.of();boolean loaded;Set<UUID> operations=new HashSet<>();LibraryView lastReply;
    }
    private SpawnerEditorService(){}
    public static CosmicSpawnerBlockEntity target(ServerPlayer player){
        if(!Authority.isDeveloper(player)||!player.isAlive()||player.isSpectator())return null;
        var hit=player.pick(5.0,1,false);
        if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK)return null;
        var pos=block.getBlockPos();var level=player.level();
        if(!level.hasChunkAt(pos)||player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))>36)return null;
        return level.getBlockEntity(pos) instanceof CosmicSpawnerBlockEntity be?be:null;
    }
    private static Session create(ServerPlayer p,CosmicSpawnerBlockEntity be,CompoundTag data,CompoundTag baseline,boolean loaded){
        SpawnerPresetFileStore.validate(data);
        var detached=new CosmicSpawnerBlockEntity(be.getBlockPos(),be.getBlockState());
        detached.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),data));
        var s=new Session();s.be=be;s.draft=SpawnerEditorConfig.read(detached,data);s.baseline=baseline;s.working=data.copy();s.loaded=loaded;
        s.expires=p.tickCount+12000;s.equipment=SpawnerEquipmentConfig.snapshot(data,p.registryAccess());s.held=p.getMainHandItem().copy();
        if(!s.held.isEmpty()&&ItemStack.CODEC.encodeStart(p.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),s.held).getOrThrow().toString().length()>32768)
            throw new IllegalArgumentException("Held snapshot exceeds editor limit.");
        s.loot=SpawnerLootConfig.snapshot(data,s.draft.entityId(),p.level().getServer());
        return s;
    }
    public static int open(ServerPlayer player){
        var be=target(player);
        if(be==null){player.displayClientMessage(Component.literal("Look at a Cosmic Spawner within5 blocks as a Developer."),false);return 0;}
        try{
            var saved=be.saveWithoutMetadata(player.registryAccess());var s=create(player,be,saved,SpawnerEditorConfig.comparable(saved),false);
            String note="";try{s.names=SpawnerPresetFileStore.listPresetNames(player.level().getServer());}catch(Exception ex){note="Preset library: "+message(ex);}
            SESSIONS.put(player,s);send(player,s,Status.OPEN,note);return 1;
        }catch(Exception ex){player.displayClientMessage(Component.literal("Editor read-only: "+message(ex)),false);return 0;}
    }
    private static void entity(ServerPlayer player,String id){
        var location=ResourceLocation.tryParse(id);var type=location==null?null:BuiltInRegistries.ENTITY_TYPE.getOptional(location).orElse(null);
        if(type==null||!type.canSummon()||!(type.create(player.level(),EntitySpawnReason.COMMAND) instanceof Mob))
            throw new IllegalArgumentException("Choose a registered summonable mob.");
    }
    private static CompoundTag current(ServerPlayer player,Session s){
        if(!valid(player,s)||target(player)!=s.be)throw new IllegalStateException("Target or Developer access changed. Reopen /spawner edit.");
        var current=s.be.saveWithoutMetadata(player.registryAccess());
        if(!s.baseline.equals(SpawnerEditorConfig.comparable(current)))throw new IllegalStateException("Spawner changed during editing. Reopen to review its current configuration.");
        return current;
    }
    private static CompoundTag build(ServerPlayer player,Session s,SpawnerEditorDraft draft,SpawnerEquipmentDraft equipment,SpawnerLootDraft loot){
        var current=current(player,s);
        if(!draft.error().isEmpty())throw new IllegalArgumentException(draft.error());entity(player,draft.entityId());
        var resolved=new ArrayList<ItemStack>();
        for(var slot:Slot.values()){
            var edit=equipment.get(slot);
            if(edit.origin()==Origin.HELD&&!ItemStack.matches(s.held,player.getMainHandItem()))
                throw new IllegalArgumentException("Held item changed. Reopen to copy the current item.");
            var stack=SpawnerEquipmentRules.resolve(s.equipment.get(slot.ordinal()),s.held,edit,player.registryAccess());
            if(edit.changed()&&!edit.advanced()&&!SpawnerEquipmentRules.fits(slot,stack))throw new IllegalArgumentException("Item does not fit "+slot.id+" in Normal mode.");
            resolved.add(stack);
        }
        var updated=SpawnerEditorConfig.patch(s.loaded?s.working:current,s.draft,draft);
        updated=SpawnerEquipmentConfig.patch(updated,draft.entityId(),equipment,resolved,player.registryAccess());
        updated=SpawnerLootConfig.patch(updated,draft.entityId(),loot);SpawnerPresetFileStore.validate(updated);
        return updated;
    }
    public static void applyConfiguration(ServerPlayer player,CosmicSpawnerBlockEntity be,CompoundTag updated){
        if(target(player)!=be)throw new IllegalArgumentException("Look at the same spawner with Developer access.");
        SpawnerPresetFileStore.validate(updated);entity(player,updated.getStringOr("SpawnerEntityId",""));
        be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,player.registryAccess(),updated));
        be.setChanged();player.level().sendBlockUpdated(be.getBlockPos(),be.getBlockState(),be.getBlockState(),3);
    }
    public static void request(ServerPlayer player,Request request){
        var s=SESSIONS.get(player);
        if(s==null||!s.id.equals(request.session())){
            ModNetwork.sendTo(player,new View(request.session(),BlockPos.ZERO,SpawnerEditorDraft.empty(),Status.CLOSED,"Session expired. Reopen /spawner edit."));return;
        }
        if(request.kind()==Kind.CANCEL){SESSIONS.remove(player);send(player,s,Status.CLOSED,"Changes discarded.");return;}
        try{
            var updated=build(player,s,request.draft(),request.equipment(),request.loot());
            var current=s.be.saveWithoutMetadata(player.registryAccess());
            if(!updated.equals(current))applyConfiguration(player,s.be,updated);
            SESSIONS.remove(player);send(player,s,Status.APPLIED,"Spawner changes applied.");
        }catch(IllegalStateException ex){close(player,s,message(ex));}
        catch(IllegalArgumentException ex){send(player,s,Status.INVALID,message(ex));}
    }
    public static void library(ServerPlayer player,LibraryRequest request){
        var s=SESSIONS.get(player);
        if(s==null||!s.id.equals(request.session())){
            ModNetwork.sendTo(player,new LibraryView(request.session(),request.operation(),false,"Session expired. Reopen /spawner edit.",List.of(),Collections.nCopies(5,""),SpawnerLootDraft.Snapshot.empty()));return;
        }
        try{
            current(player,s);
            if(s.operations.contains(request.operation())){
                if(s.lastReply!=null&&s.lastReply.operation().equals(request.operation()))ModNetwork.sendTo(player,s.lastReply);
                else reply(player,s,request,false,"This action was already processed.",s.loot);return;
            }
            if(s.operations.size()>=256)throw new IllegalStateException("Library action limit reached. Reopen the editor.");
            if((long)player.tickCount-s.lastLibrary<5){reply(player,s,request,false,"Please wait briefly before another library action.",s.loot);return;}
            s.lastLibrary=player.tickCount;s.operations.add(request.operation());
            var server=player.level().getServer();
            switch(request.kind()){
                case INSPECT -> {
                    entity(player,request.draft().entityId());var snapshot=SpawnerLootConfig.snapshot(s.working,request.draft().entityId(),server);
                    reply(player,s,request,true,"Default table refreshed for "+snapshot.entityId()+".",snapshot);return;
                }
                case LOAD -> {
                    var loaded=SpawnerPresetFileStore.readPreset(server,request.name());
                    var updated=loaded.applyTo(s.be.saveWithoutMetadata(player.registryAccess()));entity(player,updated.getStringOr("SpawnerEntityId",""));
                    var next=create(player,s.be,updated,s.baseline,true);next.names=SpawnerPresetFileStore.listPresetNames(server);
                    SESSIONS.put(player,next);send(player,next,Status.OPEN,"Preset loaded into draft. Apply commits; Cancel discards.");return;
                }
                case SAVE,OVERWRITE -> {
                    var names=SpawnerPresetFileStore.listPresetNames(server);
                    if(names.size()>=256&&!names.contains(request.name()))throw new IllegalArgumentException("Library limit256 reached; delete a file before creating another.");
                    SpawnerPresetFileStore.writePreset(server,request.name(),build(player,s,request.draft(),request.equipment(),request.loot()),request.kind()==LibraryKind.OVERWRITE);
                }
                case DELETE -> {if(!SpawnerPresetFileStore.deletePreset(server,request.name()))throw new IllegalArgumentException("Preset not found.");}
                case BIND -> SpawnerCommand.assignPresetShortcut(player,request.shortcut(),request.name());
                case LIST -> {}
            }
            s.names=SpawnerPresetFileStore.listPresetNames(server);
            reply(player,s,request,true,switch(request.kind()){
                case SAVE,OVERWRITE -> "Draft saved to preset file; live spawner unchanged.";
                case DELETE -> "Preset file deleted; live spawner unchanged.";
                case BIND -> "Shortcut updated; live spawner unchanged.";
                default -> "Preset library refreshed.";
            },s.loot);
        }catch(IllegalStateException ex){close(player,s,message(ex));}
        catch(Exception ex){reply(player,s,request,false,message(ex),s.loot);}
    }
    private static void reply(ServerPlayer p,Session s,LibraryRequest q,boolean success,String text,SpawnerLootDraft.Snapshot loot){
        s.lastReply=new LibraryView(s.id,q.operation(),success,text.length()>256?text.substring(0,256):text,s.names,shortcuts(p),loot);ModNetwork.sendTo(p,s.lastReply);
    }
    private static String message(Exception ex){String text=ex.getMessage();return text==null?"Invalid editor data.":text.substring(0,Math.min(text.length(),220));}
    private static List<String> shortcuts(ServerPlayer p){return SpawnerCommand.presetShortcuts(p).stream().map(n->SpawnerPresetFileStore.validName(n)?n:"").toList();}
    private static boolean valid(ServerPlayer p,Session s){
        return Authority.isDeveloper(p)&&p.isAlive()&&!p.isSpectator()&&p.tickCount<s.expires&&s.be.getLevel()==p.level()&&p.level().hasChunkAt(s.be.getBlockPos())
                &&p.level().getBlockEntity(s.be.getBlockPos())==s.be&&p.getEyePosition().distanceToSqr(Vec3.atCenterOf(s.be.getBlockPos()))<=36;
    }
    private static void send(ServerPlayer p,Session s,Status status,String message){
        if(status==Status.CLOSED){ModNetwork.sendTo(p,new View(s.id,s.be.getBlockPos(),SpawnerEditorDraft.empty(),status,message));return;}
        ModNetwork.sendTo(p,new View(s.id,s.be.getBlockPos(),s.draft,status,message.length()>256?message.substring(0,256):message,s.equipment,s.held,s.loot,s.names,shortcuts(p),s.loaded));
    }
    private static void close(ServerPlayer p,Session s,String message){SESSIONS.remove(p);send(p,s,Status.CLOSED,message);}
    public static void hover(ServerPlayer player,BlockPos pos){
        if(!Authority.isDeveloper(player))return;
        int last=HOVER_TICKS.getOrDefault(player,Integer.MIN_VALUE);
        if((long)player.tickCount-last<10)return;
        HOVER_TICKS.put(player,player.tickCount);
        var be=target(player);if(be==null||!be.getBlockPos().equals(pos))return;
        var all=be.saveWithoutMetadata(player.registryAccess());var safe=new CompoundTag();
        for(String key:List.of("CosmicSpawnerDataVersion","SpawnerEntityId","BossOneShot","BossHasSpawned","SpawnerMobCap","MinSpawnDelay",
                "MaxSpawnDelay","SpawnCount","RequiredPlayerRange","MaxNearbyEntities","SpawnerPreset"))
            if(all.contains(key))safe.put(key,all.get(key).copy());
        // Only the inspected Developer receives details. Public block updates contain visual identity only.
        safe.putBoolean("CosmicSpawnBlocked",be.isSpawnBlocked());
        if(safe.toString().length()<=65536)ModNetwork.sendTo(player,new HoverView(pos,safe));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(event.getEntity() instanceof ServerPlayer p&&p.tickCount%20==0){
            var s=SESSIONS.get(p);if(s!=null&&!valid(p,s))close(p,s,"Session ended. Reopen /spawner edit.");
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer p){SESSIONS.remove(p);HOVER_TICKS.remove(p);}
    }
}
