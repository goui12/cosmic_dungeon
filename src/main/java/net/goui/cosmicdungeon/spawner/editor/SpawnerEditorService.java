package net.goui.cosmicdungeon.spawner.editor;

import java.util.*;
import net.goui.cosmicdungeon.auth.Authority;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerBlockEntity;
import net.goui.cosmicdungeon.network.ModNetwork;
import net.goui.cosmicdungeon.network.SpawnerEditorPayloads.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.goui.cosmicdungeon.block.entity.CosmicSpawnerPreset.Slot;
import net.goui.cosmicdungeon.spawner.editor.SpawnerEquipmentDraft.Origin;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Main-thread, one bounded expiring draft per Developer. Clients never provide block NBT or target identity. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class SpawnerEditorService {
    private static final Map<ServerPlayer,Session> SESSIONS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Integer> HOVER_TICKS=new WeakHashMap<>();
    private record Session(UUID id,CosmicSpawnerBlockEntity be,SpawnerEditorDraft draft,CompoundTag baseline,int expires,List<ItemStack> equipment,ItemStack held){}
    private SpawnerEditorService(){}
    public static CosmicSpawnerBlockEntity target(ServerPlayer player){
        if(!Authority.isDeveloper(player)||!player.isAlive()||player.isSpectator())return null;
        var hit=player.pick(5.0,1,false);
        if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK)return null;
        var pos=block.getBlockPos();var level=player.level();
        if(!level.hasChunkAt(pos)||player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos))>36)return null;
        return level.getBlockEntity(pos) instanceof CosmicSpawnerBlockEntity be?be:null;
    }
    public static int open(ServerPlayer player){
        var be=target(player);
        if(be==null){player.displayClientMessage(Component.literal("Look at a Cosmic Spawner within 5 blocks as a Developer."),false);return 0;}
        var saved=be.saveWithoutMetadata(player.registryAccess());
        if(saved.toString().length()>65536||saved.getIntOr("CosmicSpawnerDataVersion",0)>153||
                saved.getCompound("SpawnerPreset").map(t->t.getIntOr("presetVersion",0)>5).orElse(false)){
            player.displayClientMessage(Component.literal("This spawner has newer or oversized authored data; editor is read-only."),false);return 0;
        }
        try{
            var draft=SpawnerEditorConfig.read(be,saved);
            var session=new Session(UUID.randomUUID(),be,draft,SpawnerEditorConfig.comparable(saved),player.tickCount+12000,SpawnerEquipmentConfig.snapshot(saved,player.registryAccess()),player.getMainHandItem().copy());
            if(!session.held.isEmpty()&&ItemStack.CODEC.encodeStart(player.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),session.held).getOrThrow().toString().length()>32768)
                throw new IllegalArgumentException("Held item snapshot too large.");
            SESSIONS.put(player,session);send(player,session,Status.OPEN,"");return 1;
        }catch(IllegalArgumentException ex){
            player.displayClientMessage(Component.literal("This authored name or held-item data exceeds the editor limit; use existing commands to edit it."),false);return 0;
        }
    }
    public static void request(ServerPlayer player,Request request){
        var session=SESSIONS.get(player);
        if(session==null||!session.id.equals(request.session())){
            ModNetwork.sendTo(player,new View(request.session(),BlockPos.ZERO,SpawnerEditorDraft.empty(),Status.CLOSED,"Session expired. Reopen /spawner edit."));return;
        }
        if(request.kind()==Kind.CANCEL){SESSIONS.remove(player);send(player,session,Status.CLOSED,"Changes discarded.");return;}
        if(!valid(player,session)||target(player)!=session.be){
            close(player,session,"Target or Developer access changed. Reopen /spawner edit.");return;
        }
        String error=request.draft().error();
        if(!error.isEmpty()){send(player,session,Status.INVALID,error);return;}
        var type=BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(request.draft().entityId())).orElse(null);
        if(type==null||!type.canSummon()||!(type.create(player.level(),EntitySpawnReason.COMMAND) instanceof Mob)){
            send(player,session,Status.INVALID,"Choose a registered summonable mob.");return;
        }
        var current=session.be.saveWithoutMetadata(player.registryAccess());
        if(!session.baseline.equals(SpawnerEditorConfig.comparable(current))){
            close(player,session,"Spawner changed during editing. Reopen to review the current configuration.");return;
        }
        CompoundTag updated;
        try{
            var resolved=new ArrayList<ItemStack>();
            for(var slot:Slot.values()){
                var edit=request.equipment().get(slot);
                if(edit.origin()==Origin.HELD&&!ItemStack.matches(session.held,player.getMainHandItem()))
                    throw new IllegalArgumentException("Held item changed. Reopen the editor to copy the current item.");
                var stack=SpawnerEquipmentRules.resolve(session.equipment.get(slot.ordinal()),session.held,edit,player.registryAccess());
                if(edit.changed()&&!edit.advanced()&&!SpawnerEquipmentRules.fits(slot,stack))
                    throw new IllegalArgumentException("Item does not fit "+slot.id+" in Normal mode.");
                resolved.add(stack);
            }
            updated=SpawnerEditorConfig.patch(current,session.draft,request.draft());
            updated=SpawnerEquipmentConfig.patch(updated,request.draft().entityId(),request.equipment(),resolved,player.registryAccess());
            if(updated.toString().length()>65536)throw new IllegalArgumentException("Result exceeds the editor's bounded snapshot size.");
        }catch(IllegalArgumentException ex){send(player,session,Status.INVALID,ex.getMessage().length()>256?"Invalid equipment components.":ex.getMessage());return;}
        if(!updated.equals(current)){
            session.be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,player.registryAccess(),updated));
            session.be.setChanged();player.level().sendBlockUpdated(session.be.getBlockPos(),session.be.getBlockState(),session.be.getBlockState(),3);
        }
        SESSIONS.remove(player);send(player,session,Status.APPLIED,"Spawner changes applied.");
    }
    private static boolean valid(ServerPlayer p,Session s){
        return Authority.isDeveloper(p)&&p.isAlive()&&!p.isSpectator()&&p.tickCount<s.expires&&
                s.be.getLevel()==p.level()&&p.level().hasChunkAt(s.be.getBlockPos())&&
                p.level().getBlockEntity(s.be.getBlockPos())==s.be&&
                p.getEyePosition().distanceToSqr(Vec3.atCenterOf(s.be.getBlockPos()))<=36;
    }
    private static void send(ServerPlayer p,Session s,Status status,String message){
        ModNetwork.sendTo(p,new View(s.id,s.be.getBlockPos(),status==Status.CLOSED?SpawnerEditorDraft.empty():s.draft,status,message,
                status==Status.CLOSED?Collections.nCopies(6,ItemStack.EMPTY):s.equipment,status==Status.CLOSED?ItemStack.EMPTY:s.held));
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
