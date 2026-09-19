package net.goui.cosmicdungeon.economy;

import net.goui.cosmicdungeon.CosmicDungeonMod;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.item.ModItems;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Server-only coordination. Native item entities retain ordinary physics, damage, portals and expiry.
 * Their exact native image is stored with the account; projections never enter chunk entity saves.
 * An uncertain write holds currency work until verified, rather than refunding or issuing another drop. */
@EventBusSubscriber(modid=CosmicDungeonMod.MOD_ID)
public final class DeathCurrencyService {
    private static final String LIFE="death_currency_life_v1";
    private static final Map<MinecraftServer,State> STATES=new WeakHashMap<>();
    private static final class State {
        final ArrayDeque<UUID> queue=new ArrayDeque<>();
        final Set<UUID> queued=new HashSet<>(),lifeHeld=new HashSet<>();
        final Map<UUID,ItemEntity> live=new HashMap<>();
        final Map<UUID,Integer> snapTick=new HashMap<>();
        boolean paused,fatal;int retryAt;
        void enqueue(UUID id){if(queued.add(id))queue.addLast(id);}
    }
    private DeathCurrencyService(){}
    private static State state(MinecraftServer server){
        return STATES.computeIfAbsent(server,s->{var state=new State();PlayerCurrencyData.get(s).deathIds().forEach(state::enqueue);return state;});
    }
    public static boolean suspended(MinecraftServer server){var s=STATES.get(server);return s!=null&&(s.paused||s.fatal);}
    public static boolean blocked(ServerPlayer p){
        var s=STATES.get(p.level().getServer());
        return (s!=null&&(s.paused||s.fatal||s.lifeHeld.contains(p.getUUID())))
                ||PlayerCurrencyData.get(p.level().getServer()).pendingDeath(p.getUUID());
    }
    public static boolean marked(ItemStack stack){
        var custom=stack.get(DataComponents.CUSTOM_DATA);
        return custom!=null&&custom.contains(DeathCurrencyRecord.MARKER);
    }
    public static boolean logical(Entity entity){
        return entity instanceof ItemEntity item&&(entity.getPersistentData().contains(DeathCurrencyRecord.MARKER)||marked(item.getItem()));
    }
    private static boolean identity(ItemEntity item){
        return item.getUUID().toString().equals(item.getPersistentData().getStringOr(DeathCurrencyRecord.MARKER,""))&&marked(item.getItem());
    }
    private static boolean verify(MinecraftServer server){
        var s=state(server);
        if(s.fatal)return false;
        boolean ok=PlayerCurrencyData.get(server).flushVerified();
        s.paused=!ok;s.retryAt=server.getTickCount()+D1EconomyConfig.DEATH_RETRY_TICKS.get();return ok;
    }
    private static void fault(MinecraftServer server,Exception failure){
        var s=state(server);s.fatal=true;
        com.mojang.logging.LogUtils.getLogger().error("Death currency held for recovery; account/drop evidence retained",failure);
    }
    private static UUID life(ServerPlayer p){
        String value=p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getStringOr(LIFE,"");
        return value.isEmpty()?null:UUID.fromString(value);
    }
    /** Saving a new life token precedes play. Existing class clone copies this root on respawn. */
    private static void prepareLife(ServerPlayer p,boolean respawn){
        var server=p.level().getServer();var s=state(server);var data=PlayerCurrencyData.get(server);
        if(data.pendingDeath(p.getUUID())){s.lifeHeld.add(p.getUUID());return;}
        try{
            UUID current=life(p);
            if(current==null||(p.isAlive()&&(respawn||data.deathSeen(p.getUUID(),current)))){
                var root=p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
                root.putString(LIFE,UUID.randomUUID().toString());p.getPersistentData().put(ClassData.ROOT_TAG,root);
                s.lifeHeld.add(p.getUUID());
            }
            if(s.lifeHeld.contains(p.getUUID())&&PlayerSaveProof.save(p))s.lifeHeld.remove(p.getUUID());
        }catch(Exception error){fault(server,error);}
    }
    /** Injected at the final return of ServerPlayer.die, after NeoForge's cancellation return and loot. */
    public static void died(ServerPlayer p){
        var server=p.level().getServer();
        try{
            var s=state(server);var data=PlayerCurrencyData.get(server);
            UUID id=life(p);
            if(id!=null&&data.deathSeen(p.getUUID(),id))return;
            if(id==null){
                var root=p.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);id=UUID.randomUUID();
                root.putString(LIFE,id.toString());p.getPersistentData().put(ClassData.ROOT_TAG,root);
                s.lifeHeld.add(p.getUUID());
            }
            long before=data.getBalanceTrace(p.getUUID());
            long amount=DeathCurrencyRecord.loss(before,D1EconomyConfig.DEATH_THRESHOLD.get(),D1EconomyConfig.DEATH_PERCENT.get(),D1EconomyConfig.DEATH_MIN.get());
            long run=DungeonRunRegistryData.get(server).findRunForPlayer(p.getUUID()).map(r->r.runId()).orElse(0L);
            var item=new ItemEntity(p.level(),p.getX(),p.getY(),p.getZ(),visual(id,amount));
            item.setUUID(id);item.getPersistentData().putString(DeathCurrencyRecord.MARKER,id.toString());item.setDefaultPickUpDelay();
            var record=new DeathCurrencyRecord(id,p.getUUID(),before,amount,run,System.currentTimeMillis(),
                    p.level().dimension().location().toString(),snapshot(item),false);
            // Persist intent first. Startup can finish it without needing the owner online.
            if(data.prepareDeath(record))s.enqueue(id);
            if(!verify(server))return;
            // Missing legacy life tokens must be durably tied to this death before another life.
            if(s.lifeHeld.contains(p.getUUID())&&!PlayerSaveProof.save(p))return;
            s.lifeHeld.remove(p.getUUID());
            reconcile(server,id);
        }catch(Exception error){fault(server,error);}
    }
    private static ItemStack visual(UUID id,long amount){
        var stack=new ItemStack(ModItems.ATTUNEMENT_TRACE.get());var tag=new CompoundTag();
        tag.putString(DeathCurrencyRecord.MARKER,id.toString());tag.putBoolean("no_trade",true);tag.putBoolean("no_drop",true);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));stack.set(DataComponents.CUSTOM_NAME,Component.literal(amount+" Trace"));return stack;
    }
    private static CompoundTag snapshot(ItemEntity item){
        var errors=new ProblemReporter.Collector();var output=TagValueOutput.createWithContext(errors,item.registryAccess());
        item.saveWithoutId(output);if(!errors.isEmpty())throw new IllegalStateException(errors.getReport());
        var tag=output.buildResult();tag.putString("death_projection_id",item.getUUID().toString());return tag;
    }
    private static void track(ItemEntity item){
        PlayerCurrencyData.get(item.level().getServer()).snapshotDeath(item.getUUID(),item.level().dimension().location().toString(),snapshot(item));
    }
    private static void reconcile(MinecraftServer server,UUID id){
        var s=state(server);if(s.paused||s.fatal)return;
        var data=PlayerCurrencyData.get(server);var record=data.deathDrop(id).orElse(null);
        if(record==null)return;
        if(!record.active()){
            if(!data.commitDeath(id)||!verify(server))return;
            record=data.deathDrop(id).orElse(null);if(record==null)return;
        }
        var live=s.live.get(id);
        if(live!=null&&!live.isRemoved()){
            if(server.getTickCount()-s.snapTick.getOrDefault(id,Integer.MIN_VALUE/2)>=D1EconomyConfig.DEATH_SNAPSHOT_TICKS.get()){
                track(live);s.snapTick.put(id,server.getTickCount());
            }
            return;
        }
        s.live.remove(id);s.snapTick.remove(id);
        var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(record.dimension())));
        if(level==null)return; // Preserve value in an unavailable dimension; never relocate or force-load it.
        var pos=record.image().getListOrEmpty("Pos");
        if(!level.hasChunkAt(BlockPos.containing(pos.getDouble(0).orElseThrow(),pos.getDouble(1).orElseThrow(),pos.getDouble(2).orElseThrow())))return;
        var item=new ItemEntity(EntityType.ITEM,level);var errors=new ProblemReporter.Collector();
        item.load(TagValueInput.create(errors,server.registryAccess(),record.image()));
        if(!errors.isEmpty()||!identity(item)||!id.equals(item.getUUID()))throw new IllegalStateException("Invalid recovered death projection "+id);
        s.live.put(id,item);
        if(!level.addFreshEntity(item)){s.live.remove(id,item);throw new IllegalStateException("Death projection UUID collision "+id);}
        s.snapTick.put(id,server.getTickCount());
    }
    public static void added(ItemEntity item){
        if(!logical(item)||item.level().isClientSide())return;
        var server=item.level().getServer();var s=state(server);var old=s.live.get(item.getUUID());
        var record=PlayerCurrencyData.get(server).deathDrop(item.getUUID()).orElse(null);
        if(!identity(item)||record==null||!record.active()||(old!=null&&!old.isRemoved()&&old!=item)){
            item.discard();return;
        }
        // Includes the native portal replacement entity, preserving its normal destination/motion/age.
        s.live.put(item.getUUID(),item);s.enqueue(item.getUUID());track(item);
    }
    public static void touch(ItemEntity item,Player player){
        if(!(player instanceof ServerPlayer p)||item.hasPickUpDelay()||!p.isAlive()||p.isSpectator())return;
        var server=p.level().getServer();var s=state(server);
        if(s.live.get(item.getUUID())!=item||!identity(item)||blocked(p)||!CurrencyService.transactionsAllowed(p))return;
        try{
            var data=PlayerCurrencyData.get(server);
            var record=data.deathDrop(item.getUUID()).orElse(null);
            if(record!=null&&record.active()&&!data.canDeposit(p.getUUID(),record.amount())){
                CurrencyPickupEvents.showCapacityDeniedMessage(p);return;
            }
            if(data.collectDeath(item.getUUID(),p.getUUID(),System.currentTimeMillis())&&verify(server))item.discard();
        }catch(Exception error){fault(server,error);}
    }
    /** Called before native removal. Unload/portal is a checkpoint, never a supply sink. */
    public static boolean removing(ItemEntity item,Entity.RemovalReason reason){
        if(item.level().isClientSide())return true;
        var server=item.level().getServer();var s=state(server);
        if(s.live.get(item.getUUID())!=item)return true; // Rejecting a duplicate must not destroy the real value.
        var data=PlayerCurrencyData.get(server);
        try{
            if(!reason.shouldDestroy()){
                track(item);s.live.remove(item.getUUID(),item);s.snapTick.remove(item.getUUID());return true;
            }
            if(s.paused||s.fatal)return false;
            if(data.deathDrop(item.getUUID()).isPresent()){
                data.destroyDeath(item.getUUID(),"native_"+reason.name().toLowerCase(Locale.ROOT),System.currentTimeMillis());
                if(!verify(server))return false;
            }
            s.live.remove(item.getUUID(),item);s.snapTick.remove(item.getUUID());return true;
        }catch(Exception error){fault(server,error);return false;}
    }
    /** Called at the filesystem reset boundary, covering loaded AND unloaded records in that dimension. */
    public static boolean resetDimension(ServerLevel level){
        var server=level.getServer();var s=state(server);if(s.paused||s.fatal)return false;
        var data=PlayerCurrencyData.get(server);String dimension=level.dimension().location().toString();
        try{
            var ids=data.deathIds().stream().filter(id->data.deathDrop(id).orElseThrow().dimension().equals(dimension)).toList();
            if(ids.stream().anyMatch(id->!data.deathDrop(id).orElseThrow().active()))return false;
            // Batch bounded archive writes through the account outbox; each removed value is durable before world deletion.
            for(UUID id:ids){
                data.destroyDeath(id,"dungeon_dimension_reset",System.currentTimeMillis());
                if(!verify(server))return false;
                var item=s.live.get(id);if(item!=null)item.discard();
            }
            return true;
        }catch(Exception error){fault(server,error);return false;}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void started(ServerStartedEvent event){state(event.getServer());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)prepareLife(p,false);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)prepareLife(p,true);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();var s=state(server);if(s.fatal)return;
        if(s.paused){if(server.getTickCount()<s.retryAt||!verify(server))return;}
        try{
            int budget=Math.min(s.queue.size(),D1EconomyConfig.DEATH_WORK_PER_TICK.get());
            for(int i=0;i<budget;i++){
                UUID id=s.queue.removeFirst();s.queued.remove(id);reconcile(server,id);
                if(PlayerCurrencyData.get(server).deathDrop(id).isPresent())s.enqueue(id);
                else {var item=s.live.get(id);if(item!=null)item.discard();}
                if(s.paused)break;
            }
            if(server.getTickCount()%D1EconomyConfig.DEATH_RETRY_TICKS.get()==0)
                for(UUID id:List.copyOf(s.lifeHeld)){var p=server.getPlayerList().getPlayer(id);if(p!=null)prepareLife(p,false);}
        }catch(Exception error){fault(server,error);}
    }
    @SubscribeEvent public static void stopping(ServerStoppingEvent event){
        var s=STATES.get(event.getServer());if(s==null)return;
        try{for(var item:List.copyOf(s.live.values()))if(!item.isRemoved())track(item);verify(event.getServer());}
        catch(Exception error){fault(event.getServer(),error);}
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){STATES.remove(event.getServer());}
}
