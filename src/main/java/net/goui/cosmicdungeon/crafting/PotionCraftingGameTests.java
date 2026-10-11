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
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Real owner saves exercise server recipe decisions and persistent brewing fuel. */
public final class PotionCraftingGameTests {
    private PotionCraftingGameTests() {}
    private static final class Fixture implements AutoCloseable {
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
                var profile=new GameProfile(UUID.randomUUID(),"PotionTest"+i);String id=profile.id().toString();
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
            for(int i=0;i<3;i++) consent(i,true);
        }
        static Object field(Class<?> type,String name,Object owner)throws ReflectiveOperationException {
            var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner);
        }
        ServerPlayer create(GameProfile profile) {
            var server=level.getServer();var channel=new io.netty.channel.embedded.EmbeddedChannel();channels.add(channel);
            var player=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            packets.computeIfAbsent(profile.id(),ignored->new ArrayList<>());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public io.netty.channel.Channel channel(){return channel;}
                @Override public boolean isConnected(){return true;}
            };
            net.neoforged.neoforge.network.registration.ChannelAttributes.setConnectionType(connection,
                    net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
            net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,
                    net.neoforged.neoforge.network.registration.NetworkPayloadSetup.empty());
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)) {
                @Override public void send(Packet<?> packet){packets.get(profile.id()).add(packet);}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){send(packet);}
                @Override public void resetPosition(){}
                @Override public void teleport(double x,double y,double z,float yaw,float pitch){}
            };
            player.setGameMode(GameType.SURVIVAL);player.setHealth(20);player.setPos(origin);owned.add(player);return player;
        }
        ServerPlayer p(int index){return current.get(index);}
        CompoundTag root(int index){return p(index).getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
        void consent(int index,boolean enabled) {
            check(GroupResourceConsent.choose(p(index),run,Long.toString(run),enabled)==null,"Native consent update succeeds");
            SharedCrafting.forget(p(index));
        }
        void clear() {
            for(var player:current){player.getInventory().clearContent();SharedCrafting.forget(player);}
        }
        void classId(int index,String id) {
            var root=root(index).copy();root.putString(ClassData.KEY_CLASS_ID,id);p(index).getPersistentData().put(ClassData.ROOT_TAG,root);
            SharedCrafting.forget(p(index));
        }
        void potion(int index,int slot,Item container,net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
            p(index).getInventory().setItem(slot,net.minecraft.world.item.alchemy.PotionContents.createItemStack(container,potion));SharedCrafting.forget(p(index));
        }
        void item(int index,int slot,Item item,int count) {
            p(index).getInventory().setItem(slot,new ItemStack(item,count));SharedCrafting.forget(p(index));
        }
        long count(int index,Item item){return ChopTravelRecovery.inventory(p(index)).stream().filter(s->s.is(item)).mapToLong(ItemStack::getCount).sum();}
        int charges(int index){return BrewingFuel.charges(SharedCraftData.get(level.getServer()).fuel(p(index).getUUID()));}
        long revision(int index){return SharedCraftData.get(level.getServer()).fuel(p(index).getUUID()).getLongOr("revision",-1);}
        void healing(int owner,int bottles) {
            for(int i=0;i<bottles;i++)potion(owner,i,Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER);
            item(owner,3,Items.NETHER_WART,bottles);item(owner,4,Items.GLISTERING_MELON_SLICE,bottles);
            item(owner,5,Items.GUNPOWDER,bottles);item(owner,6,Items.BLAZE_POWDER,1);
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
        }
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
            try {
                @SuppressWarnings("unchecked") var fuel=(Map<UUID,CompoundTag>)field(SharedCraftData.class,"fuel",data);
                for(var id:ids)fuel.remove(id);
                data.setDirty();
            } catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            check(data.image().getCompoundOrEmpty("pending").equals(journalBefore.getCompoundOrEmpty("pending")),"Other pending decisions remain untouched");
            check(data.image().getCompoundOrEmpty(BrewingFuel.KEY).equals(journalBefore.getCompoundOrEmpty(BrewingFuel.KEY)),"Other owners' saved fuel remains untouched");
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
            catch(IOException ex){throw new IllegalStateException("Potion crafting fixture cleanup",ex);}
        }
    }


    public static void transactions(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3201)) {
            var server=f.level.getServer();String healing="minecraft:healing_splash";
            f.clear();f.healing(0,2);f.healing(1,2);var donorBefore=f.inventory(1);
            var offer=PotionCrafting.quote(f.p(0),f.run,healing);
            f.check(offer.own()==2&&offer.fuel().charges()==0&&offer.fuel().capacity()==20&&offer.steps()==3&&offer.powders()==1,
                    "Two complete own water recipes expose exact three-step fuel cost");
            var own=PotionCrafting.prepare(f.p(0),f.run,healing);
            f.check(own!=null&&own.owners().equals(List.of(f.p(0).getUUID()))&&own.hasFuel(),
                    "A complete own route is selected before any group material");
            f.check(PotionCrafting.craft(f.p(0),f.run,healing),"First native instant Healing craft succeeds");
            f.check(f.charges(0)==17&&f.revision(0)==1&&f.count(0,Items.BLAZE_POWDER)==0&&f.products()==1,
                    "One powder supplies twenty steps and first full route preserves seventeen charges");
            f.check(f.count(0,Items.NETHER_WART)==1&&f.count(0,Items.GLISTERING_MELON_SLICE)==1&&f.count(0,Items.GUNPOWDER)==1
                    &&f.count(0,Items.POTION)==1,"Every complete vanilla ingredient is debited exactly once");
            f.check(PotionCrafting.craft(f.p(0),f.run,healing)&&f.charges(0)==14&&f.revision(0)==2&&f.products()==2,
                    "Second craft uses saved charges without requiring another powder");
            f.check(f.inventory(1).equals(donorBefore),"Own-first leaves consenting donors' identical materials untouched");
            f.reload(0);
            f.check(f.p(0).getInventory().getItem(0).getCount()==2&&f.p(0).getInventory().getItem(0).getMaxStackSize()==64,
                    "Repeated crafted outputs merge into one bounded stack and survive the actual owner save/reload");

            f.clear();f.potion(0,0,Items.POTION,Potions.AWKWARD);f.item(0,1,Items.GLISTERING_MELON_SLICE,1);f.item(0,2,Items.GUNPOWDER,1);
            offer=PotionCrafting.quote(f.p(0),f.run,healing);
            f.check(offer.own()==1&&offer.steps()==2&&offer.powders()==0,"Owned Awkward skips the already completed wart step");
            f.check(PotionCrafting.craft(f.p(0),f.run,healing)&&f.charges(0)==12,"Intermediate potion consumes only its two remaining brewing steps");

            f.clear();f.classId(1,"theurgist");f.potion(1,0,Items.POTION,Potions.WATER);
            f.item(1,1,Items.NETHER_WART,1);f.item(1,2,Items.GUNPOWDER,1);f.item(1,3,Items.BLAZE_POWDER,1);
            f.check(!PotionCrafting.craft(f.p(1),f.run,"minecraft:strength_splash")&&f.charges(1)==0&&f.count(1,Items.BLAZE_POWDER)==1,
                    "A single powder cannot simultaneously supply Strength reagent and initial fuel");
            f.item(1,3,Items.BLAZE_POWDER,2);
            f.check(PotionCrafting.craft(f.p(1),f.run,"minecraft:strength_splash")&&f.charges(1)==17&&f.count(1,Items.BLAZE_POWDER)==0,
                    "Strength debits two powders for one reagent and one fuel refill");

            f.clear();f.healing(0,1);var guarded=f.images();var fuelBefore=SharedCraftData.get(server).fuel(f.p(0).getUUID());
            f.classId(0,"judicator");
            f.check(!PotionCrafting.craft(f.p(0),f.run,healing),"Other classes cannot use a positive recipe ID");
            f.classId(0,"theurgist");
            for(var forbidden:List.of("minecraft:luck_splash","minecraft:turtle_master_splash","minecraft:strong_turtle_master_splash","minecraft:poison_splash"))
                f.check(!PotionCrafting.craft(f.p(0),f.run,forbidden),"Unavailable or wrong-class potion is denied: "+forbidden);
            f.check(!PotionCrafting.craft(f.p(0),f.run-1,healing),"Wrong run cannot use a server recipe");
            f.unchanged(guarded,"Denied target and class requests consume nothing");
            f.check(SharedCraftData.get(server).fuel(f.p(0).getUUID()).equals(fuelBefore),"Denied target requests preserve fuel state");
            f.item(0,0,Items.GLASS_BOTTLE,1);guarded=f.images();
            f.check(!PotionCrafting.craft(f.p(0),f.run,healing),"Empty glass is not a filled water bottle");f.unchanged(guarded,"Empty bottle rejection");
            f.potion(0,0,Items.POTION,Potions.WATER);
            var custom=f.p(0).getInventory().getItem(0);
            custom.set(DataComponents.POTION_CONTENTS,custom.get(DataComponents.POTION_CONTENTS).withEffectAdded(new MobEffectInstance(MobEffects.POISON,200)));
            SharedCrafting.forget(f.p(0));guarded=f.images();
            f.check(!PotionCrafting.craft(f.p(0),f.run,healing),"Custom effect potion cannot substitute for ordinary water");f.unchanged(guarded,"Custom bottle retained exactly");

            f.clear();for(int slot=0;slot<36;slot++)f.item(0,slot,Items.DIAMOND,64);f.healing(1,1);guarded=f.images();
            f.check(PotionCrafting.prepare(f.p(0),f.run,healing)==null&&!PotionCrafting.craft(f.p(0),f.run,healing),
                    "Full crafter inventory rejects a shared recipe before material or fuel charge");
            f.unchanged(guarded,"Full output guard");f.check(f.charges(0)==12,"Full output does not debit remaining fuel");
            var nearlyFull=PotionStacks.crafted(PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING));
            nearlyFull.setCount(63);f.p(0).getInventory().setItem(0,nearlyFull);
            f.check(PotionCrafting.craft(f.p(0),f.run,healing)&&f.p(0).getInventory().getItem(0).getCount()==64&&f.charges(0)==9,
                    "One shared craft fills exactly the last compatible stack space");
            f.healing(1,1);guarded=f.images();
            f.check(!PotionCrafting.craft(f.p(0),f.run,healing)&&f.charges(0)==9,
                    "A full crafted stack plus full inventory rejects the next click without fuel debit");
            f.unchanged(guarded,"Full stack never consumes donor ingredients");

            f.clear();
            for(int owner=0;owner<2;owner++) {
                f.classId(owner,owner==0?"theurgist":"venefex");f.potion(owner,0,Items.POTION,Potions.WATER);
                f.item(owner,1,Items.NETHER_WART,1);f.item(owner,2,Items.TURTLE_HELMET,1);
                f.check(PotionCrafting.craft(f.p(owner),f.run,"minecraft:turtle_master_drinkable"),"Both approved classes can craft drinkable Turtle Master");
                f.check(ChopTravelRecovery.inventory(f.p(owner)).stream().anyMatch(s->s.is(Items.POTION)&&s.get(DataComponents.POTION_CONTENTS)!=null
                        &&s.get(DataComponents.POTION_CONTENTS).potion().map(p->p.is(Potions.TURTLE_MASTER)).orElse(false)),
                        "Mixed result is native drinkable Turtle Master");
                f.check(!PotionCrafting.craft(f.p(owner),f.run,"minecraft:turtle_master_splash"),"Neither class can craft splash Turtle Master");
            }
            f.check(f.charges(0)==7&&f.charges(1)==15,"Turtle Master charges water-to-Awkward and helmet steps only");
        }
        h.succeed();
    }

    public static void recovery(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3202)) {
            var server=f.level.getServer();var data=SharedCraftData.get(server);String healing="minecraft:healing_splash";
            f.clear();f.potion(0,0,Items.POTION,Potions.WATER);f.item(0,1,Items.NETHER_WART,1);
            f.item(1,0,Items.GLISTERING_MELON_SLICE,1);f.item(2,0,Items.GUNPOWDER,1);f.item(2,1,Items.BLAZE_POWDER,1);
            var offer=PotionCrafting.quote(f.p(0),f.run,healing);
            f.check(offer.own()==0&&offer.shared()==1&&offer.steps()==3&&offer.powders()==1,
                    "Split donors expose one shared full recipe including fuel");
            var plan=PotionCrafting.prepare(f.p(0),f.run,healing);var before=f.images();var checks=new int[1];
            f.check(plan!=null&&plan.owners().size()==3&&plan.fuel().getCompoundOrEmpty("after").getIntOr("charges",-1)==17,
                    "Multi-owner decision includes an exact fuel afterimage");
            f.check(!SharedCraftTransactions.execute(f.p(0),plan,()->{
                if(++checks[0]==2)f.consent(1,false);
                return GroupResourceConsent.canContribute(f.p(0),f.p(1),f.run)&&GroupResourceConsent.canContribute(f.p(0),f.p(2),f.run);
            }),"Consent withdrawn after reservation cancels both inventory and fuel");
            f.check(checks[0]==2&&f.charges(0)==0&&f.revision(0)==0&&data.pendingRun(f.run)==null,"Canceled decision leaves no charge or pending craft");
            f.unchanged(before,"Consent cancellation is atomic");f.consent(1,true);
            f.saveAll();plan=PotionCrafting.prepare(f.p(0),f.run,healing);f.reserve(plan);f.custody(1,plan);
            for(int owner=0;owner<3;owner++)f.reload(owner);
            for(int owner=0;owner<3;owner++)f.check(SharedCraftTransactions.reconcile(f.p(owner)),"Partial prepared crash cancels after real owner reload");
            f.unchanged(before,"Prepared restart cannot consume ingredients");f.check(f.charges(0)==0&&f.revision(0)==0,"Prepared restart cannot consume fuel");

            plan=PotionCrafting.prepare(f.p(0),f.run,healing);f.reserve(plan);for(int owner=0;owner<3;owner++)f.custody(owner,plan);
            data.commit(plan.id());f.check(data.flushVerified(),"Committed world decision and fuel are durably saved together");
            f.check(f.charges(0)==17&&f.revision(0)==1,"Fuel afterimage commits before owner inventory projection");
            var decoded=SharedCraftData.CODEC.parse(NbtOps.INSTANCE,data.image()).getOrThrow();
            f.check(decoded.pendingRun(f.run).committed()&&BrewingFuel.charges(decoded.fuel(f.p(0).getUUID()))==17,
                    "Reloaded native journal preserves the same committed decision and fuel revision");
            f.unchanged(before,"World commit alone does not partly rewrite an owner inventory");
            f.check(SharedCraftTransactions.reconcile(f.p(1))&&f.count(1,Items.GLISTERING_MELON_SLICE)==0,"First donor has one saved debit");
            for(int owner=0;owner<3;owner++)f.reload(owner);
            for(int owner=0;owner<3;owner++)f.check(SharedCraftTransactions.reconcile(f.p(owner)),"Committed potion completes after native owner reload");
            f.check(f.products()==1&&f.count(0,Items.POTION)==0&&f.count(0,Items.NETHER_WART)==0
                    &&f.count(2,Items.GUNPOWDER)==0&&f.count(2,Items.BLAZE_POWDER)==0,"Exactly one potion consumes the complete split recipe");
            f.check(f.charges(0)==17&&f.revision(0)==1&&data.pendingRun(f.run)==null,"Owner recovery and retirement do not charge brewing fuel again");
            f.check(DungeonMetricsService.resourceSequence(server,f.run)==1&&f.metrics(2).contributed().getOrDefault("minecraft:blaze_powder",0L)==1,
                    "Fuel powder is counted once as an actual donated material");
            var settled=f.images();var stale=plan;
            f.check(!SharedCraftTransactions.execute(f.p(0),stale,()->true),"Replaying settled potion decision cannot mint another potion");
            for(int owner=0;owner<3;owner++){f.reload(owner);f.check(SharedCraftTransactions.reconcile(f.p(owner)),"Settled native owner reload is idempotent");}
            f.unchanged(settled,"Settled replay preserves exact owner inventories");
            f.check(f.charges(0)==17&&f.revision(0)==1&&f.notices(1)==1&&f.notices(2)==2,"No duplicate fuel charge or donor notices on replay");
            for(int owner=0;owner<3;owner++)f.check(PlayerSaveProof.matches(PlayerSaveProof.snapshot(f.p(owner)),f.disk(owner))
                    &&f.root(owner).getStringOr("future_marker","").equals("preserved"),"Owner files retain unrelated saved fields");

            f.p(0).setHealth(0);f.check(!PotionCrafting.craft(f.p(0),f.run,healing)&&f.charges(0)==17,"Dead owner cannot spend or reset saved fuel");
            f.p(0).setHealth(20);f.clear();f.potion(0,0,Items.POTION,Potions.AWKWARD);
            f.item(0,1,Items.GLISTERING_MELON_SLICE,1);f.item(0,2,Items.GUNPOWDER,1);
            f.check(PotionCrafting.craft(f.p(0),f.run,healing)&&f.charges(0)==15&&f.revision(0)==2,
                    "Surviving charges remain usable after journal retirement, owner reload and death boundary");
        }
        h.succeed();
    }
}
