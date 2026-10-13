package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.mercenary.MercenaryWolves;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import static net.goui.cosmicdungeon.playerclass.bogatyr.WolfDiagnosticWindow.Event.*;

/** Cameron's incident instrumentation: enabled by default, bounded, event driven, no saved tags or scans. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class WolfSpawnDiagnostics {
    private static final long WINDOW_NANOS=10_000_000_000L;
    private static final int PENDING_LIMIT=256;
    private static final Map<MinecraftServer,State> STATES=new WeakHashMap<>();
    private static final boolean ENABLED=!Boolean.getBoolean("cosmicdungeon.disableWolfDiagnostics");
    private static final class State {
        final WolfDiagnosticWindow window=new WolfDiagnosticWindow();
        final Map<Wolf,Boolean> pending=new WeakHashMap<>();
        long next=System.nanoTime()+WINDOW_NANOS;
    }
    private WolfSpawnDiagnostics(){}
    private static State state(Wolf wolf){
        if(!ENABLED||!(wolf.level() instanceof ServerLevel level))return null;
        return STATES.computeIfAbsent(level.getServer(),s->new State());
    }
    private static void record(Wolf wolf,WolfDiagnosticWindow.Event event,String detail){
        var state=state(wolf);if(state==null)return;
        var level=(ServerLevel)wolf.level();
        long savedRun=MercenaryWolves.managed(wolf)?MercenaryWolves.diagnosticRun(wolf)
                :wolf.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0);
        String dimension=level.dimension().location().toString();
        state.window.record(event,"savedRun="+savedRun+" dimension="+dimension,()->{
            // The dimension lookup scans the registry: do it only for the two retained samples,
            // never for every event in a flood. Saved IDs also retain PAUSED/ended-run evidence.
            var registry=DungeonRunRegistryData.get(level.getServer());
            var run=savedRun>0?registry.getRun(savedRun).orElse(null)
                    :registry.findRunForInstanceDimension(level.dimension()).orElse(null);
            long dimensionRun=run==null?0:run.runId();
            var owner=wolf.getOwnerReference();
            return "entity="+wolf.getUUID()+" bond="+BogatyrIdentity.id(wolf)
                    +" owner="+(owner==null?"none":owner.getUUID())+" run="+dimensionRun
                    +" savedRun="+savedRun+" runState="+(run==null?"missing":run.stateEnum())
                    +" dimension="+dimension+" pos="+wolf.blockPosition().toShortString()
                    +" role="+(MercenaryWolves.managed(wolf)?"mercenary":BogatyrWolfEvents.managed(wolf)?"player":"unmanaged")
                    +" mercenary="+MercenaryWolves.diagnosticBond(wolf)+" detail="+detail;
        });
    }
    /** Called before either player or mercenary admission can cancel the event. */
    public static void joining(EntityJoinLevelEvent event,Wolf wolf){
        var state=state(wolf);if(state==null)return;
        // Only Boolean metadata is retained; failed attempts cannot grow this map without bound.
        if(state.pending.size()<PENDING_LIMIT||state.pending.containsKey(wolf))state.pending.put(wolf,event.loadedFromDisk());
        record(wolf,event.loadedFromDisk()?JOIN_DISK:JOIN_FRESH,"attempt");
    }
    public static void added(Wolf wolf){
        var state=state(wolf);if(state==null)return;
        var disk=state.pending.remove(wolf);
        // NeoForge's legacy-chunk path invokes this callback even after cancellation.
        // Require native lookup identity before reporting a confirmed insertion.
        if(wolf.isRemoved()||((ServerLevel)wolf.level()).getEntity(wolf.getUUID())!=wolf){
            record(wolf,UNCONFIRMED_CALLBACK,"native-lookup-not-confirmed");return;
        }
        record(wolf,disk==null?ADDED_UNKNOWN:disk?ADDED_DISK:ADDED_FRESH,disk==null?"origin-unavailable":"native-onAddedToLevel");
    }
    public static void egg(Wolf wolf){record(wolf,EGG,"egg-before-insertion");}
    public static void birth(Wolf wolf){record(wolf,BIRTH,"birth-before-insertion");}
    public static void summon(Wolf wolf){record(wolf,MERCENARY_SUMMON,"mercenary-before-insertion");}
    private static void rejected(Wolf wolf,WolfDiagnosticWindow.Event event,String detail){
        record(wolf,event,detail);var state=state(wolf);if(state!=null)state.pending.remove(wolf);
    }
    public static void rejectedRun(Wolf wolf){rejected(wolf,REJECT_RUN,"ended/missing/foreign-run");}
    public static void rejectedIdentity(Wolf wolf){rejected(wolf,REJECT_IDENTITY,"copied/invalid/held-bond");}
    public static void rejectedMercenary(Wolf wolf){rejected(wolf,REJECT_MERCENARY,"lifecycle/owner/cap/dismissal");}
    public static void removing(Wolf wolf,Entity.RemovalReason reason){
        record(wolf,REMOVED,reason.name()+" wasAdded="+wolf.isAddedToLevel());
        var state=state(wolf);if(state!=null)state.pending.remove(wolf);
    }
    private static void flush(State state,String boundary){
        if(!state.window.empty())com.mojang.logging.LogUtils.getLogger().info(
                "[WolfSpawn] boundary={} windowSeconds=10 {} (counts are events, not unique wolves; samples capped at2 per event)",
                boundary,state.window.summary());
        state.window.clear();state.next=System.nanoTime()+WINDOW_NANOS;
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var state=STATES.get(event.getServer());
        if(state!=null&&System.nanoTime()>=state.next)flush(state,"periodic");
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){
        var state=STATES.remove(event.getServer());if(state!=null)flush(state,"server-stop");
    }
    static long count(MinecraftServer server,WolfDiagnosticWindow.Event event){
        var state=STATES.get(server);return state==null?0:state.window.count(event);
    }
}
