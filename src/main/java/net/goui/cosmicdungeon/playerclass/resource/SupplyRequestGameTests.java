package net.goui.cosmicdungeon.playerclass.resource;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.network.SupplyRequestPayloads;
import net.goui.cosmicdungeon.network.SupplyRequestPayloads.Decision;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Real owner files and registered stacks, isolated synchronous CI fixtures; no forced chunks or server tick edits. */
public final class SupplyRequestGameTests {
    private SupplyRequestGameTests(){}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;final ServerLevel level;final Vec3 origin;final long run;
        final List<ServerPlayer> current=new ArrayList<>(),owned=new ArrayList<>(),online;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs;final Map<UUID,ServerPlayer> players;
        final Map<UUID,?> stats,advancements;final List<Path> files=new ArrayList<>();
        final List<io.netty.channel.embedded.EmbeddedChannel> channels=new ArrayList<>();
        final Map<UUID,List<Packet<?>>> packets=new HashMap<>();
        final CompoundTag journalBefore;
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper,long run,String... classes){
            this.helper=helper;this.level=helper.getLevel();this.run=run;var server=level.getServer();
            var anchor=helper.absoluteVec(new Vec3(1.5,10.5,1.5));var chunk=new ChunkPos(BlockPos.containing(anchor));
            origin=new Vec3(chunk.getMinBlockX()+8.5,anchor.y,chunk.getMinBlockZ()+8.5);
            try{
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)field(DungeonRunRegistryData.class,"runsById",DungeonRunRegistryData.get(server));
                players=(Map<UUID,ServerPlayer>)field(PlayerList.class,"playersByUUID",server.getPlayerList());
                online=(List<ServerPlayer>)field(PlayerList.class,"players",server.getPlayerList());
                stats=(Map<UUID,?>)field(PlayerList.class,"stats",server.getPlayerList());
                advancements=(Map<UUID,?>)field(PlayerList.class,"advancements",server.getPlayerList());
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            journalBefore=SupplyTransferData.get(server).image();check(!runs.containsKey(run),"Unique supply fixture run");
            for(int i=0;i<classes.length;i++){
                var profile=new GameProfile(UUID.randomUUID(),"SupplyTest"+i);String id=profile.id().toString();
                check(!players.containsKey(profile.id())&&!stats.containsKey(profile.id())&&!advancements.containsKey(profile.id()),"Unique owner");
                var ownerFiles=List.of(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat"),
                        server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat_old"),
                        server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(id+".json"),
                        server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(id+".json"));
                check(ownerFiles.stream().noneMatch(Files::exists),"Native fixture files must be new");files.addAll(ownerFiles);
                var player=create(profile);var root=new CompoundTag();root.putString(ClassData.KEY_CLASS_ID,classes[i]);
                root.putString("future_marker","preserved");player.getPersistentData().put(ClassData.ROOT_TAG,ClassResourceLedger.forRun(root,run).applyTo(root));
                current.add(player);players.put(profile.id(),player);online.add(player);
            }
            runs.put(run,new DungeonRunRegistryData.RunRecord(run,"dungeon_2","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,current.stream().map(ServerPlayer::getUUID).toList(),List.of(),List.of()));
        }
        private static Object field(Class<?> type,String name,Object owner)throws ReflectiveOperationException{
            var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner);
        }
        ServerPlayer create(GameProfile profile){
            var channel=new io.netty.channel.embedded.EmbeddedChannel();channels.add(channel);
            var server=level.getServer();var player=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            packets.computeIfAbsent(profile.id(),id->new ArrayList<>());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
                @Override public io.netty.channel.Channel channel(){return channel;}
                @Override public boolean isConnected(){return true;}
            };
            net.neoforged.neoforge.network.registration.ChannelAttributes.setConnectionType(connection,
                    net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
            net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,
                    net.neoforged.neoforge.network.registration.NetworkPayloadSetup.empty());
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
                @Override public void send(Packet<?> packet){packets.get(profile.id()).add(packet);}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){send(packet);}
                @Override public void resetPosition(){}
                @Override public void teleport(double x,double y,double z,float yaw,float pitch){}
            };
            player.setGameMode(GameType.SURVIVAL);player.setHealth(20);player.setPos(origin);owned.add(player);return player;
        }
        ServerPlayer p(int index){return current.get(index);}
        CompoundTag root(int index){return p(index).getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
        ClassResourceLedger ledger(int index){return ClassResourceLedger.forRun(root(index),run);}
        void amount(int index,int amount){
            p(index).getPersistentData().put(ClassData.ROOT_TAG,ledger(index).withAmount(ClassResourceKind.BREWING_SUPPLIES,amount).applyTo(root(index)));
        }
        int amount(int index){return ledger(index).amount(ClassResourceKind.BREWING_SUPPLIES);}
        void sync(){SupplyRequests.syncAll(level.getServer());}
        void fresh(){for(var player:current)SupplyRequests.forget(player);sync();}
        void supplies(int donor,int count){
            var stack=new ItemStack(Items.SUGAR,count);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Authored sugar"));
            p(donor).getInventory().setItem(0,stack);p(donor).getInventory().setItem(1,new ItemStack(Items.DIAMOND,4));
        }
        Path file(int index){return level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p(index).getStringUUID()+".dat");}
        CompoundTag disk(int index){
            try{return NbtIo.readCompressed(file(index),NbtAccounter.create(64L*1024*1024));}
            catch(IOException error){throw new IllegalStateException(error);}
        }
        void reload(int index){
            var old=p(index);var image=disk(index);SupplyRequests.forget(old);ClassResourceService.forget(old);
            var next=create(old.getGameProfile());next.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),image));
            online.remove(old);online.add(next);players.put(next.getUUID(),next);current.set(index,next);next.connection.player=next;next.setPos(origin);
        }
        void custody(int index,SupplyTransferPlan plan){
            var next=root(index).copy();next.put(SupplyTransfers.CUSTODY,plan.reservation(p(index).getUUID()));
            p(index).getPersistentData().put(ClassData.ROOT_TAG,next);check(PlayerSaveProof.save(p(index)),"Owner reservation persisted");
        }
        SupplyTransferPlan plan(int donor,int recipient,int amount){
            var before=ChopTravelRecovery.inventory(p(donor));var after=ChopTravelRecovery.inventory(p(donor));
            after.getFirst().shrink(amount);
            return SupplyTransferPlan.create(p(donor).getUUID(),p(recipient).getUUID(),run,ClassResourceKind.BREWING_SUPPLIES,amount,
                    ChopTravelRecovery.encode(p(donor),before),ChopTravelRecovery.encode(p(donor),after),ledger(recipient));
        }
        void check(boolean condition,String message){helper.assertTrue(condition,Component.literal(message));}
        @Override public void close(){
            for(var player:owned){SupplyRequests.forget(player);ClassResourceService.forget(player);}
            var data=SupplyTransferData.get(level.getServer());var ownerIds=current.stream().map(ServerPlayer::getUUID).toList();
            for(var owner:ownerIds){
                var pending=data.pending(owner);if(pending==null)continue;
                if(!ownerIds.contains(pending.donor())||!ownerIds.contains(pending.recipient()))
                    throw new IllegalStateException("Foreign owner in fixture journal");
                if(!pending.acknowledged(pending.donor()))data.acknowledge(pending.id(),pending.donor());
                if(data.pending(pending.recipient())!=null)data.acknowledge(pending.id(),pending.recipient());
            }
            if(!data.flushVerified())throw new IllegalStateException("Fixture journal cleanup not saved");
            for(UUID owner:ownerIds){
                players.remove(owner);online.removeIf(p->p.getUUID().equals(owner));
                if(advancements.get(owner) instanceof net.minecraft.server.PlayerAdvancements progress)progress.stopListening();
                stats.remove(owner);advancements.remove(owner);
            }
            runs.remove(run);D1RunData.get(level.getServer()).clearRun(run);channels.forEach(io.netty.channel.embedded.EmbeddedChannel::finishAndReleaseAll);
            try{for(var path:files)Files.deleteIfExists(path);}catch(IOException error){throw new IllegalStateException("Fixture owner cleanup failed",error);}
        }
    }

    public static void consentAndBulk(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2101,"theurgist","bogatyr","judicator")){
            f.amount(0,597);f.amount(1,12);f.supplies(2,8);
            var roots=f.current.stream().map(player->player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy()).toList();
            var inventories=f.current.stream().map(ChopTravelRecovery::saveInventory).toList();
            f.fresh();
            for(int owner=0;owner<f.current.size();owner++){
                SupplyRequests.sync(f.p(owner),true);
                for(var decision:Decision.values()){
                    var ids=decision==Decision.REQUEST?List.<UUID>of():List.of(UUID.randomUUID());
                    var stale=new SupplyRequestPayloads.Action(f.run,7,decision,ids);
                    SupplyRequests.action(f.p(owner),stale);SupplyRequests.action(f.p(owner),stale);
                    SupplyRequests.action(f.p(owner),new SupplyRequestPayloads.Action(f.run-100,0,decision,ids));
                }
            }
            f.sync();
            for(int owner=0;owner<f.current.size();owner++){
                f.check(f.root(owner).equals(roots.get(owner))&&ChopTravelRecovery.saveInventory(f.p(owner)).equals(inventories.get(owner)),
                        "Retired request, single and bulk decisions preserve every owner's ledger, authored components and inventory");
                f.check(PlayerSaveProof.save(f.p(owner)),"Preserved owner still supports native saving");f.reload(owner);
                f.check(f.root(owner).equals(roots.get(owner))&&ChopTravelRecovery.saveInventory(f.p(owner)).equals(inventories.get(owner)),
                        "Reload cannot reactivate or apply a retired request");
            }
            f.check(f.packets.values().stream().flatMap(List::stream).noneMatch(packet->
                    packet instanceof ClientboundCustomPayloadPacket custom&&custom.payload() instanceof SupplyRequestPayloads.View),
                    "No class receives the retired Requests UI snapshot");
            f.check(f.packets.values().stream().flatMap(List::stream).noneMatch(packet->packet instanceof ClientboundContainerClosePacket),
                    "Retired requests never close a native menu");
            f.check(SupplyTransferData.get(f.level.getServer()).image().equals(f.journalBefore),"No retired decision starts a new transfer");
            helper.succeed();
        }
    }
    public static void saveRecovery(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2102,"judicator","theurgist")){
            f.supplies(0,8);f.amount(1,10);var plan=f.plan(0,1,3);var data=SupplyTransferData.get(f.level.getServer());
            var invalid=plan.image();var changed=ChopTravelRecovery.inventory(f.p(0));changed.get(0).shrink(3);changed.get(1).grow(1);
            invalid.put("inventory_after",ChopTravelRecovery.encode(f.p(0),changed));
            boolean rejected=false;
            try{SupplyTransfers.validateImages(f.p(0),new SupplyTransferPlan(invalid));}
            catch(RuntimeException expected){rejected=true;}
            f.check(rejected,"Malformed saved plan cannot project an unrelated item increase");
            data.reserve(plan);f.check(data.flushVerified(),"Prepared world decision persisted");
            f.custody(0,plan);f.custody(1,plan);data.commit(plan.id());f.check(data.flushVerified(),"Commit decision persisted before either owner projection");
            f.check(SupplyTransfers.reconcile(f.p(0)),"Committed donor settles first");
            f.check(f.p(0).getInventory().getItem(0).getCount()==5&&data.pending(f.p(0).getUUID()).acknowledged(f.p(0).getUUID()),
                    "Donor item debit and receipt are saved before acknowledgement");
            f.reload(0);f.reload(1);
            f.check(SupplyTransfers.reconcile(f.p(0))&&f.p(0).getInventory().getItem(0).getCount()==5,
                    "Reloaded acknowledged donor does not lose items twice");
            f.check(SupplyTransfers.reconcile(f.p(1))&&f.amount(1)==13,"Reloaded reserved recipient receives exactly one committed credit");
            f.check(SupplyTransfers.reconcile(f.p(0))&&SupplyTransfers.reconcile(f.p(1))&&f.amount(1)==13
                    &&f.p(0).getInventory().getItem(0).getCount()==5,"Repeated settled recovery is idempotent");
            f.check(PlayerSaveProof.matches(PlayerSaveProof.snapshot(f.p(0)),f.disk(0))
                    &&PlayerSaveProof.matches(PlayerSaveProof.snapshot(f.p(1)),f.disk(1)),"Both native owner files match their settled paired states");
            f.check(data.image().equals(f.journalBefore),"Both acknowledgements retire only this completed journal plan");

            var canceled=f.plan(0,1,2);data.reserve(canceled);f.check(data.flushVerified(),"Partial preparation persisted");
            f.custody(0,canceled); // Crash before recipient reservation and before the commit decision.
            f.reload(0);f.reload(1);
            f.check(SupplyTransfers.reconcile(f.p(0))&&SupplyTransfers.reconcile(f.p(1)),"Uncommitted partial preparation cancels safely for both owners");
            f.check(f.p(0).getInventory().getItem(0).getCount()==5&&f.amount(1)==13
                    &&f.p(0).getInventory().getItem(1).getCount()==4,"Cancellation neither consumes supplies nor grants resources");
            f.check(!f.root(0).contains(SupplyTransfers.CUSTODY)&&!f.root(1).contains(SupplyTransfers.CUSTODY)
                    &&data.image().equals(f.journalBefore),"Canceled receipts clear custody and preserve unrelated global journal entries");
            f.check(f.root(0).getStringOr("future_marker","").equals("preserved")
                    &&f.root(1).getStringOr("future_marker","").equals("preserved"),"Unknown owner fields survive native reload and recovery");
            helper.succeed();
        }
    }

    /** Retirement cannot reinterpret or erase an already committed pre-upgrade transfer. */
    public static void craftingAndCapIntegration(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2601,"theurgist","judicator")){
            f.amount(0,597);f.supplies(1,8);var plan=f.plan(1,0,3);var data=SupplyTransferData.get(f.level.getServer());
            data.reserve(plan);f.check(data.flushVerified(),"Legacy prepared transfer is durable");
            f.custody(0,plan);f.custody(1,plan);data.commit(plan.id());f.check(data.flushVerified(),"Legacy commit remains durable before retirement entrypoints run");
            var pending=data.image();var recipient=f.root(0).copy();var donor=f.root(1).copy();
            f.check(!net.goui.cosmicdungeon.playerclass.theurgist.TheurgistCrafting.craft(f.p(0),f.run,false),
                    "Retired random crafting cannot interfere with reserved legacy balances");
            ClassResourceService.pulse(f.p(0),100,false);ClassResourceService.pulse(f.p(0),120,true);
            SupplyRequests.syncAll(f.level.getServer());
            SupplyRequests.action(f.p(1),new SupplyRequestPayloads.Action(f.run,0,Decision.ACCEPT,List.of(UUID.randomUUID())));
            f.check(data.image().equals(pending)&&f.root(0).equals(recipient)&&f.root(1).equals(donor)
                    &&f.p(1).getInventory().getItem(0).getCount()==8,
                    "Retired gameplay preserves the exact pending decision and both custody records without prematurely consuming items");
            f.check(SupplyTransfers.reconcile(f.p(1))&&SupplyTransfers.reconcile(f.p(0)),
                    "Explicit legacy recovery remains available after Requests and Brewing Supplies retirement");
            f.check(f.amount(0)==600&&f.p(1).getInventory().getItem(0).getCount()==5,
                    "Committed old transfer settles its three-item debit and capped historical credit exactly once");
            f.reload(0);f.reload(1);
            f.check(SupplyTransfers.reconcile(f.p(0))&&SupplyTransfers.reconcile(f.p(1))&&f.amount(0)==600
                    &&f.p(1).getInventory().getItem(0).getCount()==5,"Reloaded old transfer cannot apply twice");
            f.check(f.p(1).getInventory().getItem(0).getHoverName().getString().equals("Authored sugar")
                    &&f.root(0).getStringOr("future_marker","").equals("preserved"),"Recovery preserves authored components and unknown owner fields");
            f.check(data.image().equals(f.journalBefore),"Recovered legacy plan retires only its own journal entry");
        }
        try(var f=new Fixture(helper,Long.MAX_VALUE-2602,"theurgist","judicator","pyroclast")){
            f.amount(0,597);f.supplies(1,8);f.supplies(2,8);
            var before=f.root(0).copy();
            ClassResourceService.pulse(f.p(0),100,false);
            for(int tick=120;tick<=180;tick+=20)ClassResourceService.pulse(f.p(0),tick,true);
            ClassResourceService.recycle(f.p(0),new net.goui.cosmicdungeon.network.ClassResourcePayloads.Recycle(f.run,"brewing_supplies",f.ledger(0).revision()));
            for(int donor=1;donor<=2;donor++)
                SupplyRequests.action(f.p(donor),new SupplyRequestPayloads.Action(f.run,0,Decision.ACCEPT_ALL,List.of(UUID.randomUUID())));
            f.check(f.root(0).equals(before)&&f.p(1).getInventory().getItem(0).getCount()==8&&f.p(2).getInventory().getItem(0).getCount()==8,
                    "Retired regeneration, recycling and racing bulk donors cannot change a historical balance or consume either inventory");
            f.check(SupplyTransferData.get(f.level.getServer()).image().equals(f.journalBefore),"Retired interleavings create no unsettled transfer");
        }
        helper.succeed();
    }

}
