package net.goui.cosmicdungeon.crafting;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.dungeon.metrics.*;
import net.goui.cosmicdungeon.npc.tamsin.GroupResourceConsent;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.resource.*;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.goui.cosmicdungeon.gametest.DurableRecoveryGameTestSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Registered native inventories, owner files and consent guards; no forced chunks or tick changes. */
public final class SharedResourceGameTests {
    private SharedResourceGameTests() {}
    private static final class Fixture implements AutoCloseable {
        final DurableRecoveryGameTestSupport.Disconnects disconnects=new DurableRecoveryGameTestSupport.Disconnects();
        final GameTestHelper h; final ServerLevel level; final long run; final Vec3 origin;
        final DungeonRunRegistryData.RunRecord record;
        final List<ServerPlayer> current=new ArrayList<>(), owned=new ArrayList<>(), online;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs; final Map<UUID,ServerPlayer> players;
        final Map<UUID,?> stats,advancements; final List<Path> files=new ArrayList<>();
        final List<io.netty.channel.embedded.EmbeddedChannel> channels=new ArrayList<>();
        final Map<UUID,List<Packet<?>>> packets=new HashMap<>();
        final CompoundTag journalBefore; final Path resourceFolder;
        @SuppressWarnings("unchecked") Fixture(GameTestHelper h,long run) {
            this.h=h;this.run=run;level=h.getLevel();var server=level.getServer();
            var anchor=h.absolutePos(BlockPos.ZERO);var chunk=new ChunkPos(anchor);
            origin=new Vec3(chunk.getMinBlockX()+8.5,anchor.getY()+10.5,chunk.getMinBlockZ()+8.5);
            try {
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)field(DungeonRunRegistryData.class,"runsById",DungeonRunRegistryData.get(server));
                players=(Map<UUID,ServerPlayer>)field(PlayerList.class,"playersByUUID",server.getPlayerList());
                online=(List<ServerPlayer>)field(PlayerList.class,"players",server.getPlayerList());
                stats=(Map<UUID,?>)field(PlayerList.class,"stats",server.getPlayerList());
                advancements=(Map<UUID,?>)field(PlayerList.class,"advancements",server.getPlayerList());
            } catch(ReflectiveOperationException ex) {throw new IllegalStateException(ex);}
            journalBefore=SharedCraftData.get(server).image();check(!runs.containsKey(run),"Unique shared crafting run");
            for(int i=0;i<3;i++) {
                var profile=new GameProfile(UUID.randomUUID(),"SharedTest"+i);String id=profile.id().toString();
                check(!players.containsKey(profile.id())&&!stats.containsKey(profile.id())&&!advancements.containsKey(profile.id()),"Unique native owner");
                var ownerFiles=List.of(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat"),
                        server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat_old"),
                        server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(id+".json"),
                        server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(id+".json"));
                check(ownerFiles.stream().noneMatch(Files::exists),"Fixture owner files must be new");files.addAll(ownerFiles);
                var player=create(profile);var root=new CompoundTag();
                root.putString(ClassData.KEY_CLASS_ID,i==0?"theurgist":"judicator");root.putString("future_marker","preserved");
                player.getPersistentData().put(ClassData.ROOT_TAG,ClassResourceLedger.forRun(root,run).applyTo(root));
                current.add(player);players.put(profile.id(),player);online.add(player);
            }
            record=new DungeonRunRegistryData.RunRecord(run,"dungeon_1","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",System.currentTimeMillis(),
                    current.stream().map(ServerPlayer::getUUID).toList(),List.of(),List.of());
            try {
                var metricRoot=server.getWorldPath(LevelResource.ROOT).resolve("Dungeon Instance Metrics");
                var instance=RunMetrics.instanceId(DungeonMetricsService.archive(server).worldId(),run);
                resourceFolder=metricRoot.resolve("resources").resolve(instance);
                var metricFiles=List.of(metricRoot.resolve(instance+".xml"),metricRoot.resolve("resource-state").resolve(instance+".xml"));
                check(!Files.exists(resourceFolder)&&metricFiles.stream().noneMatch(Files::exists),"Fixture metrics paths must be new");
                files.addAll(metricFiles);
            } catch(IOException ex){throw new IllegalStateException(ex);}
            runs.put(run,record);DungeonMetricsService.begin(server,record,true);
            for(int i=0;i<3;i++) storedConsent(i,true);
        }
        static Object field(Class<?> type,String name,Object owner)throws ReflectiveOperationException {
            var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner);
        }
        ServerPlayer create(GameProfile profile) {
            var server=level.getServer();var channel=new io.netty.channel.embedded.EmbeddedChannel();channels.add(channel);
            var player=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            packets.computeIfAbsent(profile.id(),ignored->new ArrayList<>());
            var disconnected=new boolean[]{false};
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public io.netty.channel.Channel channel(){return channel;}
                @Override public boolean isConnected(){return !disconnected[0];}
            };
            net.neoforged.neoforge.network.registration.ChannelAttributes.setConnectionType(connection,
                    net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
            net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,
                    net.neoforged.neoforge.network.registration.NetworkPayloadSetup.empty());
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)) {
                @Override public void disconnect(Component reason){disconnected[0]=true;disconnects.record(this.player,reason);}
                @Override public void disconnect(net.minecraft.network.DisconnectionDetails details){disconnect(details.reason());}
                @Override public void send(Packet<?> packet){packets.get(profile.id()).add(packet);}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){send(packet);}
                @Override public void resetPosition(){}
                @Override public void teleport(double x,double y,double z,float yaw,float pitch){}
            };
            player.setGameMode(GameType.SURVIVAL);player.setHealth(20);player.setPos(origin);owned.add(player);return player;
        }
        ServerPlayer p(int index){return current.get(index);}
        CompoundTag root(int index){return p(index).getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
        void storedConsent(int index,boolean enabled) {
            // Fixture setup/fault injection only: active gameplay changes are rejected by GroupResourceConsentGameTests.
            net.goui.cosmicdungeon.dungeon.d1.D1RunData.get(level.getServer())
                    .setValue(run,"resource_sharing_v1:"+p(index).getUUID(),enabled?"YES":"NO");
            SharedCrafting.forget(p(index));
        }
        void seed() {
            for(var player:current){player.getInventory().clearContent();SharedCrafting.forget(player);}
            p(0).getInventory().setItem(0,named(Items.NETHER_WART,2,"Authored wart"));
            p(0).getInventory().setItem(8,named(Items.DIAMOND,4,"Unrelated treasure"));
            p(1).getInventory().setItem(0,named(Items.SUGAR,4,"Authored sugar"));
            p(2).getInventory().setItem(0,named(Items.GLOWSTONE_DUST,4,"Authored glowstone"));
        }
        ItemStack named(Item item,int count,String name) {
            var stack=new ItemStack(item,count);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));return stack;
        }
        SharedCrafting.Recipe recipe() {
            return new SharedCrafting.Recipe("test:shared_resources","Shared fixture potion",Set.of("theurgist"),
                    List.of(new SharedCrafting.Requirement("wart",1,s->s.is(Items.NETHER_WART)),
                            new SharedCrafting.Requirement("sugar",1,s->s.is(Items.SUGAR)),
                            new SharedCrafting.Requirement("glowstone",1,s->s.is(Items.GLOWSTONE_DUST))),
                    named(Items.SPLASH_POTION,1,"Authored output"));
        }
        CompoundTag inventory(int index){return ChopTravelRecovery.saveInventory(p(index));}
        Path file(int index){return level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p(index).getStringUUID()+".dat");}
        CompoundTag disk(int index) {
            try{return NbtIo.readCompressed(file(index),NbtAccounter.create(64L*1024*1024));}
            catch(IOException ex){throw new IllegalStateException(ex);}
        }
        void saveAll(){for(var player:current)check(PlayerSaveProof.save(player),"Native owner file verified");}
        void reload(int index) {
            var previous=p(index);var image=disk(index);SharedCrafting.forget(previous);SupplyRequests.forget(previous);ClassResourceService.forget(previous);
            var next=create(previous.getGameProfile());next.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),image));
            online.remove(previous);online.add(next);players.put(next.getUUID(),next);current.set(index,next);next.connection.player=next;next.setPos(origin);
            check(next!=previous&&players.get(next.getUUID())==next&&PlayerSaveProof.matches(image,PlayerSaveProof.snapshot(next)),
                    "Native disk owner replaces the prior object with the exact saved inventory and proof");
        }
        SharedCraftData reloadJournal(){return DurableRecoveryGameTestSupport.reload(level.getServer(),SharedCraftData.class,SharedCraftData.ID);}
        void custody(int index,SharedCraftPlan plan) {
            var next=root(index).copy();next.put(SharedCraftTransactions.CUSTODY,plan.reservation(p(index).getUUID()));
            p(index).getPersistentData().put(ClassData.ROOT_TAG,next);check(PlayerSaveProof.save(p(index)),"Native custody persisted");
        }
        void reserve(SharedCraftPlan plan) {
            SharedCraftData.get(level.getServer()).reserve(plan);check(SharedCraftData.get(level.getServer()).flushVerified(),"World reservation persisted");
        }
        RunMetrics.Stats metrics(int index) {
            return DungeonMetricsService.snapshot(level.getServer(),record,"ACTIVE").members().stream()
                    .filter(m->m.id().equals(p(index).getUUID())).findFirst().orElseThrow().stats();
        }
        long products(){return ChopTravelRecovery.inventory(p(0)).stream().filter(s->s.is(Items.SPLASH_POTION)).mapToLong(ItemStack::getCount).sum();}
        long notices(int index) {
            return packets.get(p(index).getUUID()).stream().filter(packet->packet instanceof ClientboundSystemChatPacket chat
                    &&chat.content().getString().startsWith("You have contributed a(n) ")).count();
        }
        void unchanged(List<CompoundTag> before,String message) {
            for(int i=0;i<3;i++)check(inventory(i).equals(before.get(i)),message+" owner "+i);
        }
        List<CompoundTag> images(){return List.of(inventory(0),inventory(1),inventory(2));}
        void check(boolean condition,String message){h.assertTrue(condition,Component.literal(message));}
        @Override public void close() {
            var data=SharedCraftData.get(level.getServer());var ids=current.stream().map(ServerPlayer::getUUID).toList();
            var pending=data.pendingRun(run);
            if(pending!=null) {
                if(!ids.containsAll(pending.owners()))throw new IllegalStateException("Foreign owner in fixture journal");
                for(var id:pending.owners())data.acknowledge(pending.id(),id);
                if(pending.committed())data.acknowledgeMetrics(pending.id());
                data.retire(pending.id());
            }
            check(data.flushVerified(),"Fixture journal cleanup saved");
            for(var player:owned){SharedCrafting.forget(player);SupplyRequests.forget(player);ClassResourceService.forget(player);player.discard();}
            for(var id:ids) {
                players.remove(id);online.removeIf(player->player.getUUID().equals(id));
                if(advancements.get(id) instanceof net.minecraft.server.PlayerAdvancements progress)progress.stopListening();
                stats.remove(id);advancements.remove(id);
            }
            runs.remove(run);D1RunData.get(level.getServer()).clearRun(run);channels.forEach(io.netty.channel.embedded.EmbeddedChannel::finishAndReleaseAll);
            try {
                for(var path:files)Files.deleteIfExists(path);
                if(Files.exists(resourceFolder))try(var paths=Files.walk(resourceFolder)) {
                    for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);
                }
                DungeonMetricsService.stopped(level.getServer());
            }
            catch(IOException ex){throw new IllegalStateException("Shared crafting fixture cleanup",ex);}
        }
    }

    public static void recovery(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3101)) {
            f.seed();var server=f.level.getServer();var recipe=f.recipe();
            var availability=SharedCrafting.preview(f.p(0),f.run,recipe);
            f.check(availability.own()==0&&availability.shared()==2,"Split group inventories expose exactly two complete recipes");
            var before=f.images();var plan=SharedCrafting.prepare(f.p(0),f.run,recipe);
            f.check(plan!=null&&plan.owners().size()==3,"Exact plan includes the crafter and both ingredient owners");f.unchanged(before,"Preparation is read-only");
            var invalid=plan.image();var changed=ChopTravelRecovery.decode(f.p(0),plan.inventory(f.p(0).getUUID(),true));changed.get(8).grow(1);
            invalid.getCompoundOrEmpty("owners").getCompoundOrEmpty(f.p(0).getStringUUID()).put("after",ChopTravelRecovery.encode(f.p(0),changed));
            boolean rejected=false;
            try{SharedCraftTransactions.validateImages(f.p(0),new SharedCraftPlan(invalid));}catch(RuntimeException expected){rejected=true;}
            f.check(rejected,"A native plan cannot grant unrelated items or change authored components");
            f.reserve(plan);for(int i=0;i<3;i++)f.custody(i,plan);
            var data=SharedCraftData.get(server);data.commit(plan.id());f.check(data.flushVerified(),"Commit persisted before any inventory projection");
            data=f.reloadJournal();for(int i=0;i<3;i++)f.reload(i);
            var beforeOwnerAck=DurableRecoveryGameTestSupport.disk(server,SharedCraftData.class,SharedCraftData.ID);
            f.check(SharedCraftTransactions.reconcile(f.p(1))&&f.p(1).getInventory().getItem(0).getCount()==3,"First donor saves exactly one debit");
            // Retain the actual newer owner file with the earlier durable world image: receipt-before-ack cut.
            data=DurableRecoveryGameTestSupport.restoreCut(server,SharedCraftData.class,SharedCraftData.ID,beforeOwnerAck);
            f.reload(1);
            f.check(SharedCraftTransactions.reconcile(f.p(1)),"Native owner receipt survives a journal acknowledgement cut without another debit");
            f.check(data.pending(f.p(1).getUUID()).acknowledged(f.p(1).getUUID()),"Saved donor receipt precedes its journal acknowledgement");
            data=f.reloadJournal();for(int i=0;i<3;i++)f.reload(i);
            f.check(SharedCraftTransactions.reconcile(f.p(1))&&f.p(1).getInventory().getItem(0).getCount()==3,"Relogged donor receipt cannot debit twice");
            f.check(SharedCraftTransactions.reconcile(f.p(2)),"Last donor recovers from the actual disk decision");
            var beforeMetrics=DurableRecoveryGameTestSupport.disk(server,SharedCraftData.class,SharedCraftData.ID);
            f.check(SharedCraftTransactions.reconcile(f.p(0)),"Remaining committed owners recover after relog");
            // All owner files now contain real production receipts. Reinstall the valid all-acked pre-metrics cut.
            beforeMetrics.acknowledge(plan.id(),f.p(0).getUUID());
            data=DurableRecoveryGameTestSupport.restoreCut(server,SharedCraftData.class,SharedCraftData.ID,beforeMetrics);
            f.reload(0);
            f.check(data.pendingRun(f.run).ownersAcknowledged()&&!data.pendingRun(f.run).metricsAcknowledged(),
                    "Native journal reload retains the owner-acknowledged metrics boundary");
            f.check(SharedCraftTransactions.reconcile(f.p(0))&&data.pendingRun(f.run)==null,
                    "Production replay retires the remaining metrics boundary exactly once");
            f.check(f.products()==1&&f.p(0).getInventory().getItem(0).getCount()==1&&f.p(2).getInventory().getItem(0).getCount()==3,
                    "One output and precisely one item from each of the three owners");
            f.check(f.p(0).getInventory().getItem(8).getCount()==4&&f.p(0).getInventory().getItem(8).getHoverName().getString().equals("Unrelated treasure")
                    &&f.p(1).getInventory().getItem(0).getHoverName().getString().equals("Authored sugar"),"Unspent item components survive the transaction");
            f.check(ChopTravelRecovery.inventory(f.p(0)).stream().filter(s->s.is(Items.SPLASH_POTION))
                    .allMatch(s->s.getHoverName().getString().equals("Authored output")),"Output retains its server-authored components");
            f.check(DungeonMetricsService.resourceSequence(server,f.run)==1&&DungeonMetricsService.donationCursor(server,f.run)==2,
                    "Exactly one metrics transaction and two donated units are durable");
            f.check(f.metrics(0).used().getOrDefault("minecraft:sugar",0L)==1&&DungeonMetricsService.resourceUnits(D1RunData.get(server),f.run,f.p(0).getUUID(),"own")==1
                    &&f.metrics(1).contributed().getOrDefault("minecraft:sugar",0L)==1&&f.metrics(2).contributed().getOrDefault("minecraft:glowstone_dust",0L)==1,
                    "Own use, shared use and separate donor totals retain exact registry IDs");
            try {
                var durable=DungeonMetricsService.archive(server).readResourceState(f.run);
                f.check(durable.sequence()==1&&durable.recent().size()==3,"One resource afterimage retains every split ingredient event");
            } catch(IOException ex){throw new IllegalStateException(ex);}
            D1RunData.get(server).setValue(f.run,"metrics_v1:resource_sequence","0");
            D1RunData.get(server).setCount(f.run,"metrics_v1:used:"+f.p(0).getUUID()+":minecraft:sugar",0);
            DungeonMetricsService.stopped(server);
            f.check(DungeonMetricsService.resourceSequence(server,f.run)==1&&f.metrics(0).used().getOrDefault("minecraft:sugar",0L)==1,
                    "Restart loads the exact durable afterimage over stale SavedData without replaying a debit");
            data=f.reloadJournal();var settled=f.images();for(int i:new int[]{2,1,0}){f.reload(i);f.check(SharedCraftTransactions.reconcile(f.p(i)),"Settled replay succeeds");}
            f.unchanged(settled,"Repeated native reload preserves the settled images");
            f.check(!SharedCraftTransactions.execute(f.p(0),plan,()->true)&&f.products()==1
                    &&DungeonMetricsService.resourceSequence(server,f.run)==1,"Stale first plan cannot duplicate output or metrics");
            f.check(f.notices(1)==1&&f.notices(2)==1,"Each actual donor gets one contribution message across relog and replay");
            for(int i=0;i<3;i++)f.check(PlayerSaveProof.matches(PlayerSaveProof.snapshot(f.p(i)),f.disk(i))
                    &&f.root(i).getStringOr("future_marker","").equals("preserved"),"Verified owner files retain unrelated future data");
            f.check(data.image().equals(f.journalBefore),"Every owner and metrics acknowledgement retires only this fixture decision");
        }
        h.succeed();
    }

    private static void malformedProofs(GameTestHelper h){
        int scenario=0;
        for(String key:List.of(SharedCraftTransactions.CUSTODY,SharedCraftTransactions.RECEIPT))
            for(int shape=0;shape<3;shape++)try(var f=new Fixture(h,Long.MAX_VALUE-3120-scenario++)){
                f.seed();f.saveAll();var plan=SharedCrafting.prepare(f.p(0),f.run,f.recipe());f.reserve(plan);
                var root=f.root(0).copy();
                if(shape==0)root.putString(key,"preserve malformed proof");
                else if(shape==1)root.put(key,new ListTag());
                else{var future=key.equals(SharedCraftTransactions.CUSTODY)?plan.reservation(f.p(0).getUUID()):plan.receipt(f.p(0).getUUID());
                    future.putInt("schema",99);future.putString("unknown","preserve");root.put(key,future);}
                f.p(0).getPersistentData().put(ClassData.ROOT_TAG,root);f.saveAll();
                var owner=f.disk(0);var inventory=f.inventory(0);var journal=f.reloadJournal().image();f.reload(0);
                f.disconnects.expect(f.p(0));
                f.check(!SharedCraftTransactions.reconcile(f.p(0))&&SharedCraftTransactions.blocked(f.p(0))&&f.disconnects.recoveryHold(f.p(0)),
                        "Present malformed crafting custody/receipt holds instead of becoming missing partial preparation");
                f.check(f.root(0).equals(root)&&f.inventory(0).equals(inventory)&&f.disk(0).equals(owner)
                        &&SharedCraftData.get(f.level.getServer()).image().equals(journal),
                        "Malformed crafting recovery preserves the original native owner file and world decision");
            }
    }
    private static void zeroReceiptSequence(GameTestHelper h){
        for(boolean pending:new boolean[]{false,true})try(var f=new Fixture(h,Long.MAX_VALUE-3135-(pending?1:0))){
            f.seed();f.saveAll();var plan=SharedCrafting.prepare(f.p(0),f.run,f.recipe());
            if(pending){f.reserve(plan);for(int i=0;i<3;i++)f.custody(i,plan);}
            var root=f.root(0).copy();var malformed=plan.receipt(f.p(0).getUUID());
            malformed.putString("id",UUID.randomUUID().toString());malformed.putLong("sequence",0);
            root.put(SharedCraftTransactions.RECEIPT,malformed);f.p(0).getPersistentData().put(ClassData.ROOT_TAG,root);f.saveAll();
            f.check(SharedCraftData.get(f.level.getServer()).flushVerified(),"Zero-sequence fixture has its own durable journal even without a pending plan");
            var owner=f.disk(0);var inventory=f.inventory(0);var journal=f.reloadJournal().image();f.reload(0);
            f.disconnects.expect(f.p(0));
            f.check(!SharedCraftTransactions.reconcile(f.p(0))&&SharedCraftTransactions.blocked(f.p(0))
                    &&f.disconnects.recoveryHold(f.p(0)),"Zero-sequence prior receipt holds with and without a pending distinct operation");
            f.check(f.root(0).equals(root)&&f.disk(0).equals(owner)&&f.inventory(0).equals(inventory)
                    &&SharedCraftData.get(f.level.getServer()).image().equals(journal)
                    &&DungeonMetricsService.resourceSequence(f.level.getServer(),f.run)==0&&f.notices(1)==0&&f.notices(2)==0,
                    "Zero-sequence saved receipt and original owner/world evidence remain intact without debit, output, acknowledgement or metrics");
        }
    }
    private static void terminalReceiptConflicts(GameTestHelper h) {
        for(int scenario=0;scenario<2;scenario++)try(var f=new Fixture(h,Long.MAX_VALUE-3140-scenario)){
            f.seed();f.saveAll();var plan=SharedCrafting.prepare(f.p(0),f.run,f.recipe());f.reserve(plan);
            if(scenario==1){
                f.custody(0,plan);var data=SharedCraftData.get(f.level.getServer());data.commit(plan.id());
                f.check(data.flushVerified(),"Committed conflicting-receipt world plan is durable");
            }
            var root=f.root(0).copy();root.put(SharedCraftTransactions.RECEIPT,
                    (scenario==0?plan.commit():plan).receipt(f.p(0).getUUID()));
            f.p(0).getPersistentData().put(ClassData.ROOT_TAG,root);f.saveAll();
            var owner=f.disk(0);var inventory=f.inventory(0);var journal=f.reloadJournal().image();f.reload(0);
            f.disconnects.expect(f.p(0));
            f.check(!SharedCraftTransactions.reconcile(f.p(0))&&SharedCraftTransactions.blocked(f.p(0))
                    &&f.disconnects.recoveryHold(f.p(0)),"Same-operation opposite crafting receipt holds before terminal outcome replacement");
            f.check(f.root(0).equals(root)&&f.disk(0).equals(owner)&&f.inventory(0).equals(inventory)
                    &&SharedCraftData.get(f.level.getServer()).image().equals(journal)
                    &&DungeonMetricsService.resourceSequence(f.level.getServer(),f.run)==0&&f.notices(1)==0&&f.notices(2)==0,
                    "Conflicting terminal crafting proof preserves exact native evidence and world without settlement effects");
        }
        try(var f=new Fixture(h,Long.MAX_VALUE-3142)){
            f.seed();f.saveAll();var previous=SharedCrafting.prepare(f.p(0),f.run,f.recipe());
            f.check(SharedCraftTransactions.execute(f.p(0),previous,()->true),"Actual first craft saves supported previous receipts");
            var older=f.root(0).getCompoundOrEmpty(SharedCraftTransactions.RECEIPT).copy();
            f.check(previous.commit().receipt(f.p(0).getUUID()).equals(older),"Previous receipt comes from actual production settlement");
            f.seed();f.saveAll();var plan=SharedCrafting.prepare(f.p(0),f.run,f.recipe());f.reserve(plan);
            for(int i=0;i<3;i++)f.custody(i,plan);
            var data=SharedCraftData.get(f.level.getServer());data.commit(plan.id());f.check(data.flushVerified(),"Second distinct craft committed");
            data=f.reloadJournal();for(int i=0;i<3;i++)f.reload(i);
            f.check(!plan.id().equals(previous.id())&&f.root(0).getCompoundOrEmpty(SharedCraftTransactions.RECEIPT).equals(older),
                    "Native owner reload retains legitimate older receipt beside current custody");
            for(int i:new int[]{2,1,0})f.check(SharedCraftTransactions.reconcile(f.p(i)),"Different-operation supported receipt is supersedable");
            data=f.reloadJournal();for(int i:new int[]{0,1,2}){f.reload(i);f.check(SharedCraftTransactions.reconcile(f.p(i)),"Second craft repeat replay succeeds");}
            f.check(f.products()==1&&DungeonMetricsService.resourceSequence(f.level.getServer(),f.run)==2
                    &&data.image().equals(f.journalBefore),"Both distinct production crafts settle once without holding previous IDs");
        }
    }
    public static void guards(GameTestHelper h) {
        zeroReceiptSequence(h);
        terminalReceiptConflicts(h);
        malformedProofs(h);
        try(var f=new Fixture(h,Long.MAX_VALUE-3102)) {
            f.seed();var server=f.level.getServer();var recipe=f.recipe();var data=SharedCraftData.get(server);
            for(int slot=0;slot<36;slot++)f.p(0).getInventory().setItem(slot,new ItemStack(Items.DIAMOND,64));
            f.p(1).getInventory().setItem(1,new ItemStack(Items.NETHER_WART,2));var full=f.images();
            f.check(SharedCrafting.prepare(f.p(0),f.run,recipe)==null&&!SharedCrafting.craft(f.p(0),f.run,recipe),"Full output inventory rejects before reservation");
            f.unchanged(full,"Output-full consumes no owner's material");
            f.seed();var before=f.images();var prepared=SharedCrafting.prepare(f.p(0),f.run,recipe);var checks=new int[1];
            f.check(!SharedCraftTransactions.execute(f.p(0),prepared,()->{
                if(++checks[0]==2)f.storedConsent(1,false);
                return GroupResourceConsent.canContribute(f.p(0),f.p(1),f.run)&&GroupResourceConsent.canContribute(f.p(0),f.p(2),f.run);
            }),"Stored consent invalidated after durable reservations cancels before commit");
            f.check(checks[0]==2,"Consent is revalidated after all owner reservations");f.unchanged(before,"Withdrawn consent changes no inventory");
            f.check(data.image().equals(f.journalBefore)&&DungeonMetricsService.resourceSequence(server,f.run)==0,"Canceled reservations create neither metrics nor pending decisions");
            f.storedConsent(1,true);
            prepared=SharedCrafting.prepare(f.p(0),f.run,recipe);f.p(1).getInventory().getItem(0).shrink(1);var changed=f.images();
            f.check(!SharedCraftTransactions.execute(f.p(0),prepared,()->true),"Exact current images reject a stale count even without a dirty notification");
            f.unchanged(changed,"Stale prepared count cannot charge another owner");
            f.seed();before=f.images();f.p(1).setHealth(0);
            f.check(!SharedCrafting.craft(f.p(0),f.run,recipe),"Dead donors are unavailable");f.p(1).setHealth(20);f.unchanged(before,"Death guard consumes nothing");
            var disconnected=f.p(2);f.players.remove(disconnected.getUUID());f.online.remove(disconnected);
            try{f.check(!SharedCrafting.craft(f.p(0),f.run,recipe),"Disconnected donors cannot satisfy a cached recipe");}
            finally{f.players.put(disconnected.getUUID(),disconnected);f.online.add(disconnected);}
            f.unchanged(before,"Disconnect guard consumes nothing");
            f.check(!SharedCrafting.craft(f.p(0),f.run-1,recipe),"Wrong instance revision cannot craft");
            f.saveAll();var canceled=SharedCrafting.prepare(f.p(0),f.run,recipe);f.reserve(canceled);f.custody(1,canceled);
            data=f.reloadJournal();for(int i=0;i<3;i++)f.reload(i);
            for(int i:new int[]{2,1,0})f.check(SharedCraftTransactions.reconcile(f.p(i)),"Partially prepared crash cancels for reserved and unreserved owners");
            f.unchanged(before,"Uncommitted crash restores no output and removes no materials");
            f.check(f.products()==0&&DungeonMetricsService.resourceSequence(server,f.run)==0&&f.notices(1)==0&&f.notices(2)==0,
                    "Canceled and rejected attempts produce no metrics or contribution notifications");
            f.check(data.image().equals(f.journalBefore),"Canceled recovery preserves the pre-existing journal");
            for(int i=0;i<3;i++)f.check(!f.root(i).contains(SharedCraftTransactions.CUSTODY)
                    &&f.root(i).getStringOr("future_marker","").equals("preserved"),"Native recovery clears only fixture custody");
        }
        h.succeed();
    }
}
