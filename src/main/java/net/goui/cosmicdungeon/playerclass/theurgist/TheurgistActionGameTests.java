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
        }
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
            f.check(!TheurgistCrafting.craft(f.p(0),f.run,false)&&f.amount(0)==60,"Full inventory cannot lose supplies or drop a potion");
            f.p(0).getInventory().setItem(7,ItemStack.EMPTY);
            f.check(TheurgistCrafting.craft(f.p(0),f.run,false)&&f.amount(0)==40,"One normal physical splash costs20");
            var item=f.p(0).getInventory().getItem(7);
            f.check(item.is(Items.SPLASH_POTION)&&item.getCount()==1&&TheurgistPotionCatalog.pool(false).contains(item.get(DataComponents.POTION_CONTENTS).potion().orElseThrow()),"Crafted normal uses exact native pool");
            f.p(0).getInventory().setItem(8,ItemStack.EMPTY);
            f.check(TheurgistCrafting.craft(f.p(0),f.run,true)&&f.amount(0)==0,"One epic physical splash costs40");
            f.check(TheurgistPotionCatalog.pool(true).contains(f.p(0).getInventory().getItem(8).get(DataComponents.POTION_CONTENTS).potion().orElseThrow()),"Epic pool contains actual tierII only");
            f.check(!TheurgistCrafting.craft(f.p(0),f.run,false)&&!TheurgistCrafting.craft(f.p(1),f.run,false),"Insufficient resources and wrong class cannot craft");
            f.check(f.p(0).getInventory().getItem(0).is(Items.DIAMOND)&&f.p(0).getInventory().getItem(0).getCount()==64,"Crafting preserves every unrelated inventory slot");
            f.reload(0);f.check(f.amount(0)==0&&f.p(0).getInventory().getItem(7).is(Items.SPLASH_POTION)&&f.p(0).getInventory().getItem(8).is(Items.SPLASH_POTION),"Native reload pairs physical potion delivery and exact resource debit");
            f.amount(0,40);f.p(0).getInventory().setItem(9,ItemStack.EMPTY);f.fresh();
            var click=f.action(0,TheurgistPayloads.Kind.CRAFT,TheurgistPayloads.NONE,TheurgistPayloads.NONE);
            TheurgistActions.action(f.p(0),click);TheurgistActions.action(f.p(0),click);
            f.check(f.amount(0)==20,"Duplicate craft generation spends only once");
            f.check(f.packets.values().stream().flatMap(List::stream).noneMatch(packet->packet instanceof ClientboundContainerClosePacket),"Crafting keeps the native inventory open");
            helper.succeed();
        }
    }
    public static void resurrection(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2211,"theurgist","judicator","theurgist")){
            f.amount(0,240);f.amount(2,240);
            f.p(1).setHealth(0);var death=MercenaryResurrection.prepareDeath(f.p(1));f.fresh();f.offer(0,1);
            f.check(f.amount(0)==240&&f.view(1).offer()!=null,"Offering resurrection costs nothing");
            f.offer(2,1);f.check(f.view(1).offer().caster().equals(f.p(0).getUUID()),"Competing caster cannot replace one explicit offer");
            f.decision(1,TheurgistPayloads.Kind.DECLINE);
            f.check(f.amount(0)==240&&f.p(1).isDeadOrDying(),"Declining costs nothing and does not respawn");
            f.fresh();f.offer(0,1);
            var offered=f.view(1).offer();var accept=f.action(1,TheurgistPayloads.Kind.ACCEPT,offered.caster(),offered.id());
            var original=f.p(1);var origin=death.position();TheurgistActions.action(original,accept);
            var replacement=f.players.get(original.getUUID());f.current.set(1,replacement);f.owned.add(replacement);
            f.check(replacement!=original&&replacement.isAlive()&&replacement.position().equals(origin),"Accepted native resurrection restores latest death position in generic active dungeon");
            f.check(f.amount(0)==120&&f.amount(2)==240,"Only accepted caster is charged exactly120");
            f.check(MercenaryResurrectionState.death(D1RunData.get(f.level.getServer()),f.run,replacement.getUUID())==null,"Successful shared latest-death claim cannot be reused");
            TheurgistActions.action(original,accept);TheurgistActions.action(replacement,accept);
            f.check(f.players.get(replacement.getUUID())==replacement&&f.amount(0)==120,"Duplicate and stale players cannot respawn or charge twice");
            var damage=new net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent(replacement,replacement.damageSources().fellOutOfWorld(),false);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(damage);f.check(damage.isInvulnerable(),"Shared native five-second protection is retained");
            f.check(RevivalData.get(f.level.getServer()).image().equals(f.journalBefore),"Successful owner acknowledgements retire only this revival plan");
            // A later genuine death can be offered immediately: player resurrection has no mercenary cooldown.
            replacement.setHealth(0);MercenaryResurrection.prepareDeath(replacement);f.fresh();f.offer(0,1);
            f.check(f.view(1).offer()!=null,"No player resurrection cooldown or extra skill level gate");
            f.p(0).setHealth(0);f.sync();f.check(f.view(1).offer()==null,"Caster death invalidates outstanding offer before acceptance");
            f.p(0).setHealth(20);f.amount(0,119);f.fresh();
            f.check(f.view(0).targets().stream().noneMatch(TheurgistPayloads.Target::available),"Below120 has no enabled resurrection action");
            helper.succeed();
        }
    }
    public static void recovery(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2212,"theurgist","judicator")){
            f.amount(0,240);var data=RevivalData.get(f.level.getServer());
            var plan=RevivalPlan.create(f.p(0).getUUID(),f.p(1).getUUID(),UUID.randomUUID(),f.run,f.ledger(0));
            data.reserve(plan);f.check(data.flushVerified(),"Pending world plan is durable before owner saves");f.custody(0,plan);f.custody(1,plan);
            f.receipt(1,plan); // Crash after native success save but before a world outcome or caster debit.
            f.reload(0);f.reload(1);
            f.check(TheurgistRevival.reconcile(f.p(1))&&TheurgistRevival.reconcile(f.p(0))&&f.amount(0)==120,"Saved target success recovers exactly one120 debit");
            f.reload(0);f.reload(1);
            f.check(TheurgistRevival.reconcile(f.p(0))&&TheurgistRevival.reconcile(f.p(1))&&f.amount(0)==120,"Repeated native reload cannot debit again");
            f.check(data.image().equals(f.journalBefore),"Successful journal settlement preserves unrelated plans");
            var canceled=RevivalPlan.create(f.p(0).getUUID(),f.p(1).getUUID(),UUID.randomUUID(),f.run,f.ledger(0));
            data.reserve(canceled);f.check(data.flushVerified(),"Interrupted preparation saved");f.custody(0,canceled);
            f.reload(0);f.reload(1);
            f.check(TheurgistRevival.reconcile(f.p(1))&&TheurgistRevival.reconcile(f.p(0))&&f.amount(0)==120,"Absent target success receipt cancels without resource loss");
            f.check(!f.root(0).contains(TheurgistRevival.CUSTODY)&&data.image().equals(f.journalBefore),"Partial prepare retires its reservations and journal only");
            f.check(f.root(0).getStringOr("future_marker","").equals("preserved"),"Recovery preserves unknown owner fields");
            helper.succeed();
        }
    }
}
