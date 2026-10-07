package net.goui.cosmicdungeon.playerclass.resource;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.network.ClassResourcePayloads;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Synchronous CI-only native tags, inventory/save transactions, clone and session lifecycle fixtures. */
public final class ClassResourceGameTests {
    private ClassResourceGameTests(){}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;final ServerLevel level;final Vec3 origin;final GameProfile profile;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs;final Map<UUID,ServerPlayer> players;
        final Map<UUID,?> stats,advancements;final List<ServerPlayer> online,owned=new ArrayList<>();
        final Set<Long> runIds=new HashSet<>();final List<ClassResourcePayloads.View> views=new ArrayList<>();
        final io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel();
        final List<Path> files;ServerPlayer player;long runId;final String dungeonId;
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper,long runId,String dungeonId,String classId){
            this.helper=helper;this.level=helper.getLevel();this.runId=runId;this.dungeonId=dungeonId;
            var server=level.getServer();profile=new GameProfile(UUID.randomUUID(),"ResourceTest");
            var anchor=helper.absoluteVec(new Vec3(1.5,10.5,1.5));var chunk=new ChunkPos(BlockPos.containing(anchor));
            origin=new Vec3(chunk.getMinBlockX()+8.5,anchor.y,chunk.getMinBlockZ()+8.5);
            try{
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)field(DungeonRunRegistryData.class,"runsById",DungeonRunRegistryData.get(server));
                players=(Map<UUID,ServerPlayer>)field(PlayerList.class,"playersByUUID",server.getPlayerList());
                online=(List<ServerPlayer>)field(PlayerList.class,"players",server.getPlayerList());
                stats=(Map<UUID,?>)field(PlayerList.class,"stats",server.getPlayerList());
                advancements=(Map<UUID,?>)field(PlayerList.class,"advancements",server.getPlayerList());
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            String owner=profile.id().toString();
            files=List.of(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(owner+".dat"),
                    server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(owner+".dat_old"),
                    server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(owner+".json"),
                    server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(owner+".json"));
            check(!players.containsKey(profile.id())&&!stats.containsKey(profile.id())&&!advancements.containsKey(profile.id())
                    &&files.stream().noneMatch(Files::exists),"Fixture owner must be new");
            player=createPlayer();setClass(classId);registerRun(runId);players.put(profile.id(),player);online.add(player);
        }
        private static Object field(Class<?> type,String name,Object target)throws ReflectiveOperationException{
            var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(target);
        }
        ServerPlayer createPlayer(){
            var server=level.getServer();var next=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
                @Override public io.netty.channel.Channel channel(){return channel;}
                @Override public boolean isConnected(){return true;}
            };
            net.neoforged.neoforge.network.registration.ChannelAttributes.setConnectionType(connection,
                    net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
            net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,
                    net.neoforged.neoforge.network.registration.NetworkPayloadSetup.empty());
            next.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,next,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
                @Override public void send(Packet<?> packet){capture(packet);}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){capture(packet);}
                @Override public void resetPosition(){}
                @Override public void teleport(double x,double y,double z,float yaw,float pitch){}
            };
            next.setGameMode(GameType.SURVIVAL);next.setHealth(20);next.setPos(origin);owned.add(next);return next;
        }
        void capture(Packet<?> packet){
            if(packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload() instanceof ClassResourcePayloads.View view)views.add(view);
        }
        void registerRun(long next){
            check(!runs.containsKey(next),"Unique resource run "+next);
            runs.put(next,new DungeonRunRegistryData.RunRecord(next,dungeonId,"minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,
                    List.of(profile.id()),List.of(),List.of()));runIds.add(next);runId=next;
        }
        void replace(ServerPlayer next){
            ClassResourceService.forget(player);online.remove(player);player=next;
            players.put(profile.id(),next);online.add(next);next.connection.player=next;next.setPos(origin);
        }
        CompoundTag root(){return player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
        void setClass(String id){var next=root().copy();next.putString(ClassData.KEY_CLASS_ID,id);next.putString("future_marker","retained");
            player.getPersistentData().put(ClassData.ROOT_TAG,next);}
        ClassResourceLedger ledger(){return ClassResourceLedger.forRun(root(),runId);}
        void ledger(ClassResourceLedger next){player.getPersistentData().put(ClassData.ROOT_TAG,next.applyTo(root()));}
        int amount(ClassResourceKind kind){return ledger().amount(kind);}
        void freshSession(){ClassResourceService.forget(player);ClassResourceService.pulse(player,level.getServer().getTickCount(),false);}
        ClassResourcePayloads.Recycle request(String kind){return new ClassResourcePayloads.Recycle(runId,kind,ledger().revision());}
        ClassResourcePayloads.View view(){check(!views.isEmpty(),"Native resource snapshot sent");return views.getLast();}
        CompoundTag disk(){
            try{return NbtIo.readCompressed(files.getFirst(),NbtAccounter.create(64L*1024*1024));}
            catch(IOException error){throw new IllegalStateException(error);}
        }
        void check(boolean condition,String message){helper.assertTrue(condition,Component.literal(message));}
        @Override public void close(){
            owned.forEach(ClassResourceService::forget);
            players.remove(profile.id());online.removeIf(p->p.getUUID().equals(profile.id()));
            if(advancements.get(profile.id()) instanceof net.minecraft.server.PlayerAdvancements progress)progress.stopListening();
            stats.remove(profile.id());advancements.remove(profile.id());
            for(long id:runIds){runs.remove(id);D1RunData.get(level.getServer()).clearRun(id);}
            channel.finishAndReleaseAll();
            try{for(var path:files)Files.deleteIfExists(path);}
            catch(IOException error){throw new IllegalStateException("Fixture files were not removed",error);}
        }
    }
    private static void exactTag(Fixture f,ClassResourceKind kind,String names){
        var expected=Arrays.stream(names.split(",")).map(name->"minecraft:"+name).collect(Collectors.toSet());
        var actual=BuiltInRegistries.ITEM.stream().filter(item->item.builtInRegistryHolder().is(kind.tag()))
                .map(item->BuiltInRegistries.ITEM.getKey(item).toString()).collect(Collectors.toSet());
        f.check(actual.equals(expected),kind.id()+" exact generated tag; observed "+actual);
    }
    public static void generatedTagsAndRecycleGuards(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2001,"dungeon_1","theurgist")){
            exactTag(f,ClassResourceKind.BREWING_SUPPLIES,"sugar,rabbit_foot,glistering_melon_slice,spider_eye,blaze_powder,golden_carrot,ghast_tear,pufferfish,magma_cream,turtle_helmet,phantom_membrane,breeze_rod,stone,cobweb,fermented_spider_eye,slime_block");
            exactTag(f,ClassResourceKind.KIBBLE,"rotten_flesh,beef,porkchop,mutton,chicken,rabbit");
            var inventory=f.player.getInventory();
            inventory.setItem(0,new ItemStack(Items.SUGAR,8));inventory.setItem(1,new ItemStack(Items.DIAMOND,4));
            inventory.setItem(2,new ItemStack(Items.BEEF,5));inventory.setItem(3,new ItemStack(Items.COOKED_BEEF,2));
            inventory.setItem(4,new ItemStack(Items.SALMON,2));
            f.ledger(f.ledger().withAmount(ClassResourceKind.BREWING_SUPPLIES,597));f.freshSession();
            f.check(f.view().amount()==597&&f.view().recyclable(),"Server advertises real eligible headroom");
            var first=f.request("brewing_supplies");ClassResourceService.recycle(f.player,first);
            f.check(f.amount(ClassResourceKind.BREWING_SUPPLIES)==600&&inventory.getItem(0).getCount()==5,
                    "Only three tagged items consumed at the cap");
            f.check(inventory.getItem(1).getCount()==4&&inventory.getItem(2).getCount()==5,"Unrelated and other-class ingredients unchanged");
            f.check(PlayerSaveProof.matches(PlayerSaveProof.snapshot(f.player),f.disk()),"Accepted native save contains the item debit and resource credit together");
            long accepted=f.ledger().revision();
            ClassResourceService.recycle(f.player,first);
            f.check(f.ledger().revision()==accepted&&inventory.getItem(0).getCount()==5,"Immediate duplicate cannot charge twice");
            f.freshSession();ClassResourceService.recycle(f.player,first);
            f.check(f.ledger().revision()==accepted&&f.amount(ClassResourceKind.BREWING_SUPPLIES)==600,"Old token stays stale after session replacement");
            f.freshSession();ClassResourceService.recycle(f.player,new ClassResourcePayloads.Recycle(f.runId,"kibble",accepted));
            f.check(f.ledger().revision()==accepted&&inventory.getItem(2).getCount()==5,"Forged other-class request consumes nothing");
            f.freshSession();ClassResourceService.recycle(f.player,new ClassResourcePayloads.Recycle(f.runId-100,"brewing_supplies",accepted));
            f.check(f.ledger().revision()==accepted,"Stale run cannot mutate current action generation");
            f.freshSession();ClassResourceService.recycle(f.player,f.request("brewing_supplies"));
            f.check(f.amount(ClassResourceKind.BREWING_SUPPLIES)==600&&inventory.getItem(0).getCount()==5&&!f.view().recyclable(),"Full resource cannot waste remaining ingredients");
            f.ledger(f.ledger().withAmount(ClassResourceKind.BREWING_SUPPLIES,590));f.freshSession();
            var carriedRequest=f.request("brewing_supplies");
            f.player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND));
            ClassResourceService.recycle(f.player,carriedRequest);
            f.check(f.ledger().revision()>carriedRequest.revision()&&f.view().revision()==f.ledger().revision(),
                    "Rejected carried-item race acknowledges the pending generation");
            f.check(f.amount(ClassResourceKind.BREWING_SUPPLIES)==590&&inventory.getItem(0).getCount()==5
                    &&f.player.inventoryMenu.getCarried().is(Items.DIAMOND),"Rejected action preserves held and inventory items");
            f.player.inventoryMenu.setCarried(ItemStack.EMPTY);f.freshSession();
            ClassResourceService.recycle(f.player,f.request("brewing_supplies"));
            f.check(f.amount(ClassResourceKind.BREWING_SUPPLIES)==595&&inventory.getItem(0).isEmpty(),"Fresh acknowledged retry consumes remaining five eligible items exactly once");
            f.setClass("bogatyr");f.freshSession();ClassResourceService.recycle(f.player,f.request("kibble"));
            f.check(f.amount(ClassResourceKind.KIBBLE)==5&&f.amount(ClassResourceKind.BREWING_SUPPLIES)==595&&inventory.getItem(2).isEmpty(),
                    "Kibble uses its own balance and eligible raw meat");
            f.check(inventory.getItem(1).getCount()==4&&inventory.getItem(3).getCount()==2&&inventory.getItem(4).getCount()==2,
                    "Diamonds, cooked meat and fish are never recycled");
            helper.succeed();
        }
    }
    public static void persistenceDeathAndOnlineLifecycle(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2002,"dungeon_2","bogatyr")){
            ClassResourceService.pulse(f.player,100,false);
            f.check(f.view().active()&&f.view().amount()==0,"Generic active dungeon starts at zero");
            ClassResourceService.pulse(f.player,119,true);f.check(f.amount(ClassResourceKind.KIBBLE)==0,"No partial-second credit");
            ClassResourceService.pulse(f.player,120,true);ClassResourceService.pulse(f.player,120,true);
            f.check(f.amount(ClassResourceKind.KIBBLE)==1,"One credit per elapsed online second, even duplicate observations");
            ClassResourceService.pulse(f.player,140,true);f.player.setHealth(0);
            ClassResourceService.pulse(f.player,160,true);
            f.check(f.amount(ClassResourceKind.KIBBLE)==3&&!f.view().alive()&&!f.view().recyclable(),"Dead online members retain and regenerate their resource without recycling");
            var dead=f.player;var clone=f.createPlayer();clone.restoreFrom(dead,false);f.replace(clone);
            f.check(f.amount(ClassResourceKind.KIBBLE)==3&&f.root().getStringOr("future_marker","").equals("retained"),
                    "Native death clone event preserves resource and unrelated owner fields");
            ClassResourceService.pulse(f.player,200,false);ClassResourceService.pulse(f.player,220,true);
            f.check(f.amount(ClassResourceKind.KIBBLE)==4,"Living clone resumes one-second regeneration");
            f.player.getInventory().setItem(1,new ItemStack(Items.DIAMOND,2));
            f.check(PlayerSaveProof.save(f.player),"Native owner save is verified");var saved=f.disk();
            var relog=f.createPlayer();relog.load(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),saved));f.replace(relog);
            ClassResourceService.pulse(f.player,5000,false);
            f.check(f.amount(ClassResourceKind.KIBBLE)==4&&f.player.getInventory().getItem(1).getCount()==2,
                    "Native owner reload retains saved resource and inventory without offline catch-up");
            ClassResourceService.pulse(f.player,5019,true);f.check(f.amount(ClassResourceKind.KIBBLE)==4,"New session requires a full online second");
            ClassResourceService.pulse(f.player,5020,true);f.check(f.amount(ClassResourceKind.KIBBLE)==5,"New session accrues exactly one point");
            f.runs.remove(f.runId);ClassResourceService.pulse(f.player,6000,true);
            f.check(f.view().runId()==0&&f.amount(ClassResourceKind.KIBBLE)==5,"Inactive run clears display and stops accrual while retaining its saved balance");
            f.registerRun(Long.MAX_VALUE-2003);ClassResourceService.pulse(f.player,6020,false);
            f.check(f.amount(ClassResourceKind.KIBBLE)==0&&f.amount(ClassResourceKind.BREWING_SUPPLIES)==0
                    &&f.ledger().revision()==0&&f.root().getStringOr("future_marker","").equals("retained"),
                    "Next generic run resets known resources and token without erasing unrelated fields");
            ClassResourceService.pulse(f.player,100000,true);
            f.check(f.amount(ClassResourceKind.KIBBLE)==1,"Sparse observations never manufacture catch-up credits");
            helper.succeed();
        }
    }
}
