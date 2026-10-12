package net.goui.cosmicdungeon.playerclass.theurgist;
import net.goui.cosmicdungeon.playerclass.resource.*;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.network.TheurgistPayloads;

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
import net.goui.cosmicdungeon.gametest.DurableRecoveryGameTestSupport;
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
public final class TheurgistActionGameTests {
    private TheurgistActionGameTests(){}
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
            journalBefore=RevivalData.get(server).image();check(!runs.containsKey(run),"Unique supply fixture run");
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
        void sync(){TheurgistActions.syncAll(level.getServer());}
        void fresh(){for(var player:current)TheurgistActions.forget(player);sync();}
        TheurgistPayloads.View view(int index){
            var list=packets.get(p(index).getUUID());
            for(int n=list.size()-1;n>=0;n--)if(list.get(n) instanceof ClientboundCustomPayloadPacket packet
                    &&packet.payload() instanceof TheurgistPayloads.View view)return view;
            throw new IllegalStateException("No Theurgist view");
        }
        TheurgistPayloads.Action action(int index,TheurgistPayloads.Kind kind,UUID target,UUID token){
            var view=view(index);return new TheurgistPayloads.Action(view.run(),view.revision(),kind,target,token);
        }
        void offer(int caster,int target){
            var t=view(caster).targets().stream().filter(x->x.player().equals(p(target).getUUID())).findFirst().orElseThrow();
            TheurgistActions.action(p(caster),action(caster,TheurgistPayloads.Kind.OFFER,t.player(),t.death()));
        }
        void decision(int target,TheurgistPayloads.Kind kind){
            var o=view(target).offer();check(o!=null,"Dead teammate has an explicit offer");
            TheurgistActions.action(p(target),action(target,kind,o.caster(),o.id()));
        }
        Path file(int index){return level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p(index).getStringUUID()+".dat");}
        CompoundTag disk(int index){
            try{return NbtIo.readCompressed(file(index),NbtAccounter.create(64L*1024*1024));}
            catch(IOException error){throw new IllegalStateException(error);}
        }
        void reload(int index){
            var old=p(index);var image=disk(index);SupplyRequests.forget(old);TheurgistActions.forget(old);ClassResourceService.forget(old);
            var next=create(old.getGameProfile());next.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),image));
            online.remove(old);online.add(next);players.put(next.getUUID(),next);current.set(index,next);next.connection.player=next;next.setPos(origin);
            check(next!=old&&players.get(next.getUUID())==next&&PlayerSaveProof.matches(image,PlayerSaveProof.snapshot(next)),
                    "Native disk revival owner replaces the prior object and retains exact saved proof");
        }
        RevivalData reloadJournal(){return DurableRecoveryGameTestSupport.reload(level.getServer(),RevivalData.class,RevivalData.ID);}
        void custody(int index,RevivalPlan plan){
            var next=root(index).copy();next.put(TheurgistRevival.CUSTODY,plan.reservation(p(index).getUUID()));
            p(index).getPersistentData().put(ClassData.ROOT_TAG,next);check(PlayerSaveProof.save(p(index)),"Revival owner reservation saved");
        }
        void receipt(int index,RevivalPlan plan){
            var next=root(index).copy();next.remove(TheurgistRevival.CUSTODY);next.put(TheurgistRevival.RECEIPT,plan.receipt(p(index).getUUID(),true));
            p(index).getPersistentData().put(ClassData.ROOT_TAG,next);check(PlayerSaveProof.save(p(index)),"Native target success receipt saved");
        }
        void check(boolean condition,String message){helper.assertTrue(condition,Component.literal(message));}
        @Override public void close(){
            for(var player:owned){SupplyRequests.forget(player);TheurgistActions.forget(player);ClassResourceService.forget(player);}
            var data=RevivalData.get(level.getServer());var ownerIds=current.stream().map(ServerPlayer::getUUID).toList();
            for(var owner:ownerIds){
                var pending=data.pending(owner);if(pending==null)continue;
                if(!ownerIds.contains(pending.caster())||!ownerIds.contains(pending.target()))
                    throw new IllegalStateException("Foreign owner in fixture journal");
                if(pending.decision()==0){data.decide(pending.id(),false);pending=data.pending(owner);}
                if(!pending.acknowledged(pending.caster()))data.acknowledge(pending.id(),pending.caster());
                if(data.pending(pending.target())!=null)data.acknowledge(pending.id(),pending.target());
            }
            if(!data.flushVerified())throw new IllegalStateException("Fixture journal cleanup not saved");
            for(UUID owner:ownerIds){
                var actual=players.remove(owner);if(actual!=null){TheurgistActions.forget(actual);SupplyRequests.forget(actual);ClassResourceService.forget(actual);level.removePlayerImmediately(actual,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}
                online.removeIf(p->p.getUUID().equals(owner));
                if(advancements.get(owner) instanceof net.minecraft.server.PlayerAdvancements progress)progress.stopListening();
                stats.remove(owner);advancements.remove(owner);
            }
            runs.remove(run);D1RunData.get(level.getServer()).clearRun(run);channels.forEach(io.netty.channel.embedded.EmbeddedChannel::finishAndReleaseAll);
            try{for(var path:files)Files.deleteIfExists(path);}catch(IOException error){throw new IllegalStateException("Fixture owner cleanup failed",error);}
        }
    }


    public static void crafting(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2210,"theurgist","judicator")){
            f.amount(0,60);f.fresh();
            for(int slot=0;slot<36;slot++)f.p(0).getInventory().setItem(slot,new ItemStack(Items.DIAMOND,64));
            f.check(!TheurgistCrafting.craft(f.p(0),f.run,false)&&f.amount(0)==60,"Retired random crafting cannot debit even a full inventory");
            f.p(0).getInventory().setItem(7,ItemStack.EMPTY);f.p(0).getInventory().setItem(8,ItemStack.EMPTY);
            var inventory=ChopTravelRecovery.saveInventory(f.p(0));var owner=f.root(0).copy();
            f.check(!TheurgistCrafting.craft(f.p(0),f.run,false)&&!TheurgistCrafting.craft(f.p(0),f.run,true),
                    "Neither normal nor epic random crafting remains available with sufficient old supplies and free slots");
            f.check(!TheurgistCrafting.craft(f.p(1),f.run,false),"Wrong-class callers also remain denied");
            for(var kind:List.of(TheurgistPayloads.Kind.CRAFT,TheurgistPayloads.Kind.EPIC)){
                f.fresh();f.check(!f.view(0).normal()&&!f.view(0).epic(),"Snapshots never advertise retired random potion actions");
                var click=f.action(0,kind,TheurgistPayloads.NONE,TheurgistPayloads.NONE);
                TheurgistActions.action(f.p(0),click);TheurgistActions.action(f.p(0),click);
            }
            f.check(f.root(0).equals(owner)&&ChopTravelRecovery.saveInventory(f.p(0)).equals(inventory),
                    "Retired direct calls and duplicate/stale action tokens preserve the entire owner ledger and inventory");
            f.check(PlayerSaveProof.save(f.p(0)),"Preserved legacy owner can still save normally");f.reload(0);
            f.check(f.amount(0)==60&&f.p(0).getInventory().getItem(7).isEmpty()&&f.p(0).getInventory().getItem(8).isEmpty()
                    &&f.p(0).getInventory().getItem(0).getCount()==64,"Native reload retains old balance without inventing new random potions");
            f.check(f.packets.values().stream().flatMap(List::stream).noneMatch(packet->packet instanceof ClientboundContainerClosePacket),
                    "Rejected retired crafting never closes the inventory");
            helper.succeed();
        }
    }
    public static void resurrection(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2211,"theurgist","judicator","theurgist")){
            f.amount(0,0);f.amount(2,0);
            f.p(1).setHealth(0);var death=MercenaryResurrection.prepareDeath(f.p(1));f.fresh();f.offer(0,1);
            f.check(f.amount(0)==0&&f.view(1).offer()!=null,"Immediately available at zero supplies and no level gate");
            f.check(f.view(1).casters().size()==2&&f.view(0).resurrection().ready(),"Dead viewer sees each player Theurgist");
            f.offer(2,1);f.check(f.view(1).offer().caster().equals(f.p(0).getUUID()),"Competing caster cannot replace one explicit offer");
            f.decision(1,TheurgistPayloads.Kind.DECLINE);
            f.check(ResurrectionCooldown.seconds(f.p(0))==0&&f.p(1).isDeadOrDying(),"Declining starts no cooldown");
            f.fresh();f.offer(0,1);
            try{
                @SuppressWarnings("unchecked") var states=(Map<Object,Object>)Fixture.field(TheurgistActions.class,"STATES",null);
                var state=states.get(f.level.getServer());
                @SuppressWarnings("unchecked") var offers=(Map<UUID,TheurgistActions.Pending>)Fixture.field(state.getClass(),"offers",state);
                var o=offers.get(f.p(1).getUUID());
                offers.put(f.p(1).getUUID(),new TheurgistActions.Pending(o.id(),o.run(),o.caster(),o.target(),o.death(),f.level.getServer().overworld().getGameTime()));
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            f.sync();f.check(f.view(1).offer()==null&&ResurrectionCooldown.seconds(f.p(0))==0,"Expired offer clears without cooldown");
            f.fresh();f.offer(0,1);
            var offered=f.view(1).offer();var accept=f.action(1,TheurgistPayloads.Kind.ACCEPT,offered.caster(),offered.id());
            var original=f.p(1);var origin=death.position();TheurgistActions.action(original,accept);
            var replacement=f.players.get(original.getUUID());f.current.set(1,replacement);f.owned.add(replacement);
            f.check(replacement!=original&&replacement.isAlive()&&replacement.position().equals(origin),"Accepted native resurrection restores latest death position");
            f.check(f.amount(0)==0&&f.amount(2)==0&&ResurrectionCooldown.seconds(f.p(0))==180
                    &&ResurrectionCooldown.seconds(f.p(2))==0,"Only successful caster starts180 seconds; nobody pays resources");
            var cooldown=ResurrectionCooldown.read(f.root(0));
            f.check(MercenaryResurrectionState.death(D1RunData.get(f.level.getServer()),f.run,replacement.getUUID())==null,"Successful latest-death claim cannot be reused");
            TheurgistActions.action(original,accept);TheurgistActions.action(replacement,accept);
            f.check(f.players.get(replacement.getUUID())==replacement&&ResurrectionCooldown.read(f.root(0)).equals(cooldown),"Duplicate/stale acceptance cannot respawn or restart cooldown");
            var damage=new net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent(replacement,replacement.damageSources().fellOutOfWorld(),false);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(damage);f.check(damage.isInvulnerable(),"Native five-second protection retained");
            f.check(RevivalData.get(f.level.getServer()).image().equals(f.journalBefore),"Successful owner acknowledgements retire only this plan");
            f.reload(0);f.check(ResurrectionCooldown.read(f.root(0)).equals(cooldown),"Native owner reload preserves exact cooldown");
            replacement.setHealth(0);MercenaryResurrection.prepareDeath(replacement);f.fresh();f.offer(0,1);
            f.check(f.view(1).offer()==null&&f.view(0).targets().stream().noneMatch(TheurgistPayloads.Target::available),"Cooldown blocks later death even with fresh action tokens");
            f.offer(2,1);f.check(f.view(1).offer()!=null,"Another ready Theurgist may offer");
            f.p(2).setHealth(0);f.sync();f.check(f.view(1).offer()==null,"Caster death invalidates outstanding offer");
            f.p(0).setHealth(0);f.sync();
            f.check(f.view(1).casters().stream().anyMatch(c->c.id().equals(f.p(0).getUUID())&&c.resurrection().state().equals("DEAD")&&c.resurrection().seconds()==180),"Dead caster status retains remaining cooldown");
            var old=f.p(0);var next=f.level.getServer().getPlayerList().respawn(old,false,net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            next.connection.player=next;f.current.set(0,next);f.owned.add(next);next.setPos(f.origin);
            f.check(ResurrectionCooldown.read(f.root(0)).equals(cooldown),"Native death clone cannot reset cooldown");
            f.players.remove(next.getUUID());f.sync();
            f.check(f.view(1).casters().stream().anyMatch(c->c.id().equals(next.getUUID())&&c.resurrection().state().equals("OFFLINE")&&c.resurrection().seconds()==-1),"Offline caster never masquerades as ready");
            f.players.put(next.getUUID(),next);
            helper.succeed();
        }
    }
    private static void malformedRevivalProofs(GameTestHelper helper){
        int scenario=0;
        for(String key:List.of(TheurgistRevival.CUSTODY,TheurgistRevival.RECEIPT))
            for(int shape=0;shape<3;shape++)try(var f=new Fixture(helper,Long.MAX_VALUE-2230-scenario++,"theurgist","judicator")){
                var plan=RevivalPlan.free(f.p(0).getUUID(),f.p(1).getUUID(),UUID.randomUUID(),f.run,new CompoundTag(),
                        f.level.getServer().overworld().getGameTime());
                var data=RevivalData.get(f.level.getServer());data.reserve(plan);f.check(data.flushVerified(),"Malformed revival fixture world reservation is durable");
                f.custody(0,plan);var root=f.root(1).copy();
                if(shape==0)root.putString(key,"preserve malformed revival");
                else if(shape==1)root.put(key,new ListTag());
                else{var future=key.equals(TheurgistRevival.CUSTODY)?plan.reservation(f.p(1).getUUID()):plan.receipt(f.p(1).getUUID(),false);
                    future.putInt("schema",99);future.putString("unknown","preserve");root.put(key,future);}
                f.p(1).getPersistentData().put(ClassData.ROOT_TAG,root);f.check(PlayerSaveProof.save(f.p(1)),"Malformed target proof is preserved natively");
                var disk=f.disk(1);var inventory=ChopTravelRecovery.saveInventory(f.p(1));var journal=f.reloadJournal().image();f.reload(0);f.reload(1);
                f.check(!TheurgistRevival.reconcile(f.p(1))&&TheurgistRevival.blocked(f.p(1)),
                        "Malformed present revival custody/receipt is held instead of proving unsuccessful preparation");
                f.check(f.root(1).equals(root)&&f.disk(1).equals(disk)&&ChopTravelRecovery.saveInventory(f.p(1)).equals(inventory)
                        &&RevivalData.get(f.level.getServer()).image().equals(journal)&&ResurrectionCooldown.read(f.root(0)).isEmpty(),
                        "Unsupported revival retains native target evidence, undecided world image and caster cooldown");
            }
    }
    private static void casterStateOrdering(GameTestHelper helper){
        for(int scenario=0;scenario<2;scenario++)try(var f=new Fixture(helper,Long.MAX_VALUE-2240-scenario,"theurgist","judicator")){
            long now=f.level.getServer().overworld().getGameTime();
            var plan=RevivalPlan.free(f.p(0).getUUID(),f.p(1).getUUID(),UUID.randomUUID(),f.run,new CompoundTag(),now);
            var data=RevivalData.get(f.level.getServer());data.reserve(plan);f.check(data.flushVerified(),"Undecided revival journal is durable");
            f.custody(0,plan);f.custody(1,plan);f.receipt(1,plan);
            var root=f.root(0).copy();var cooldown=ResurrectionCooldown.start(new CompoundTag(),now);
            if(scenario==0)cooldown.putInt("schema",99);
            root.put(ResurrectionCooldown.KEY,cooldown);f.p(0).getPersistentData().put(ClassData.ROOT_TAG,root);
            f.check(PlayerSaveProof.save(f.p(0)),"Malformed or changed caster state is saved without a current success receipt");
            var casterDisk=f.disk(0);var targetDisk=f.disk(1);var targetRoot=f.root(1).copy();var journal=f.reloadJournal().image();f.reload(0);f.reload(1);
            f.check(!TheurgistRevival.reconcile(f.p(0))&&TheurgistRevival.blocked(f.p(0)),
                    "Undecided caster source-state mismatch holds before recursively settling the successful target");
            f.check(f.root(0).equals(root)&&f.disk(0).equals(casterDisk)&&f.root(1).equals(targetRoot)&&f.disk(1).equals(targetDisk)
                    &&RevivalData.get(f.level.getServer()).image().equals(journal),
                    "Caster preflight preserves both exact native owner files and the undecided world outcome before peer recursion");
        }
    }
    public static void recovery(GameTestHelper helper){
        casterStateOrdering(helper);
        malformedRevivalProofs(helper);
        try(var f=new Fixture(helper,Long.MAX_VALUE-2212,"theurgist","judicator")){
            f.amount(0,240);var data=RevivalData.get(f.level.getServer());
            var plan=RevivalPlan.create(f.p(0).getUUID(),f.p(1).getUUID(),UUID.randomUUID(),f.run,f.ledger(0));
            data.reserve(plan);f.check(data.flushVerified(),"Pending world plan is durable before owner saves");f.custody(0,plan);f.custody(1,plan);
            f.receipt(1,plan); // Crash after native success save but before a world outcome or caster debit.
            data=f.reloadJournal();f.reload(0);f.reload(1);
            f.check(TheurgistRevival.reconcile(f.p(1))&&TheurgistRevival.reconcile(f.p(0))&&f.amount(0)==120,"Saved target success recovers exactly one120 debit");
            data=f.reloadJournal();f.reload(0);f.reload(1);
            f.check(TheurgistRevival.reconcile(f.p(0))&&TheurgistRevival.reconcile(f.p(1))&&f.amount(0)==120,"Repeated native reload cannot debit again");
            f.check(data.image().equals(f.journalBefore),"Successful journal settlement preserves unrelated plans");
            var canceled=RevivalPlan.create(f.p(0).getUUID(),f.p(1).getUUID(),UUID.randomUUID(),f.run,f.ledger(0));
            data.reserve(canceled);f.check(data.flushVerified(),"Interrupted preparation saved");f.custody(0,canceled);
            f.check(PlayerSaveProof.save(f.p(1)),"Unreserved target has its complete native owner file");
            data=f.reloadJournal();f.reload(0);f.reload(1);
            f.check(TheurgistRevival.reconcile(f.p(1))&&TheurgistRevival.reconcile(f.p(0))&&f.amount(0)==120,"Absent target success receipt cancels without resource loss");
            f.check(!f.root(0).contains(TheurgistRevival.CUSTODY)&&data.image().equals(f.journalBefore),"Partial prepare retires its reservations and journal only");
            f.check(f.root(0).getStringOr("future_marker","").equals("preserved"),"Recovery preserves unknown owner fields");
            long now=f.level.getServer().overworld().getGameTime();
            var free=RevivalPlan.free(f.p(0).getUUID(),f.p(1).getUUID(),UUID.randomUUID(),f.run,new CompoundTag(),now);
            data.reserve(free);f.check(data.flushVerified(),"Free cooldown journal saved before owner reservations");
            f.custody(0,free);f.custody(1,free);f.receipt(1,free);
            data=f.reloadJournal();f.reload(0);f.reload(1);
            f.check(TheurgistRevival.reconcile(f.p(1)),"Target success decides the disk-loaded cooldown journal before caster settlement");
            var beforeCasterAck=DurableRecoveryGameTestSupport.disk(f.level.getServer(),RevivalData.class,RevivalData.ID);
            f.check(TheurgistRevival.reconcile(f.p(1))&&TheurgistRevival.reconcile(f.p(0))
                    &&f.amount(0)==120&&ResurrectionCooldown.read(f.root(0)).equals(free.tag("after")),"Saved target success recovers free cooldown without legacy debit");
            data=DurableRecoveryGameTestSupport.restoreCut(f.level.getServer(),RevivalData.class,RevivalData.ID,beforeCasterAck);
            f.reload(0);f.reload(1);
            f.check(TheurgistRevival.reconcile(f.p(0))&&TheurgistRevival.reconcile(f.p(1))
                    &&ResurrectionCooldown.read(f.root(0)).equals(free.tag("after"))&&data.image().equals(f.journalBefore),"Repeated recovery cannot restart cooldown");
            data=f.reloadJournal();f.reload(1);f.reload(0);
            f.check(TheurgistRevival.reconcile(f.p(1))&&TheurgistRevival.reconcile(f.p(0))
                    &&ResurrectionCooldown.read(f.root(0)).equals(free.tag("after")),"Reverse settled journal/owner reload preserves the original cooldown deadline");
            f.root(0).remove(ResurrectionCooldown.KEY); // Isolated fixture for an interrupted unsuccessful free attempt.
            var canceledFree=RevivalPlan.free(f.p(0).getUUID(),f.p(1).getUUID(),UUID.randomUUID(),f.run,new CompoundTag(),now);
            data.reserve(canceledFree);f.check(data.flushVerified(),"Canceled free plan prepared");f.custody(0,canceledFree);
            data=f.reloadJournal();f.reload(0);f.reload(1);
            f.check(TheurgistRevival.reconcile(f.p(0))&&TheurgistRevival.reconcile(f.p(1))
                    &&!f.root(0).contains(ResurrectionCooldown.KEY)&&f.amount(0)==120,"No target success means no cooldown or resource charge");

            helper.succeed();
        }
    }

    public static void resourceDeathIntegration(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2604,"theurgist","judicator")){
            f.amount(0,120);f.p(0).getInventory().setItem(0,new ItemStack(Items.DIAMOND,2));f.p(1).setHealth(0);
            var firstDeath=MercenaryResurrection.prepareDeath(f.p(1));f.fresh();f.offer(0,1);
            var offer=f.view(1).offer();var oldAccept=f.action(1,TheurgistPayloads.Kind.ACCEPT,offer.caster(),offer.id());
            f.check(!TheurgistCrafting.craft(f.p(0),f.run,false)&&f.amount(0)==120,"Retired random crafting cannot spend old supplies during a free resurrection offer");
            f.check(f.view(1).offer()!=null&&ResurrectionCooldown.seconds(f.p(0))==0,"A rejected retired craft does not invalidate a free resurrection offer");
            var old=f.p(1);var next=f.level.getServer().getPlayerList().respawn(old,false,net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            next.connection.player=next;f.current.set(1,next);f.owned.add(next);next.setPos(f.origin.add(1,0,0));
            next.getInventory().setItem(8,new ItemStack(Items.APPLE,3));next.setHealth(0);
            var latest=MercenaryResurrection.prepareDeath(next);
            f.check(latest!=null&&!latest.id().equals(firstDeath.id()),"Native respawn followed by a new death replaces the canonical token");
            var data=D1RunData.get(f.level.getServer());
            net.goui.cosmicdungeon.dungeon.d1.DeathInventoryLayout.capture(data,f.run,next,latest.id());
            next.getInventory().setItem(8,ItemStack.EMPTY); // Missing physical drops must never be recreated from the layout.
            TheurgistActions.action(old,oldAccept);f.sync();
            f.check(next.isDeadOrDying()&&f.amount(0)==120&&f.view(1).offer()==null,"An old player/offer cannot resurrect a newer death");
            f.fresh();f.offer(0,1);f.decision(1,TheurgistPayloads.Kind.ACCEPT);
            var restored=f.players.get(next.getUUID());f.current.set(1,restored);f.owned.add(restored);
            var layout=net.goui.cosmicdungeon.dungeon.d1.DeathInventoryLayout.read(data,f.run,restored);
            f.check(restored!=next&&restored.isAlive()&&restored.position().equals(latest.position())&&f.amount(0)==120&&ResurrectionCooldown.seconds(f.p(0))==180,
                    "Fresh acceptance restores latest death for free and starts cooldown once");
            f.check(layout!=null&&layout.death().equals(latest.id())&&layout.entry(8).template().getCount()==3
                    &&restored.getInventory().getItem(8).isEmpty(),"Native resurrection retains organization metadata without recreating missing inventory");
            f.reload(0);f.check(f.amount(0)==120&&f.p(0).getInventory().getItem(0).is(Items.DIAMOND)&&f.p(0).getInventory().getItem(0).getCount()==2,
                    "Caster reload retains authored inventory and the untouched legacy resource balance");
            data.clearRun(f.run);
            f.check(ResurrectionCooldown.seconds(f.p(0))==180,"Run cleanup cannot reset player cooldown");
            f.check(net.goui.cosmicdungeon.dungeon.d1.DeathInventoryLayout.read(data,f.run,restored)==null,
                    "Run cleanup clears the integrated death organization record");
            helper.succeed();
        }
    }

}
