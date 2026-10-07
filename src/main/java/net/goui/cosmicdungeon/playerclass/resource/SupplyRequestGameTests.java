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
        SupplyRequestPayloads.View view(int index){
            var list=packets.get(p(index).getUUID());
            for(int n=list.size()-1;n>=0;n--)if(list.get(n) instanceof ClientboundCustomPayloadPacket packet
                    &&packet.payload() instanceof SupplyRequestPayloads.View view)return view;
            throw new IllegalStateException("No native supply view for "+index);
        }
        SupplyRequestPayloads.Action action(int index,Decision decision,List<UUID> ids){
            var view=view(index);return new SupplyRequestPayloads.Action(view.runId(),view.revision(),decision,ids);
        }
        void request(int index){check(view(index).canRequest(),"Requester has actual server availability");SupplyRequests.action(p(index),action(index,Decision.REQUEST,List.of()));}
        void supplies(int donor,int count){
            var stack=new ItemStack(Items.SUGAR,count);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Authored sugar"));
            p(donor).getInventory().setItem(0,stack);p(donor).getInventory().setItem(1,new ItemStack(Items.DIAMOND,4));
        }
        void prepareRequests(){amount(0,597);amount(1,0);supplies(2,8);fresh();request(0);request(1);}
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
        try(var f=new Fixture(helper,Long.MAX_VALUE-2101,"theurgist","theurgist","judicator")){
            f.amount(0,597);f.amount(1,0);f.supplies(2,8);f.fresh();
            var duplicate=f.action(0,Decision.REQUEST,List.of());f.request(0);
            SupplyRequests.action(f.p(0),duplicate);
            f.check(f.view(2).cards().size()==1&&!f.view(0).canRequest(),"Duplicate requester produces one card per donor");
            f.request(1);var quote=f.view(2);
            f.check(quote.cards().size()==2&&quote.cards().get(0).requesterId().equals(f.p(0).getUUID())
                    &&quote.cards().get(1).requesterId().equals(f.p(1).getUUID()),"Independent requests coexist in creation order");
            f.check(quote.cards().get(0).yield()==3&&quote.cards().get(1).yield()==8
                    &&quote.cards().get(0).requesterName().equals(f.p(0).getGameProfile().name()),"Exact requester and individual cap-limited yields");
            var shown=quote.cards().get(0).ingredients().getFirst().stack();
            f.check(shown.getCount()==3&&shown.getHoverName().getString().equals("Authored sugar"),"Preview preserves native item components and consumed count");
            var buffer=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),f.level.registryAccess());
            try{
                SupplyRequestPayloads.View.STREAM_CODEC.encode(buffer,quote);
                f.check(quote.equals(SupplyRequestPayloads.View.STREAM_CODEC.decode(buffer))&&buffer.readableBytes()==0,
                        "Native registry-backed payload roundtrip preserves exact quoted stacks/components");
            }finally{buffer.release();}
            var stale=f.action(2,Decision.ACCEPT,List.of(quote.cards().getFirst().requestId()));
            f.p(2).getInventory().getItem(0).grow(1);int packetCount=f.packets.get(f.p(2).getUUID()).size();
            SupplyRequests.action(f.p(2),stale);
            f.check(f.p(2).getInventory().getItem(0).getCount()==9&&f.amount(0)==597&&f.amount(1)==0,
                    "Changed donor inventory refreshes consent without charging");
            f.check(f.packets.get(f.p(2).getUUID()).size()>packetCount&&f.view(2).revision()>quote.revision(),
                    "Stale quote receives a fresh generation and authoritative items");

            f.prepareRequests();quote=f.view(2);var survivor=quote.cards().get(1).requestId();
            SupplyRequests.action(f.p(2),f.action(2,Decision.DENY,List.of(quote.cards().getFirst().requestId())));
            f.check(f.view(2).cards().size()==1&&f.view(2).cards().getFirst().requestId().equals(survivor)
                    &&f.p(2).getInventory().getItem(0).getCount()==8,"Deny changes only its own card and no items");
            SupplyRequests.forget(f.p(0));SupplyRequests.sync(f.p(0),true);
            SupplyRequests.action(f.p(0),f.action(0,Decision.ACCEPT,List.of(survivor)));
            f.check(f.p(2).getInventory().getItem(0).getCount()==8&&f.amount(1)==0,"Wrong owner cannot accept another donor's card");
            f.prepareRequests();quote=f.view(2);
            SupplyRequests.action(f.p(2),new SupplyRequestPayloads.Action(f.run-100,quote.revision(),Decision.ACCEPT,
                    List.of(quote.cards().getFirst().requestId())));
            f.check(f.p(2).getInventory().getItem(0).getCount()==8&&f.amount(0)==597,"Wrong run consumes nothing");

            f.prepareRequests();quote=f.view(2);
            var bulk=f.action(2,Decision.ACCEPT_ALL,quote.cards().stream().map(SupplyRequestPayloads.Card::requestId).toList());
            f.packets.values().forEach(List::clear);SupplyRequests.action(f.p(2),bulk);
            f.check(f.amount(0)==600&&f.amount(1)==5&&f.p(2).getInventory().getItem(0).isEmpty(),
                    "Ordered bulk gives three supplies to the nearly-full first owner and only the remaining five to the second");
            f.check(f.p(2).getInventory().getItem(1).getCount()==4,"Bulk preserves unrelated authored inventory");
            f.check(f.view(2).cards().isEmpty(),"Accepted donor cards finish independently");
            f.check(f.packets.values().stream().flatMap(List::stream).noneMatch(packet->packet instanceof ClientboundContainerClosePacket),
                    "Inventory consent must not send any native close-window packet");
            SupplyRequests.action(f.p(2),bulk);
            f.check(f.amount(0)==600&&f.amount(1)==5,"Duplicate bulk cannot grant a second credit");
            f.check(SupplyTransferData.get(f.level.getServer()).image().equals(f.journalBefore),"Completed native transfers leave no fixture journal entries");

            f.amount(0,597);f.fresh();f.request(0);
            SupplyRequestEvents.logout(new PlayerEvent.PlayerLoggedOutEvent(f.p(0)));f.sync();
            f.check(f.view(2).cards().isEmpty(),"Logout expires consent involving that player");
            f.fresh();f.request(0);long beforeDeath=f.view(0).revision();
            f.p(0).setHealth(0);SupplyRequestEvents.died(new LivingDeathEvent(f.p(0),f.p(0).damageSources().generic()));f.sync();
            f.check(f.view(2).cards().isEmpty()&&!f.view(0).alive()&&f.view(0).revision()>beforeDeath,
                    "Death invalidates consent and advances the same-connection view generation");
            f.p(0).setHealth(20);long beforeRespawn=f.view(0).revision();
            SupplyRequestEvents.respawn(new PlayerEvent.PlayerRespawnEvent(f.p(0),false));
            f.check(f.view(0).revision()>beforeRespawn,"Respawn never resets the same connection to an old generation");
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
}
