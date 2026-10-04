package net.goui.cosmicdungeon.leaderboard;

import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.network.LeaderboardPayloads.*;
import net.goui.cosmicdungeon.dungeon.d1.D1LifetimeData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** One bounded read-only archive job at a time. No per-tick work or client-provided scores. */
@EventBusSubscriber(modid="cosmicdungeon")
public final class LeaderboardService{
    private static volatile State current;
    private record Key(String metric,long after,String id){}
    private record Result(List<Row> rows,long rank,boolean more,String status){}
    private record Cached(long at,Result result){}
    private static final class State{
        final MinecraftServer server;
        final List<Metric> catalog=LeaderboardMetrics.catalog();
        final Set<String> keys=new HashSet<>();
        final Map<UUID,Long> lastRequest=new HashMap<>();
        final LinkedHashMap<Key,Cached> cache=new LinkedHashMap<>();
        final ExecutorService reader=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Cosmic leaderboard archive");t.setDaemon(true);return t;});
        boolean busy;
        State(MinecraftServer server){this.server=server;catalog.forEach(m->keys.add(m.key()));}
    }
    private LeaderboardService(){}
    @SubscribeEvent public static void started(ServerStartedEvent event){current=new State(event.getServer());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){
        var state=current;current=null;if(state!=null)state.reader.shutdownNow();
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        var state=current;if(state!=null)state.lastRequest.remove(event.getEntity().getUUID());
    }
    public static void request(ServerPlayer player,Request request){
        var state=current;
        if(state==null||player.level().getServer()!=state.server)return;
        long now=System.nanoTime();Long previous=state.lastRequest.get(player.getUUID());
        if(previous!=null&&now-previous<250_000_000L)return;
        state.lastRequest.put(player.getUUID(),now);
        String metric=request.metric().isEmpty()?LeaderboardMetrics.DEFAULT:request.metric();
        if(!state.keys.contains(metric)){send(state,player,request,metric,new Result(List.of(),1,false,"Unknown statistic"));return;}
        var key=new Key(metric,request.afterValue(),request.afterId());
        var cached=state.cache.get(key);
        if(cached!=null&&now-cached.at()<30_000_000_000L){send(state,player,request,metric,cached.result());return;}
        if(state.busy){send(state,player,request,metric,new Result(List.of(),1,false,"Another archive read is finishing. Refresh shortly."));return;}
        // Main-thread snapshots isolate the worker from live player/SavedData mutation.
        var data=D1LifetimeData.get(state.server);
        Map<UUID,Row> stored=new HashMap<>(),online=new HashMap<>();
        boolean custom=metric.startsWith("cosmic|")||metric.startsWith("legacy|");
        for(UUID id:data.owners()){
            String name=data.activity(id).name();if(name.isEmpty())name=id.toString().substring(0,8);
            stored.put(id,new Row(id.toString(),name,custom?Math.max(0,data.leaderboard(id,metric)):0));
        }
        for(var p:state.server.getPlayerList().getPlayers())
            online.put(p.getUUID(),new Row(p.getStringUUID(),p.getGameProfile().name(),
                custom?Math.max(0,data.leaderboard(p.getUUID(),metric)):LeaderboardMetrics.nativeValue(p.getStats(),metric)));
        var folder=state.server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        var fixer=state.server.getFixerUpper();state.busy=true;
        state.reader.execute(()->{
            Result result;
            try{
                var ranking=new LeaderboardRanking(key.after(),key.id(),LeaderboardPayloads.PAGE_SIZE);
                online.values().forEach(ranking::accept);
                var remaining=new HashMap<>(stored);online.keySet().forEach(remaining::remove);
                int files=0,errors=0;long bytes=0,deadline=System.nanoTime()+10_000_000_000L;
                if(Files.isDirectory(folder))try(var paths=Files.newDirectoryStream(folder,"*.json")){
                    for(var path:paths){
                        if(Thread.currentThread().isInterrupted()||current!=state)return;
                        if(++files>10000||System.nanoTime()>deadline)throw new IllegalStateException("Archive query limit reached");
                        String file=path.getFileName().toString();UUID id;
                        try{id=UUID.fromString(file.substring(0,file.length()-5));}catch(IllegalArgumentException ignored){continue;}
                        if(!id.toString().equals(file.substring(0,file.length()-5))||online.containsKey(id))continue;
                        try{
                            if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("Non-regular statistics file");
                            long size=Files.size(path);bytes+=size;
                            if(bytes>64L*1024*1024)throw new IllegalStateException("Archive byte limit reached");
                            var known=remaining.remove(id);
                            String name=known==null?id.toString().substring(0,8):known.name();
                            long value=custom?(known==null?0:known.value()):StatisticsArchive.read(path,metric,fixer);
                            ranking.accept(new Row(id.toString(),name,value));
                        }catch(Exception failed){errors++;if(bytes>64L*1024*1024)break;}
                    }
                }
                remaining.values().forEach(ranking::accept);
                result=errors==0?new Result(ranking.rows(),ranking.firstRank(),ranking.more(),"Server-wide recorded activity; cached up to 30 seconds")
                    :new Result(List.of(),1,false,"Archive incomplete ("+errors+" unreadable files). No ranking shown.");
            }catch(Exception failed){
                result=new Result(List.of(),1,false,"Archive could not be read completely. Check server statistics files.");
            }
            final Result completed=result;
            if(current!=state)return;
            state.server.execute(()->{
                if(current!=state)return;state.busy=false;
                if(completed.status().startsWith("Server-wide")){
                    state.cache.put(key,new Cached(System.nanoTime(),completed));
                    while(state.cache.size()>32)state.cache.remove(state.cache.keySet().iterator().next());
                }
                if(state.server.getPlayerList().getPlayer(player.getUUID())==player)send(state,player,request,metric,completed);
            });
        });
    }
    private static void send(State state,ServerPlayer player,Request request,String metric,Result result){
        String filter=request.search().trim().toLowerCase(Locale.ROOT);
        var choices=state.catalog.stream().filter(m->m.label().toLowerCase(Locale.ROOT).contains(filter)
            ||m.key().toLowerCase(Locale.ROOT).contains(filter)).toList();
        int pages=Math.max(1,(choices.size()+LeaderboardPayloads.PAGE_SIZE-1)/LeaderboardPayloads.PAGE_SIZE);
        int page=Math.min(request.page(),pages-1),start=page*LeaderboardPayloads.PAGE_SIZE;
        var rows=choices.subList(Math.min(start,choices.size()),Math.min(start+LeaderboardPayloads.PAGE_SIZE,choices.size()));
        var named=result.rows().stream().map(row->{
            var identity=state.server.services().nameToIdCache().get(UUID.fromString(row.id()));
            String name=identity.map(net.minecraft.server.players.NameAndId::name).orElse(row.name());
            return new Row(row.id(),name.substring(0,Math.min(16,name.length())),row.value());
        }).toList();
        ModNetwork.sendTo(player,new View(request.request(),rows,named,new Page(metric,page,pages,result.rank(),result.more(),result.status())));
    }
}
