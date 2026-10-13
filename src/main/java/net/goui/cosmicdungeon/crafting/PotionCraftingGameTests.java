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
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Real owner saves exercise server recipe decisions and persistent brewing fuel. */
public final class PotionCraftingGameTests {
    private PotionCraftingGameTests() {}
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
        Fixture(GameTestHelper h,long run) { this(h,run,3,false); }
        @SuppressWarnings("unchecked") Fixture(GameTestHelper h,long run,int members,boolean allBrewers) {
            this.h=h;this.run=run;level=h.getLevel();var server=level.getServer();
            check(members>=1&&members<=6,"Native fixture is bounded to six owners");
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
            for(int i=0;i<members;i++) {
                var profile=new GameProfile(UUID.randomUUID(),"PotionTest"+i);String id=profile.id().toString();
                check(!players.containsKey(profile.id())&&!stats.containsKey(profile.id())&&!advancements.containsKey(profile.id()),"Unique native owner");
                var ownerFiles=List.of(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat"),
                        server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat_old"),
                        server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(id+".json"),
                        server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(id+".json"));
                check(ownerFiles.stream().noneMatch(Files::exists),"Fixture owner files must be new");files.addAll(ownerFiles);
                var player=create(profile);var root=new CompoundTag();
                root.putString(ClassData.KEY_CLASS_ID,allBrewers||i==0?"theurgist":"judicator");root.putString("future_marker","preserved");
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
            for(int i=0;i<members;i++) storedConsent(i,true);
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
        void clear() {
            for(var player:current){player.getInventory().clearContent();player.inventoryMenu.setCarried(ItemStack.EMPTY);PotionCursorCustody.capture(player);SharedCrafting.forget(player);}
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
            check(before.size()==current.size(),"Every fixture owner has a before image");
            for(int i=0;i<current.size();i++)check(inventory(i).equals(before.get(i)),message+" owner "+i);
        }
        List<CompoundTag> images(){var result=new ArrayList<CompoundTag>();for(int i=0;i<current.size();i++)result.add(inventory(i));return List.copyOf(result);}
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
            for(var player:owned){PotionCraftActions.forget(player);SharedCrafting.forget(player);SupplyRequests.forget(player);ClassResourceService.forget(player);player.inventoryMenu.setCarried(ItemStack.EMPTY);player.discard();}
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
            var bottle=f.p(0).getInventory().getItem(0);
            f.check(bottle.is(Items.POTION),"Metadata fixture selects the exact water bottle slot");
            bottle.set(DataComponents.CUSTOM_NAME,Component.literal("Keep this bottle's name"));
            net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA,bottle,t->t.putString("future_bottle","preserved"));
            var bottleData=bottle.get(DataComponents.CUSTOM_DATA);
            f.p(0).getInventory().getItem(1).set(DataComponents.CUSTOM_NAME,bottle.get(DataComponents.CUSTOM_NAME));
            f.p(0).getInventory().getItem(1).set(DataComponents.CUSTOM_DATA,bottleData);
            var offer=PotionCrafting.quote(f.p(0),f.run,healing);
            f.check(offer.own()==2&&offer.fuel().charges()==0&&offer.fuel().capacity()==20&&offer.steps()==3&&offer.powders()==1,
                    "Two complete own water recipes expose exact three-step fuel cost");
            var own=PotionCrafting.prepare(f.p(0),f.run,healing);
            f.check(own!=null&&own.owners().equals(List.of(f.p(0).getUUID()))&&own.hasFuel(),
                    "A complete own route is selected before any group material");
            f.check(PotionCrafting.craft(f.p(0),f.run,healing),"First native instant Healing craft succeeds");
            var crafted=ChopTravelRecovery.inventory(f.p(0)).stream().filter(s->s.is(Items.SPLASH_POTION)).findFirst().orElseThrow();
            f.check(crafted.getHoverName().getString().equals("Keep this bottle's name")&&bottleData.equals(crafted.get(DataComponents.CUSTOM_DATA)),
                    "Durable craft projects selected input bottle metadata into actual output");
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
                if(++checks[0]==2)f.storedConsent(1,false);
                return GroupResourceConsent.canContribute(f.p(0),f.p(1),f.run)&&GroupResourceConsent.canContribute(f.p(0),f.p(2),f.run);
            }),"Stored consent invalidated after reservation cancels both inventory and fuel");
            f.check(checks[0]==2&&f.charges(0)==0&&f.revision(0)==0&&data.pendingRun(f.run)==null,"Canceled decision leaves no charge or pending craft");
            f.unchanged(before,"Consent cancellation is atomic");f.storedConsent(1,true);
            f.saveAll();plan=PotionCrafting.prepare(f.p(0),f.run,healing);f.reserve(plan);f.custody(1,plan);
            data=f.reloadJournal();for(int owner=0;owner<3;owner++)f.reload(owner);
            for(int owner:new int[]{2,1,0})f.check(SharedCraftTransactions.reconcile(f.p(owner)),"Partial prepared crash cancels after real owner reload");
            f.unchanged(before,"Prepared restart cannot consume ingredients");f.check(f.charges(0)==0&&f.revision(0)==0,"Prepared restart cannot consume fuel");

            plan=PotionCrafting.prepare(f.p(0),f.run,healing);f.reserve(plan);for(int owner=0;owner<3;owner++)f.custody(owner,plan);
            data.commit(plan.id());f.check(data.flushVerified(),"Committed world decision and fuel are durably saved together");
            f.check(f.charges(0)==17&&f.revision(0)==1,"Fuel afterimage commits before owner inventory projection");
            var decoded=f.reloadJournal();data=decoded;
            f.check(decoded.pendingRun(f.run).committed()&&BrewingFuel.charges(decoded.fuel(f.p(0).getUUID()))==17,
                    "Reloaded native journal preserves the same committed decision and fuel revision");
            f.unchanged(before,"World commit alone does not partly rewrite an owner inventory");
            f.check(SharedCraftTransactions.reconcile(f.p(1))&&f.count(1,Items.GLISTERING_MELON_SLICE)==0,"First donor has one saved debit");
            data=f.reloadJournal();for(int owner=0;owner<3;owner++)f.reload(owner);
            for(int owner:new int[]{2,0,1})f.check(SharedCraftTransactions.reconcile(f.p(owner)),"Committed potion completes after native owner reload");
            f.check(f.products()==1&&f.count(0,Items.POTION)==0&&f.count(0,Items.NETHER_WART)==0
                    &&f.count(2,Items.GUNPOWDER)==0&&f.count(2,Items.BLAZE_POWDER)==0,"Exactly one potion consumes the complete split recipe");
            f.check(f.charges(0)==17&&f.revision(0)==1&&data.pendingRun(f.run)==null,"Owner recovery and retirement do not charge brewing fuel again");
            f.check(DungeonMetricsService.resourceSequence(server,f.run)==1&&f.metrics(2).contributed().getOrDefault("minecraft:blaze_powder",0L)==1,
                    "Fuel powder is counted once as an actual donated material");
            var settled=f.images();var stale=plan;
            f.check(!SharedCraftTransactions.execute(f.p(0),stale,()->true),"Replaying settled potion decision cannot mint another potion");
            data=f.reloadJournal();for(int owner:new int[]{2,1,0}){f.reload(owner);f.check(SharedCraftTransactions.reconcile(f.p(owner)),"Settled native owner reload is idempotent");}
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

    public static void paletteCursor(GameTestHelper h) {
        ingredientQuotes(h);
        try(var f=new Fixture(h,Long.MAX_VALUE-3203)) {
            String healing="minecraft:healing_splash";f.clear();f.healing(0,2);f.healing(1,2);var donor=f.inventory(1);
            var quotes=PotionCrafting.view(f.p(0),1);
            f.check(quotes.classId().equals("theurgist")&&quotes.entries().stream().noneMatch(e->e.targetId().equals("minecraft:poison_splash")
                    ||e.targetId().equals("minecraft:turtle_master_splash")||e.targetId().equals("minecraft:luck_splash")),"Palette exposes only permitted positive/mixed-drinkable variants");
            var raw=io.netty.buffer.Unpooled.buffer();
            try {
                var wire=new net.minecraft.network.RegistryFriendlyByteBuf(raw,f.level.registryAccess());
                net.goui.cosmicdungeon.network.PotionCraftPayloads.View.STREAM_CODEC.encode(wire,quotes);
                var decoded=net.goui.cosmicdungeon.network.PotionCraftPayloads.View.STREAM_CODEC.decode(wire);
                f.check(decoded.entries().size()==quotes.entries().size()&&decoded.run()==f.run&&decoded.charges()==0,"Native registry quote wire round trip");
                f.check(wire.readableBytes()==0,"Native quote decoder consumes exactly the bounded payload");
                for(int i=0;i<quotes.entries().size();i++) {
                    var expected=quotes.entries().get(i);var actual=decoded.entries().get(i);
                    f.check(ItemStack.matches(expected.output(),actual.output())&&expected.targetId().equals(actual.targetId())
                            &&expected.own()==actual.own()&&expected.shared()==actual.shared()&&expected.steps()==actual.steps()
                            &&expected.powders()==actual.powders()&&expected.materials().equals(actual.materials()),
                            "Native potion components and exact selected ingredient/fuel rows survive the wire");
                }
            } finally {raw.release();}
            f.check(PotionCrafting.craft(f.p(0),f.run,healing,false)&&PotionCrafting.craft(f.p(0),f.run,healing,false),
                    "Repeated one-click cursor crafts succeed while holding the matching potion");
            f.check(f.p(0).inventoryMenu.getCarried().getCount()==2&&f.products()==0&&f.charges(0)==14
                    &&f.inventory(1).equals(donor),"Two cursor outputs, exact own debits/fuel, donors untouched");
            f.reload(0);f.check(SharedCraftTransactions.reconcile(f.p(0)),"Actual owner reload restores cursor custody");
            f.check(f.p(0).inventoryMenu.getCarried().getCount()==2&&f.products()==0,"Cursor is neither lost nor duplicated after reconnect");
            f.p(0).inventoryMenu.clicked(36,0,net.minecraft.world.inventory.ClickType.PICKUP,f.p(0));
            f.check(f.p(0).inventoryMenu.getCarried().isEmpty()&&f.products()==2,"Vanilla inventory placement accepts crafted stack");
            f.saveAll();f.reload(0);f.check(SharedCraftTransactions.reconcile(f.p(0))&&f.p(0).inventoryMenu.getCarried().isEmpty()
                    &&f.products()==2&&!f.root(0).contains(PotionCursorCustody.KEY),"Moving cursor into inventory clears saved custody in paired native save");

            f.clear();f.healing(1,2);f.p(0).inventoryMenu.setCarried(new ItemStack(Items.DIAMOND));
            var before=f.images();int charges=f.charges(0);
            f.check(!PotionCrafting.craft(f.p(0),f.run,healing,false)&&f.charges(0)==charges,"Mismatched cursor cannot debit any owner");
            f.unchanged(before,"Mismatched cursor is atomic");
            var full=PotionStacks.crafted(PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING));full.setCount(64);
            f.p(0).inventoryMenu.setCarried(full);
            f.check(!PotionCrafting.craft(f.p(0),f.run,healing,false)&&f.charges(0)==charges,"Full cursor cannot debit ingredients/fuel");
            f.unchanged(before,"Full cursor is atomic");
            f.p(0).inventoryMenu.setCarried(ItemStack.EMPTY);
            for(int slot=0;slot<36;slot++)f.item(0,slot,Items.DIAMOND,64);
            f.check(PotionCrafting.craft(f.p(0),f.run,healing,false)&&f.p(0).inventoryMenu.getCarried().getCount()==1,
                    "Cursor delivery works with full storage and consenting donor ingredients");
            f.p(0).inventoryMenu.setCarried(ItemStack.EMPTY);before=f.images();charges=f.charges(0);
            f.check(!PotionCrafting.craft(f.p(0),f.run,healing,true)&&f.charges(0)==charges,"Shift delivery rejects completely full storage");
            f.unchanged(before,"Full Shift destination is atomic");
            f.p(0).getInventory().setItem(4,ItemStack.EMPTY);
            f.p(0).getInventory().setItem(9,PotionStacks.crafted(PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING)));
            f.check(PotionCrafting.craft(f.p(0),f.run,healing,true)&&f.p(0).getInventory().getItem(4).getCount()==1
                    &&f.p(0).getInventory().getItem(4).is(Items.SPLASH_POTION)&&f.p(0).getInventory().getItem(9).getCount()==1,
                    "Shift makes exactly one and prefers an empty hotbar slot over a main-inventory merge");
        }
        h.succeed();
    }

    private static String material(Item item){return PotionRecipeSelection.itemKey(item);}
    private static String input(Item container,net.minecraft.core.Holder<Potion> potion) {
        return PotionRecipeSelection.stock(List.of(PotionContents.createItemStack(container,potion))).keySet().iterator().next();
    }
    private static net.goui.cosmicdungeon.network.PotionCraftPayloads.Entry selected(Fixture f,String id,int own,int shared,int steps,int powders,Map<String,Integer> needed) {
        var entry=PotionCrafting.view(f.p(0),1).entries().stream().filter(e->e.targetId().equals(id)).findFirst().orElseThrow();
        f.check(entry.own()==own&&entry.shared()==shared&&entry.steps()==steps&&entry.powders()==powders,"Native selected route capacity and fuel for "+id);
        var quoted=new TreeMap<String,Integer>();
        entry.materials().forEach(row->quoted.put(row.key(),row.needed()));
        f.check(quoted.equals(needed),"Native selected input/reagent quantities for "+id);
        var ownStock=PotionRecipeSelection.stock(SharedCrafting.inventory(f.p(0),true));var stocks=new ArrayList<Map<String,Integer>>();
        for(var owner:SharedCrafting.participants(f.p(0),f.record))stocks.add(PotionRecipeSelection.stock(SharedCrafting.inventory(owner,true)));
        var pooled=PotionRecipeSelection.combine(stocks);
        for(var row:entry.materials())f.check(row.own()==ownStock.getOrDefault(row.key(),0)&&row.shared()==pooled.getOrDefault(row.key(),0),
                "Native tooltip availability matches eligible snapshot stock");
        var plan=PotionCrafting.prepare(f.p(0),f.run,id,true);
        f.check(plan!=null,"Displayed affordable selected route has a real execution plan");
        var debits=new TreeMap<String,Integer>();
        for(var owner:plan.owners())for(var debit:plan.debits(owner))debits.merge(debit.ingredient(),debit.count(),Integer::sum);
        f.check(debits.equals(quoted),"Every displayed quantity equals one actual native transaction debit");
        return entry;
    }
    private static void ingredientQuotes(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3213)) {
            String healing="minecraft:healing_splash";f.clear();f.healing(0,1);
            var base=Map.of(input(Items.POTION,Potions.WATER),1,material(Items.NETHER_WART),1,material(Items.GLISTERING_MELON_SLICE),1,
                    material(Items.GUNPOWDER),1,material(Items.BLAZE_POWDER),1);
            selected(f,healing,1,1,3,1,base);f.item(0,7,Items.GLOWSTONE_DUST,1);
            var strong=new TreeMap<>(base);strong.put(material(Items.GLOWSTONE_DUST),1);
            selected(f,"minecraft:strong_healing_splash",1,1,4,1,strong);
            f.clear();f.potion(0,0,Items.POTION,Potions.WATER);f.item(0,1,Items.NETHER_WART,1);
            f.item(0,2,Items.BLAZE_POWDER,2);f.item(0,3,Items.REDSTONE,1);f.item(0,4,Items.GUNPOWDER,1);
            selected(f,"minecraft:long_strength_splash",1,1,4,1,Map.of(input(Items.POTION,Potions.WATER),1,
                    material(Items.NETHER_WART),1,material(Items.BLAZE_POWDER),2,material(Items.REDSTONE),1,material(Items.GUNPOWDER),1));
            f.clear();f.potion(0,0,Items.POTION,Potions.WATER);f.item(0,1,Items.NETHER_WART,1);f.item(0,2,Items.GOLDEN_CARROT,1);
            f.item(0,3,Items.FERMENTED_SPIDER_EYE,1);f.item(0,4,Items.REDSTONE,1);f.item(0,5,Items.GUNPOWDER,1);f.item(0,6,Items.BLAZE_POWDER,1);
            selected(f,"minecraft:long_invisibility_splash",1,1,5,1,Map.of(input(Items.POTION,Potions.WATER),1,material(Items.NETHER_WART),1,
                    material(Items.GOLDEN_CARROT),1,material(Items.FERMENTED_SPIDER_EYE),1,material(Items.REDSTONE),1,material(Items.GUNPOWDER),1,material(Items.BLAZE_POWDER),1));
            f.clear();f.healing(0,1);f.potion(1,0,Items.POTION,Potions.HEALING);f.item(1,1,Items.GUNPOWDER,1);
            selected(f,healing,1,2,3,1,base);
            f.check(PotionCrafting.prepare(f.p(0),f.run,healing,true).owners().size()==1,"Own route is preferred over the shorter donor input");
            f.clear();f.item(0,0,Items.GUNPOWDER,1);f.item(0,1,Items.BLAZE_POWDER,1);f.potion(1,0,Items.POTION,Potions.HEALING);
            selected(f,healing,0,1,1,1,Map.of(input(Items.POTION,Potions.HEALING),1,material(Items.GUNPOWDER),1,material(Items.BLAZE_POWDER),1));
            f.clear();var crafted=PotionStacks.crafted(PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING));
            f.p(0).getInventory().setItem(0,crafted);SharedCrafting.forget(f.p(0));f.item(0,1,Items.GLOWSTONE_DUST,1);f.item(0,2,Items.BLAZE_POWDER,1);
            selected(f,"minecraft:strong_healing_splash",1,1,1,1,Map.of(input(Items.SPLASH_POTION,Potions.HEALING),1,
                    material(Items.GLOWSTONE_DUST),1,material(Items.BLAZE_POWDER),1));
            f.clear();f.healing(0,1);f.p(0).getInventory().setItem(6,ItemStack.EMPTY);SharedCrafting.forget(f.p(0));
            var missing=PotionCrafting.view(f.p(0),1).entries().stream().filter(e->e.targetId().equals(healing)).findFirst().orElseThrow();
            f.check(missing.shared()==0&&missing.steps()==0&&missing.powders()==0&&missing.materials().isEmpty(),"Missing fuel does not invent an affordable ingredient route");
            f.item(0,6,Items.BLAZE_POWDER,1);
            f.check(PotionCrafting.craft(f.p(0),f.run,healing)&&f.charges(0)==17,"Actual native craft preserves seventeen fuel charges");
            f.clear();f.potion(0,0,Items.POTION,Potions.AWKWARD);f.item(0,1,Items.GLISTERING_MELON_SLICE,1);f.item(0,2,Items.GUNPOWDER,1);
            selected(f,healing,1,1,2,0,Map.of(input(Items.POTION,Potions.AWKWARD),1,material(Items.GLISTERING_MELON_SLICE),1,material(Items.GUNPOWDER),1));
            f.check(PotionCrafting.craft(f.p(0),f.run,healing)&&f.charges(0)==15,"Displayed intermediate route debits two stored charges without a new powder");
            quoteWireBounds(f);
        }
    }
    private static int quoteBytes(Fixture f,net.goui.cosmicdungeon.network.PotionCraftPayloads.View view,boolean legacy) {
        var raw=io.netty.buffer.Unpooled.buffer();
        try {
            var b=new net.minecraft.network.RegistryFriendlyByteBuf(raw,f.level.registryAccess());
            if(!legacy)net.goui.cosmicdungeon.network.PotionCraftPayloads.View.STREAM_CODEC.encode(b,view);
            else {
                b.writeVarLong(view.run());b.writeVarLong(view.revision());b.writeUtf(view.classId(),16);
                b.writeVarInt(view.charges());b.writeVarInt(view.perPowder());b.writeVarInt(view.entries().size());
                for(var e:view.entries()) {
                    b.writeUtf(e.targetId(),128);ItemStack.STREAM_CODEC.encode(b,e.output());
                    b.writeVarInt(e.own());b.writeVarInt(e.shared());b.writeVarInt(e.steps());b.writeVarInt(e.powders());
                }
            }
            return b.readableBytes();
        }finally{raw.release();}
    }
    private static void quoteWireBounds(Fixture f) {
        var catalog=PotionCrafting.catalog(f.p(0));var planner=new PotionRecipeSelection(catalog);var water=new HashMap<String,Integer>();
        water.put(input(Items.POTION,Potions.WATER),4096);water.put(material(Items.BLAZE_POWDER),4096);
        for(var target:catalog.targets())for(var route:catalog.routes(target))
            route.ingredients().forEach(item->water.put(material(item),4096));
        for(var role:List.of("theurgist","venefex")) {
            var entries=catalog.targets().stream().filter(target->target.classes().contains(role))
                    .map(target->PotionCrafting.entry(target,planner,water,water,0)).toList();
            var view=new net.goui.cosmicdungeon.network.PotionCraftPayloads.View(f.run,1,role,0,20,entries);
            int before=quoteBytes(f,view,true),after=quoteBytes(f,view,false);
            int maximum=entries.stream().mapToInt(e->e.materials().size()).max().orElse(0);
            f.check(maximum<=net.goui.cosmicdungeon.network.PotionCraftPayloads.MAX_MATERIALS&&after<32768,"Vanilla selected route payload stays well within its row and byte bounds");
            System.out.println("BATCH23_ROUTE_QUOTE_BYTES role="+role+" entries="+entries.size()+" legacy="+before+" ingredients="+after+" max_materials="+maximum);
        }
        for(int oversized:new int[]{-1,Integer.MAX_VALUE}) {
            var raw=io.netty.buffer.Unpooled.buffer();
            try {
                var b=new net.minecraft.network.RegistryFriendlyByteBuf(raw,f.level.registryAccess());
                b.writeVarLong(f.run);b.writeVarLong(1);b.writeUtf("theurgist",16);b.writeVarInt(0);b.writeVarInt(20);b.writeVarInt(1);
                b.writeUtf("minecraft:healing_splash",128);ItemStack.STREAM_CODEC.encode(b,PotionContents.createItemStack(Items.SPLASH_POTION,Potions.HEALING));
                b.writeVarInt(1);b.writeVarInt(1);b.writeVarInt(1);b.writeVarInt(0);b.writeVarInt(oversized);
                boolean rejected=false;
                try{net.goui.cosmicdungeon.network.PotionCraftPayloads.View.STREAM_CODEC.decode(b);}
                catch(IllegalArgumentException ex){rejected=true;}
                f.check(rejected,"Malformed material count rejected before row allocation");
            }finally{raw.release();}
        }
    }

    private static void cursorReservationOrdering(GameTestHelper h){
        for(int scenario=0;scenario<3;scenario++)try(var f=new Fixture(h,Long.MAX_VALUE-3240-scenario)){
            f.clear();f.healing(0,2);
            f.check(PotionCrafting.craft(f.p(0),f.run,"minecraft:healing_splash",false),"Ordering fixture has one paid saved cursor before the next craft");
            var plan=PotionCrafting.prepare(f.p(0),f.run,"minecraft:healing_splash",false);f.reserve(plan);f.custody(0,plan);
            f.reload(0);
            var root=f.root(0).copy();var bad=root.getCompoundOrEmpty(SharedCraftTransactions.CUSTODY).copy();
            if(scenario==0)bad.putInt("schema",99);
            else if(scenario==1)bad.putString("owner",UUID.randomUUID().toString());
            else bad.putString("id",UUID.randomUUID().toString());
            root.put(SharedCraftTransactions.CUSTODY,bad);
            // The supported cursor reader accepts numeric widths. Recapture would canonicalize this Short.
            // Preserving it proves recovery rejected the reservation before transient restore/freeze too.
            root.getCompoundOrEmpty(PotionCursorCustody.KEY).putShort("schema",(short)1);
            f.p(0).getPersistentData().put(ClassData.ROOT_TAG,root);f.check(PlayerSaveProof.save(f.p(0)),"Valid cursor and unsupported reservation are saved together");
            var disk=f.disk(0);var inventory=f.inventory(0);var journal=f.reloadJournal().image();f.reload(0);
            f.check(f.p(0).inventoryMenu.getCarried().isEmpty(),"Unrestored native cursor begins empty after owner reload");
            f.disconnects.expect(f.p(0));
            f.check(!SharedCraftTransactions.reconcile(f.p(0))&&SharedCraftTransactions.blocked(f.p(0))&&f.disconnects.recoveryHold(f.p(0)),
                    "Future/foreign compound reservation holds before restoring its otherwise valid saved cursor");
            f.check(f.p(0).inventoryMenu.getCarried().isEmpty()&&f.root(0).equals(root)&&f.disk(0).equals(disk)
                    &&f.inventory(0).equals(inventory)&&SharedCraftData.get(f.level.getServer()).image().equals(journal),
                    "Ordering rejection preserves exact cursor proof widths, native owner and world before any restore or outcome mutation");
        }
    }
    public static void paletteRecovery(GameTestHelper h) {
        cursorReservationOrdering(h);
        try(var f=new Fixture(h,Long.MAX_VALUE-3204)) {
            var server=f.level.getServer();var data=SharedCraftData.get(server);String healing="minecraft:healing_splash";
            f.clear();f.healing(1,2);f.saveAll();
            var first=PotionCrafting.prepare(f.p(0),f.run,healing,false);
            f.check(first!=null&&first.cursor()&&first.image().getIntOr("schema",0)==3,"New cursor schema leaves legacy schema1/2 separate");
            f.reserve(first);for(var id:first.owners())f.custody(f.current.stream().map(ServerPlayer::getUUID).toList().indexOf(id),first);
            data=f.reloadJournal();for(int i=0;i<3;i++)f.reload(i);
            for(int i:new int[]{2,1,0})f.check(SharedCraftTransactions.reconcile(f.p(i)),"Prepared cursor crash cancels without output");
            f.check(f.p(0).inventoryMenu.getCarried().isEmpty()&&f.charges(0)==0&&f.count(1,Items.POTION)==2,"Prepared recipe preserves full materials/fuel");

            var plan=PotionCrafting.prepare(f.p(0),f.run,healing,false);f.reserve(plan);
            for(var id:plan.owners())f.custody(f.current.stream().map(ServerPlayer::getUUID).toList().indexOf(id),plan);
            data.commit(plan.id());f.check(data.flushVerified(),"Cursor world decision saved");
            data=f.reloadJournal();for(int i=0;i<3;i++)f.reload(i);
            for(int i:new int[]{2,1,0})f.check(SharedCraftTransactions.reconcile(f.p(i)),"Committed cursor crash projects exact owner images");
            f.check(f.p(0).inventoryMenu.getCarried().getCount()==1&&f.charges(0)==17&&f.count(1,Items.POTION)==1,"One committed output and one debit after recovery");
            f.reload(0);f.check(SharedCraftTransactions.reconcile(f.p(0))&&f.p(0).inventoryMenu.getCarried().getCount()==1,"Saved cursor receipt replay is idempotent");
            f.check(!SharedCraftTransactions.execute(f.p(0),plan,()->true),"Replayed prior decision cannot duplicate cursor output");

            var next=PotionCrafting.prepare(f.p(0),f.run,healing,false);PotionCursorCustody.track(f.p(0));f.reserve(next);
            for(var id:next.owners())f.custody(f.current.stream().map(ServerPlayer::getUUID).toList().indexOf(id),next);
            data.commit(next.id());f.check(data.flushVerified(),"Stack extension decision saved");
            // Simulate disconnect after world commit: prevent native close from moving the reserved old cursor.
            PotionCursorCustody.freeze(f.p(0));f.p(0).closeContainer();f.saveAll();f.reload(0);
            data=f.reloadJournal();
            var beforeRecipientAck=DurableRecoveryGameTestSupport.disk(server,SharedCraftData.class,SharedCraftData.ID);
            // Owner receipt is saved, but another donor is still unacknowledged when the owner reconnects.
            f.check(SharedCraftTransactions.reconcile(f.p(0)),"Recipient extension saved before final donor settlement");
            data=DurableRecoveryGameTestSupport.restoreCut(server,SharedCraftData.class,SharedCraftData.ID,beforeRecipientAck);
            f.reload(0);f.check(SharedCraftTransactions.reconcile(f.p(0))&&f.p(0).inventoryMenu.getCarried().getCount()==2,
                    "Partial acknowledgement never applies cursor extension twice");
            data=f.reloadJournal();for(int i:new int[]{2,1}){f.reload(i);f.check(SharedCraftTransactions.reconcile(f.p(i)),"Remaining donor settles once");}
            f.check(f.charges(0)==14&&data.pendingRun(f.run)==null,"Two total crafts retain correct charges and retire journal");
            f.p(0).closeContainer();f.saveAll();f.reload(0);
            f.check(SharedCraftTransactions.reconcile(f.p(0))&&f.p(0).inventoryMenu.getCarried().isEmpty()&&f.products()==2,
                    "Normal inventory close returns stack once and clears cursor recovery");
        }
        h.succeed();
    }
    private static net.goui.cosmicdungeon.network.PotionCraftPayloads.View sent(Fixture f,int owner) {
        return f.packets.get(f.p(owner).getUUID()).stream()
                .filter(packet->packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket)
                .map(packet->((net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket)packet).payload())
                .filter(payload->payload instanceof net.goui.cosmicdungeon.network.PotionCraftPayloads.View)
                .map(payload->(net.goui.cosmicdungeon.network.PotionCraftPayloads.View)payload).reduce((a,b)->b).orElseThrow();
    }
    public static void paletteActions(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3205)) {
            String healing="minecraft:healing_splash";f.clear();f.healing(1,3);
            var blind=new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,1,healing,false);
            PotionCraftActions.action(f.p(0),blind);
            f.check(f.p(0).inventoryMenu.getCarried().isEmpty()&&f.charges(0)==0,"Unopened action has no authority");
            PotionCraftActions.request(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(true));
            var quote=sent(f,0);
            f.check(quote.entries().stream().filter(e->e.targetId().equals(healing)).anyMatch(e->e.own()==0&&e.shared()==3),"Shared count is authoritative");
            f.storedConsent(1,false);
            PotionCraftActions.action(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,quote.revision(),healing,false));
            f.check(f.count(1,Items.POTION)==3&&f.p(0).inventoryMenu.getCarried().isEmpty()&&sent(f,0).revision()>quote.revision(),
                    "Stored-state fault rejects formerly affordable click and acknowledges it");
            // A new object models reconnect: request generation belongs to this connection, not saved class data.
            f.saveAll();f.reload(0);f.storedConsent(1,true);
            PotionCraftActions.request(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(true));quote=sent(f,0);
            var action=new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,quote.revision(),healing,false);
            PotionCraftActions.action(f.p(0),action);PotionCraftActions.action(f.p(0),action);
            f.check(f.p(0).inventoryMenu.getCarried().getCount()==1&&f.charges(0)==17&&f.count(1,Items.POTION)==2,
                    "Exact generation permits one transaction; duplicate rapid packet spends nothing");
            PotionCraftActions.request(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(false));
            PotionCraftActions.request(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(true));
            f.check(sent(f,0).revision()>action.revision(),"Close/reopen never reuses old action generation");
            PotionCraftActions.action(f.p(0),action);
            f.check(f.p(0).inventoryMenu.getCarried().getCount()==1,"Old closed-session click cannot replay");
            var beforeDimension=sent(f,0);
            PotionCraftActions.invalidate(f.p(0)); // Same connected player crossing a dimension boundary.
            PotionCraftActions.request(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(true));
            f.check(sent(f,0).revision()>beforeDimension.revision(),"Dimension change never resets a session generation");
            var original=f.p(0);f.saveAll();f.reload(0);PotionCraftActions.transfer(original,f.p(0));
            f.check(SharedCraftTransactions.reconcile(f.p(0)),"Replacement owner cursor restored before new palette");
            PotionCraftActions.request(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(true));
            f.check(sent(f,0).revision()>beforeDimension.revision(),"Player replacement inherits and invalidates the same connection's generation");
            PotionCraftActions.action(f.p(0),action);
            f.check(f.p(0).inventoryMenu.getCarried().getCount()==1,"Old pre-replacement packet cannot replay");
            f.classId(0,"venefex");var view=PotionCrafting.view(f.p(0),100);
            f.check(view.entries().stream().anyMatch(e->e.targetId().equals("minecraft:poison_splash"))
                    &&view.entries().stream().noneMatch(e->e.targetId().equals(healing)),"Venefex palette is negative recipes");
            f.classId(0,"judicator");f.check(PotionCrafting.view(f.p(0),101).run()==0,"Other class cannot keep an open brewer palette");
        }
        h.succeed();
    }


    private static void unsupportedCursorProofs(GameTestHelper h){
        for(int scenario=0;scenario<3;scenario++)try(var f=new Fixture(h,Long.MAX_VALUE-3230-scenario)){
            f.clear();f.healing(0,1);
            f.check(PotionCrafting.craft(f.p(0),f.run,"minecraft:healing_splash",false),"Cursor guard fixture has one legitimate paid output");
            f.reload(0); // Do not track/capture the new object's corrupt record before the recovery entry point.
            var root=f.root(0).copy();var saved=root.getCompoundOrEmpty(PotionCursorCustody.KEY).copy();
            if(scenario==0)saved.putInt("schema",99);
            else if(scenario==1)saved.putString("owner",UUID.randomUUID().toString());
            if(scenario==2)root.putString(PotionCursorCustody.KEY,"preserve unsupported cursor");
            else root.put(PotionCursorCustody.KEY,saved);
            f.p(0).getPersistentData().put(ClassData.ROOT_TAG,root);f.check(PlayerSaveProof.save(f.p(0)),"Unsupported cursor image is preserved in native owner file");
            var disk=f.disk(0);var inventory=f.inventory(0);var journal=f.reloadJournal().image();f.reload(0);
            f.disconnects.expect(f.p(0));
            f.check(!SharedCraftTransactions.reconcile(f.p(0))&&SharedCraftTransactions.blocked(f.p(0))&&f.disconnects.recoveryHold(f.p(0)),
                    "Future, foreign or non-compound potion cursor proof holds without guessing an output");
            f.check(f.root(0).equals(root)&&f.inventory(0).equals(inventory)&&f.p(0).inventoryMenu.getCarried().isEmpty()
                    &&f.disk(0).equals(disk)&&SharedCraftData.get(f.level.getServer()).image().equals(journal),
                    "Unsupported cursor leaves original disk proof, inventory, cursor and fuel unchanged");
        }
    }
    public static void paletteLifecycle(GameTestHelper h) {
        unsupportedCursorProofs(h);
        try(var f=new Fixture(h,Long.MAX_VALUE-3206)) {
            String healing="minecraft:healing_splash";f.clear();f.healing(0,1);
            f.check(PotionCrafting.craft(f.p(0),f.run,healing,false),"Lifecycle fixture has one paid cursor potion");
            var area=new net.minecraft.world.phys.AABB(f.origin,f.origin).inflate(8);
            var drops=f.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).stream().map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            var original=f.p(0);
            SharedCraftEvents.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(original));
            f.check(PlayerSaveProof.save(original),"Logout persists paired cursor custody before native player removal");
            original.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_WITH_PLAYER);
            f.check(f.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).stream().allMatch(item->drops.contains(item.getUUID())),
                    "Native logout/removal cannot drop an already saved cursor item");
            f.reload(0);f.check(SharedCraftTransactions.reconcile(f.p(0))&&f.p(0).inventoryMenu.getCarried().getCount()==1,
                    "One paid cursor item restored after real removal");
            // openMenu does not close an already-current inventoryMenu: preserve that otherwise-hidden cursor.
            f.p(0).containerMenu=net.minecraft.world.inventory.ChestMenu.threeRows(42,f.p(0).getInventory());
            f.check(PlayerSaveProof.save(f.p(0)),"Owner save with another native container open");
            f.reload(0);f.check(SharedCraftTransactions.reconcile(f.p(0))&&f.p(0).inventoryMenu.getCarried().getCount()==1,
                    "Opening another container cannot erase the hidden inventory cursor record");
            f.p(0).containerMenu=net.minecraft.world.inventory.ChestMenu.threeRows(43,f.p(0).getInventory());
            PotionCursorCustody.closeBeforeDeath(f.p(0));
            f.check(f.p(0).inventoryMenu.getCarried().isEmpty()&&f.products()==1&&!f.root(0).contains(PotionCursorCustody.KEY),
                    "Death settles hidden inventory cursor once before existing loot rules");
            // Move the one existing potion to cursor, save, then follow native keep-inventory removal/clone ordering.
            f.p(0).inventoryMenu.clicked(36,0,net.minecraft.world.inventory.ClickType.PICKUP,f.p(0));
            f.check(f.p(0).inventoryMenu.getCarried().getCount()==1,"Native menu lifts the same potion");
            f.check(PlayerSaveProof.save(f.p(0)),"Cursor image saved before non-death replacement");
            original=f.p(0);original.remove(net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION);
            f.check(original.inventoryMenu.getCarried().isEmpty(),"Native changed-dimension removal settles old cursor");
            var replacement=f.create(original.getGameProfile());
            var clone=new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(replacement,original,false);
            SharedCraftEvents.clone(clone);
            net.goui.cosmicdungeon.playerclass.api.ClassCloneEvents.onPlayerClone(clone);
            f.check(!replacement.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).contains(PotionCursorCustody.KEY)
                    &&f.products()==1,"Clone cannot copy the stale saved cursor after native settlement");
        }
        h.succeed();
    }

    public static void integrationParties(GameTestHelper h) {
        try(var first=new Fixture(h,Long.MAX_VALUE-3217)) {
            first.clear();first.healing(1,2);
            String healing="minecraft:healing_splash";
            first.check(PotionCrafting.craft(first.p(0),first.run,healing,false),"First party commits its own shared cursor craft");
            var inventory=first.images();int charges=first.charges(0);
            long sequence=DungeonMetricsService.resourceSequence(first.level.getServer(),first.run);
            try(var other=new Fixture(h,Long.MAX_VALUE-3218)) {
                other.clear();
                other.check(PotionCrafting.quote(other.p(0),other.run,healing).shared()==0,
                        "Simultaneous party cannot quote another party's available donor ingredients");
                other.check(!PotionCrafting.craft(other.p(0),first.run,healing),"Foreign run ID cannot spend another party's stock");
                other.healing(1,1);other.storedConsent(1,false);var before=other.images();
                other.check(!PotionCrafting.craft(other.p(0),other.run,healing),"Own party's stored donor refusal remains authoritative");
                other.unchanged(before,"Rejected foreign/withdrawn donation leaves every owner unchanged");
                other.storedConsent(1,true);other.classId(0,"venefex");
                other.check(!PotionCrafting.craft(other.p(0),other.run,healing),"Class change rejects an old positive recipe");
                other.classId(0,"theurgist");
                other.check(PotionCrafting.craft(other.p(0),other.run,healing),"Second party commits only its own consenting donor recipe");
                other.check(other.products()==1&&other.charges(0)==17
                        &&DungeonMetricsService.resourceSequence(other.level.getServer(),other.run)==1,
                        "Second run has independent output, fuel and transaction sequence");
                first.unchanged(inventory,"Parallel party cannot mutate first party inventory");
                first.check(first.charges(0)==charges&&first.p(0).inventoryMenu.getCarried().getCount()==1
                        &&DungeonMetricsService.resourceSequence(first.level.getServer(),first.run)==sequence,
                        "Parallel party cannot alter first cursor, fuel or metrics");
            }
            first.p(0).closeContainer();first.saveAll();first.reload(0);
            first.check(SharedCraftTransactions.reconcile(first.p(0))&&first.products()==1
                    &&first.p(0).inventoryMenu.getCarried().isEmpty(),"First party's cursor survives settlement/reload once");
        }
        h.succeed();
    }

    public static void integrationFinalization(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3219)) {
            var server=f.level.getServer();String healing="minecraft:healing_splash";
            f.clear();f.healing(1,1);f.saveAll();
            var plan=PotionCrafting.prepare(f.p(0),f.run,healing,false);f.check(plan!=null,"Integration decision prepared");
            PotionCursorCustody.track(f.p(0));f.reserve(plan);
            for(var id:plan.owners())f.custody(f.current.stream().map(ServerPlayer::getUUID).toList().indexOf(id),plan);
            var data=SharedCraftData.get(server);data.commit(plan.id());f.check(data.flushVerified(),"Decision and fuel durable before disconnect");
            var donor=f.p(1);f.players.remove(donor.getUUID());f.online.remove(donor);
            try {
                f.check(!SharedCraftTransactions.readyForCleanup(server,f.record.orderedPlayers()),
                        "Run cleanup waits for committed offline donor instead of dropping custody");
                f.check(data.pendingRun(f.run)!=null&&DungeonMetricsService.resourceSequence(server,f.run)==0,
                        "Unsettled donor cannot prematurely finalize resource metrics");
            } finally {f.players.put(donor.getUUID(),donor);f.online.add(donor);}
            f.reload(1);
            f.check(SharedCraftTransactions.readyForCleanup(server,f.record.orderedPlayers()),"Reconnect settles remaining donor before run cleanup");
            f.check(data.pendingRun(f.run)==null&&f.charges(0)==17&&f.p(0).inventoryMenu.getCarried().getCount()==1
                    &&DungeonMetricsService.resourceSequence(server,f.run)==1,"Exactly one output/fuel/metrics result crosses disconnect boundary");
            f.runs.put(f.run,f.record.withState(DungeonRunState.RESETTING,DungeonResetReason.COMPLETED));
            var settled=f.images();
            f.check(!PotionCrafting.craft(f.p(0),f.run,healing,false),"Resetting run denies new craft before finalization");
            f.unchanged(settled,"Denied post-run craft retains all settled inventory");
            // Archive real runtime counters in an isolated store; do not modify shared test-world lifetime totals.
            var completed=DungeonMetricsService.snapshot(server,f.record,"COMPLETED");
            var directory=Files.createTempDirectory("cosmic-batch17-finalization-");
            try {
                var archive=new RunMetricsStore(directory,completed.worldId());archive.finish(completed);
                var totals=archive.totals(f.p(0).getUUID());
                var resumed=new RunMetricsStore(directory);resumed.finish(completed);
                f.check(resumed.read(f.run).equals(completed)&&resumed.totals(f.p(0).getUUID()).equals(totals)
                        &&totals.runs()==1&&totals.stats().used().getOrDefault("minecraft:blaze_powder",0L)==1,
                        "Real resource counters finalize and restart without duplicate totals");
            } finally {try(var paths=Files.walk(directory)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
        } catch(IOException ex){throw new IllegalStateException(ex);}
        h.succeed();
    }

    public static void integrationSync(GameTestHelper h) {
        nativeMemo(h);
        sixPlayerCosts(h);
        sixPlayerQueue(h);
        sixPlayerFairness(h);
        sixPlayerContention(h);
        freshStockGuard(h);
        try(var f=new Fixture(h,Long.MAX_VALUE-3220)) {
            f.clear();f.healing(1,3);var player=f.p(0);
            PotionCraftActions.request(player,new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(true));
            var quote=sent(f,0);var buffer=io.netty.buffer.Unpooled.buffer();int bytes;
            try {var wire=new net.minecraft.network.RegistryFriendlyByteBuf(buffer,f.level.registryAccess());
                net.goui.cosmicdungeon.network.PotionCraftPayloads.View.STREAM_CODEC.encode(wire,quote);bytes=buffer.readableBytes();
            } finally {buffer.release();}
            @SuppressWarnings("unchecked") var sessions=(Map<ServerPlayer,?>)Fixture.field(PotionCraftActions.class,"SESSIONS",null);
            var session=sessions.get(player);var sync=Arrays.stream(PotionCraftActions.class.getDeclaredMethods())
                    .filter(m->m.getName().equals("sync")).findFirst().orElseThrow();sync.setAccessible(true);
            int packets=f.packets.get(player.getUUID()).size();long begin=System.nanoTime();
            for(int i=0;i<200;i++)sync.invoke(null,player,session,false);
            long unchangedNanos=System.nanoTime()-begin;
            f.check(f.packets.get(player.getUUID()).size()==packets,"Two hundred unchanged periodic quotes send no redundant packets");
            var action=new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,quote.revision(),"minecraft:healing_splash",false);
            PotionCraftActions.action(player,action);int acknowledged=f.packets.get(player.getUUID()).size();
            for(int i=0;i<200;i++)PotionCraftActions.action(player,action);
            f.check(f.packets.get(player.getUUID()).size()==acknowledged,
                    "Two hundred same-tick duplicates do not resend/rebuild a full acknowledgement palette");
            var acknowledgedQuote=sent(f,0);
            // Also model a legitimate follow-up sent after the first response but within this server tick.
            PotionCraftActions.action(player,new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(
                    f.run,acknowledgedQuote.revision(),"minecraft:healing_splash",false));
            sync.invoke(null,player,session,false);
            f.check(sent(f,0).revision()>acknowledgedQuote.revision(),
                    "Next periodic sync acknowledges even an unchanged rate-limited legitimate follow-up");
            int coalesced=f.packets.get(player.getUUID()).size();sync.invoke(null,player,session,false);
            f.check(coalesced==acknowledged+1&&f.packets.get(player.getUUID()).size()==coalesced,
                    "All deferred input coalesces to one reply then returns to zero unchanged packets");
            f.check(player.inventoryMenu.getCarried().getCount()==1&&f.charges(0)==17&&f.count(1,Items.POTION)==2,
                    "Packet pressure still commits exactly one output and donor debit");
            f.check(bytes>0&&bytes<65536,"Representative full recipe palette remains below64KiB");
            com.mojang.logging.LogUtils.getLogger().info("PALETTE_INTEGRATION_COST entries={} bytes={} unchanged_evaluations=200 unchanged_packets=0 duplicate_actions=200 duplicate_packets=0 deferred_ack_packets=1 prior_source_expected_duplicate_ack_packets=200 unchanged_total_us={}",
                    quote.entries().size(),bytes,unchangedNanos/1000.0);
        } catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        h.succeed();
    }

    /** Controlled palette refresh/action kernels; these fixtures do not model a rendered client or world TPS. */
    private static final class PalettePoller {
        final java.lang.reflect.Method sync;
        final Map<ServerPlayer,?> sessions;
        @SuppressWarnings("unchecked") PalettePoller() throws ReflectiveOperationException {
            sessions=(Map<ServerPlayer,?>)Fixture.field(PotionCraftActions.class,"SESSIONS",null);
            sync=Arrays.stream(PotionCraftActions.class.getDeclaredMethods()).filter(m->m.getName().equals("sync")).findFirst().orElseThrow();
            sync.setAccessible(true);
        }
        void poll(Fixture f) throws ReflectiveOperationException {poll(f,f.current.size());}
        void poll(Fixture f,int viewers) throws ReflectiveOperationException {
            for(var player:f.current.subList(0,viewers)) {
                var session=sessions.get(player);f.check(session!=null,"Every measured native viewer has a real palette subscription");
                sync.invoke(null,player,session,false);
            }
        }
        void nextWindow(ServerPlayer player) throws ReflectiveOperationException {
            var session=sessions.get(player);var field=session.getClass().getDeclaredField("lastAction");field.setAccessible(true);
            field.setLong(session,Long.MIN_VALUE); // Deterministic next action window, without altering the shared server's tick counter.
        }
    }
    private record WireTotals(long packets,long bytes,long materialRows) {}
    private static WireTotals wireTotals(Fixture f) {
        long packets=0,bytes=0,rows=0;
        for(var player:f.current)for(var packet:f.packets.get(player.getUUID()))
            if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom
                    &&custom.payload() instanceof net.goui.cosmicdungeon.network.PotionCraftPayloads.View view) {
                var buffer=io.netty.buffer.Unpooled.buffer();
                try {
                    var wire=new net.minecraft.network.RegistryFriendlyByteBuf(buffer,f.level.registryAccess());
                    net.goui.cosmicdungeon.network.PotionCraftPayloads.View.STREAM_CODEC.encode(wire,view);
                    int encoded=buffer.readableBytes();f.check(encoded>0&&encoded<65536,"Actual final23 recipe route payload remains bounded");
                    var decoded=net.goui.cosmicdungeon.network.PotionCraftPayloads.View.STREAM_CODEC.decode(wire);
                    f.check(buffer.readableBytes()==0&&decoded.run()==view.run()&&decoded.revision()==view.revision()
                            &&decoded.entries().size()==view.entries().size(),"Each actual subscription packet round trips its exact generation");
                    for(int i=0;i<view.entries().size();i++)
                        f.check(view.entries().get(i).materials().equals(decoded.entries().get(i).materials()),"Final23 selected-input/reagent details survive the actual wire");
                    packets++;bytes+=encoded;rows+=view.entries().stream().mapToLong(e->e.materials().size()).sum();
                } finally {buffer.release();}
            }
        return new WireTotals(packets,bytes,rows);
    }
    private static void openAll(Fixture f) {
        for(var player:f.current)PotionCraftActions.request(player,new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(true));
    }
    private static void boundary(Fixture f) {for(var player:f.current)SharedCrafting.forget(player);}
    private static void costsLog(String phase,int members,int viewers,int round,int sweeps,long[] nanos,CraftingWork.Probe probe,WireTotals before,WireTotals after) {
        long[] sorted=nanos.clone();Arrays.sort(sorted);
        com.mojang.logging.LogUtils.getLogger().info(
                "BATCH24_CRAFT_WORK phase={} members={} subscribers={} round={} sweeps={} samples={} view_calls={} quote_builds={} entry_builds={} planner_choices={} inventory_scans={} inventory_copies={} copied_slots={} stock_builds={} stock_slots={} palette_packets={} wire_packets={} wire_bytes={} material_rows={} min_us={} median_us={} p95_us={} max_us={}",
                phase,members,viewers,round,sweeps,nanos.length,probe.viewCalls,probe.quoteBuilds,probe.entryBuilds,probe.plannerChoices,
                probe.inventoryScans,probe.inventoryCopies,probe.copiedSlots,probe.stockBuilds,probe.stockSlots,probe.palettePackets,
                after.packets()-before.packets(),after.bytes()-before.bytes(),after.materialRows()-before.materialRows(),
                sorted[0]/1000.0,sorted[sorted.length/2]/1000.0,sorted[(int)Math.ceil(sorted.length*.95)-1]/1000.0,sorted[sorted.length-1]/1000.0);
    }
    private static void sixPlayerCosts(GameTestHelper h) {
        var matched=new HashMap<String,List<Long>>();
        for(boolean legacy:new boolean[]{true,false})for(int viewers:new int[]{1,6})
                try(var f=new Fixture(h,Long.MAX_VALUE-3390-viewers-(legacy?0:10),6,true)) {
            f.clear();for(int i=0;i<6;i++) {
                f.healing(i,3);f.item(i,7,Items.GLOWSTONE_DUST,3);f.item(i,8,Items.REDSTONE,3);
                f.potion(i,9,Items.POTION,Potions.AWKWARD);f.potion(i,10,Items.POTION,Potions.HEALING);
            }
            for(int i=0;i<viewers;i++)PotionCraftActions.request(f.p(i),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(true));
            var poller=new PalettePoller();var opened=wireTotals(f);
            f.check(opened.packets()==viewers&&opened.materialRows()>0,"All actual brewer subscriptions include final23 route details");
            CraftingWork.begin(legacy);
            try {for(int warm=0;warm<10;warm++){boundary(f);poller.poll(f,viewers);poller.poll(f,viewers);}}
            finally {CraftingWork.end();}
            for(int round=0;round<3;round++) {
                var before=wireTotals(f);long[] timings=new long[200];CraftingWork.begin(legacy);var probe=CraftingWork.probe;
                try {
                    for(int sweep=0;sweep<100;sweep++) {
                        boundary(f);long start=System.nanoTime();poller.poll(f,viewers);timings[sweep*2]=System.nanoTime()-start;
                        start=System.nanoTime();poller.poll(f,viewers);timings[sweep*2+1]=System.nanoTime()-start;
                    }
                } finally {CraftingWork.end();}
                var after=wireTotals(f);
                f.check(after.packets()==before.packets()&&probe.palettePackets==0,"One hundred controlled5tick boundaries plus warm sweeps send zero unchanged deltas");
                f.check(probe.viewCalls==200L*viewers&&probe.inventoryScans==100L*6&&probe.quoteBuilds==0,
                        "Every actual viewer evaluated, each owner scanned once per injected boundary, unchanged recipe entries reused");
                long stockBuilds=legacy?200L*viewers*6:100L*6, copies=legacy?200L*viewers*6:0;
                f.check(probe.stockBuilds==stockBuilds&&probe.inventoryCopies==copies
                        &&probe.copiedSlots==copies*f.p(0).getInventory().getContainerSize()
                        &&probe.stockSlots==stockBuilds*f.p(0).getInventory().getContainerSize(),
                        "Unchanged cold/warm sweeps remove only redundant detached copies/projections; six native boundary scans stay required");
                var same=List.of(probe.viewCalls,probe.quoteBuilds,probe.entryBuilds,probe.plannerChoices,probe.inventoryScans,
                        after.packets()-before.packets(),after.bytes()-before.bytes(),after.materialRows()-before.materialRows());
                String key=viewers+":unchanged:"+round;
                if(legacy)matched.put(key,same);else f.check(matched.get(key).equals(same),"Same-workload legacy/optimized unchanged quote/scans/packets/bytes remain exact");
                costsLog((legacy?"legacy_":"optimized_")+"unchanged",6,viewers,round,100,timings,probe,before,after);
            }
            for(int round=0;round<3;round++) {
                var before=wireTotals(f);long[] timings=new long[20];CraftingWork.begin(legacy);var probe=CraftingWork.probe;
                try {
                    for(int sweep=0;sweep<20;sweep++) {
                        f.p(0).getInventory().getItem(5).setCount(sweep%2==0?2:3);
                        f.p(0).getInventory().setChanged();boundary(f);
                        long start=System.nanoTime();poller.poll(f,viewers);timings[sweep]=System.nanoTime()-start;
                    }
                } finally {CraftingWork.end();}
                var after=wireTotals(f);
                f.check(after.packets()-before.packets()==20L*viewers&&probe.palettePackets==20L*viewers,
                        "Changed own/pooled ingredient rows update every real subscribed party member once");
                f.check(probe.inventoryScans==20L*6&&probe.quoteBuilds==20L*viewers,
                        "Changing stock invalidates bounded owner/quote snapshots at every represented refresh boundary");
                long stockBuilds=legacy?20L*viewers*6:20L*6, copies=legacy?20L*viewers*6:0;
                f.check(probe.stockBuilds==stockBuilds&&probe.inventoryCopies==copies
                        &&probe.copiedSlots==copies*f.p(0).getInventory().getContainerSize()
                        &&probe.stockSlots==stockBuilds*f.p(0).getInventory().getContainerSize(),
                        "Changing stock still refreshes every owner and quote while eliminating only repeated per-viewer stock copies");
                var same=List.of(probe.viewCalls,probe.quoteBuilds,probe.entryBuilds,probe.plannerChoices,probe.inventoryScans,
                        after.packets()-before.packets(),after.bytes()-before.bytes(),after.materialRows()-before.materialRows());
                String key=viewers+":changing:"+round;
                if(legacy)matched.put(key,same);else f.check(matched.get(key).equals(same),"Same-workload legacy/optimized changing quote/scans/packets/bytes remain exact");
                costsLog((legacy?"legacy_":"optimized_")+"changing",6,viewers,round,20,timings,probe,before,after);
            }
            var before=wireTotals(f);f.p(0).getInventory().getItem(5).shrink(1);f.p(0).getInventory().setChanged();
            poller.poll(f,viewers);f.check(wireTotals(f).packets()-before.packets()==viewers,"Native inventory dirty notification invalidates all pooled subscriber quotes without a forced boundary");
            for(var player:f.current)f.check(GroupResourceConsent.choice(D1RunData.get(f.level.getServer()),f.run,player.getUUID()).equals("YES"),
                    "Every measured member explicitly consents in the eligible native run");
        } catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
    }
    private static List<Integer> fuelImages(Fixture f) {return f.current.stream().map(p->BrewingFuel.charges(SharedCraftData.get(f.level.getServer()).fuel(p.getUUID()))).toList();}
    private static long total(Fixture f,Item item) {long result=0;for(int i=0;i<f.current.size();i++)result+=f.count(i,item);return result;}
    private static void noSpend(Fixture f,List<CompoundTag> inventories,List<Integer> fuel,long sequence,String why) {
        f.unchanged(inventories,why);f.check(fuelImages(f).equals(fuel)&&DungeonMetricsService.resourceSequence(f.level.getServer(),f.run)==sequence
                &&SharedCraftData.get(f.level.getServer()).pendingRun(f.run)==null,why+" preserves exact fuel, metrics and journal");
    }
    private static void sixPlayerQueue(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3270,6,true)) {
            String healing="minecraft:healing_splash";f.clear();for(int i=0;i<6;i++)f.healing(i,2);
            openAll(f);var poller=new PalettePoller();var queued=new ArrayList<net.goui.cosmicdungeon.network.PotionCraftPayloads.Action>();
            for(int i=0;i<6;i++){var view=sent(f,i);queued.add(new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,view.revision(),healing,false));}
            long start=System.nanoTime();
            for(int i=0;i<6;i++)PotionCraftActions.action(f.p(i),queued.get(i));
            long elapsed=System.nanoTime()-start;
            for(int i=0;i<6;i++)f.check(f.p(i).inventoryMenu.getCarried().getCount()==1&&f.charges(i)==17&&f.revision(i)==1
                    &&f.count(i,Items.POTION)==1&&f.count(i,Items.NETHER_WART)==1&&f.count(i,Items.GLISTERING_MELON_SLICE)==1
                    &&f.count(i,Items.GUNPOWDER)==1&&f.count(i,Items.BLAZE_POWDER)==0,"Six prequeued subscriber actions serialize to one exact own three-step craft each");
            var inventories=f.images();var fuel=fuelImages(f);long sequence=DungeonMetricsService.resourceSequence(f.level.getServer(),f.run);
            var paidCursors=f.current.stream().map(player->player.inventoryMenu.getCarried().copy()).toList();
            f.check(sequence==6&&DungeonMetricsService.donationCursor(f.level.getServer(),f.run)==0&&total(f,Items.BLAZE_POWDER)==0,
                    "Six committed sequences, six fuel powders and no fictitious shared donations");
            for(int duplicate=0;duplicate<20;duplicate++)for(int i=0;i<6;i++)PotionCraftActions.action(f.p(i),queued.get(i));
            noSpend(f,inventories,fuel,sequence,"One hundred twenty overlapping duplicate actions");
            for(int i=0;i<6;i++)f.check(ItemStack.matches(paidCursors.get(i),f.p(i).inventoryMenu.getCarried()),
                    "Duplicate actions preserve every exact single paid cursor output");
            for(int i=0;i<6;i++) {
                PotionCraftActions.request(f.p(i),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(false));
                PotionCraftActions.request(f.p(i),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Request(true));
                f.check(sent(f,i).revision()>queued.get(i).revision(),"Every close/reopen advances its individual subscriber generation");
                poller.nextWindow(f.p(i));PotionCraftActions.action(f.p(i),queued.get(i));
            }
            noSpend(f,inventories,fuel,sequence,"Old subscriber generations after reopen");
            for(int i=0;i<6;i++)f.check(ItemStack.matches(paidCursors.get(i),f.p(i).inventoryMenu.getCarried()),
                    "Old subscriber generations preserve every exact single paid cursor output");
            // New controlled donor stock for the guard matrix; the six preceding paid outcomes were asserted before this fixture reseed.
            f.clear();for(int i=1;i<6;i++)f.healing(i,1);poller.poll(f);
            f.p(0).inventoryMenu.setCarried(new ItemStack(Items.DIAMOND));PotionCursorCustody.capture(f.p(0));poller.poll(f);
            inventories=f.images();fuel=fuelImages(f);var cursor=f.p(0).inventoryMenu.getCarried().copy();
            poller.nextWindow(f.p(0));var view=sent(f,0);
            PotionCraftActions.action(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,view.revision(),healing,false));
            noSpend(f,inventories,fuel,sequence,"Mismatched full cursor");
            f.check(ItemStack.isSameItemSameComponents(cursor,f.p(0).inventoryMenu.getCarried())&&f.p(0).inventoryMenu.getCarried().getCount()==1,
                    "Rejected cursor craft retains the actual carried item");
            f.p(0).inventoryMenu.setCarried(ItemStack.EMPTY);PotionCursorCustody.capture(f.p(0));
            for(int slot=0;slot<36;slot++)f.p(0).getInventory().setItem(slot,new ItemStack(Items.DIAMOND,64));
            boundary(f);poller.poll(f);view=sent(f,0);inventories=f.images();fuel=fuelImages(f);
            f.check(view.entries().stream().anyMatch(e->e.targetId().equals(healing)&&e.shared()>0),"Full Shift destination still has genuinely affordable donor resources");
            poller.nextWindow(f.p(0));PotionCraftActions.action(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,view.revision(),healing,true));
            noSpend(f,inventories,fuel,sequence,"Full Shift inventory destination");
            f.p(0).getInventory().clearContent();boundary(f);poller.poll(f);view=sent(f,0);
            for(int i=1;i<6;i++)f.storedConsent(i,false);
            inventories=f.images();fuel=fuelImages(f);poller.nextWindow(f.p(0));
            PotionCraftActions.action(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,view.revision(),healing,false));
            noSpend(f,inventories,fuel,sequence,"All donor stored consent invalidated after queued quote");
            for(int i=1;i<6;i++)f.storedConsent(i,true);poller.poll(f);view=sent(f,0);inventories=f.images();fuel=fuelImages(f);
            f.runs.put(f.run,new DungeonRunRegistryData.RunRecord(f.run,"dungeon_1","minecraft:overworld",0,List.of("cosmicdungeon:outside_batch24_fixture"),
                    1,"ACTIVE","",System.currentTimeMillis(),f.record.orderedPlayers(),List.of(),List.of()));
            try {
                poller.nextWindow(f.p(0));PotionCraftActions.action(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,view.revision(),healing,false));
                noSpend(f,inventories,fuel,sequence,"Current native dimension excluded from eligible run");
            } finally {f.runs.put(f.run,f.record);}
            PotionCraftActions.invalidate(f.p(0));openAll(f);poller.poll(f);view=sent(f,0);inventories=f.images();fuel=fuelImages(f);
            for(int i=1;i<6;i++){f.players.remove(f.p(i).getUUID());f.online.remove(f.p(i));}
            try {
                poller.nextWindow(f.p(0));PotionCraftActions.action(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,view.revision(),healing,false));
                noSpend(f,inventories,fuel,sequence,"All queued donors disconnected before server execution");
            } finally {for(int i=1;i<6;i++){f.players.put(f.p(i).getUUID(),f.p(i));f.online.add(f.p(i));}}
            poller.poll(f);view=sent(f,0);inventories=f.images();fuel=fuelImages(f);
            PotionCraftActions.logout(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(f.p(0)));
            f.players.remove(f.p(0).getUUID());f.online.remove(f.p(0));
            try {
                PotionCraftActions.action(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,view.revision(),healing,false));
                noSpend(f,inventories,fuel,sequence,"Requester logout cancels its palette subscription and old queued action");
            } finally {f.players.put(f.p(0).getUUID(),f.p(0));f.online.add(f.p(0));}
            try(var other=new Fixture(h,Long.MAX_VALUE-3271,2,true)) {
                other.clear();openAll(other);
                other.check(sent(other,0).entries().stream().allMatch(e->e.own()==0&&e.shared()==0),"Second simultaneous party cannot quote the six-member party's physical stock");
                other.check(!PotionCrafting.craft(other.p(0),f.run,healing,false),"Foreign run cannot spend six-member party resources");
                other.healing(1,1);
                other.check(PotionCrafting.craft(other.p(0),other.run,healing,false)&&other.charges(0)==17
                        &&DungeonMetricsService.resourceSequence(other.level.getServer(),other.run)==1,"Second party independently commits one consenting shared craft");
                noSpend(f,inventories,fuel,sequence,"Second party craft isolates all six owners, fuel and metrics");
            }
            com.mojang.logging.LogUtils.getLogger().info(
                    "BATCH24_CRAFT_QUEUE members=6 queued=6 committed=6 outputs=6 steps=18 fuel_powders=6 remaining_charges=102 duplicates=120 rejected_sequence_full_cursor_full_shift_consent_dimension_donor_disconnect_requester_logout=true second_party_isolated=true transaction_total_us={}",
                    elapsed/1000.0);
        } catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
    }
    private static void sixPlayerFairness(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3272,6,true)) {
            f.clear();for(int i=1;i<6;i++)f.healing(i,2);
            var ranks=new HashMap<Integer,Long>();var expected=new HashMap<Integer,Map<Item,Long>>();
            for(int i=1;i<6;i++){ranks.put(i,0L);var counts=new HashMap<Item,Long>();for(var item:List.of(Items.POTION,Items.NETHER_WART,Items.GLISTERING_MELON_SLICE,Items.GUNPOWDER,Items.BLAZE_POWDER))counts.put(item,f.count(i,item));expected.put(i,counts);}
            long donationSequence=0;
            for(int craft=0;craft<5;craft++) {
                // Independent one-unit LRU oracle over known physical ingredients, including fuel only on the first craft.
                var ingredients=new ArrayList<Item>();if(craft==0)ingredients.add(Items.BLAZE_POWDER);
                ingredients.addAll(List.of(Items.GLISTERING_MELON_SLICE,Items.GUNPOWDER,Items.NETHER_WART,Items.POTION));
                for(var ingredient:ingredients) {
                    int chosen=-1;
                    for(int owner=1;owner<6;owner++)if(expected.get(owner).get(ingredient)>0
                            &&(chosen<0||ranks.get(owner)<ranks.get(chosen)||ranks.get(owner).equals(ranks.get(chosen))
                            &&f.p(owner).getStringUUID().compareTo(f.p(chosen).getStringUUID())<0))chosen=owner;
                    f.check(chosen>0,"Independent donor oracle has enough eligible physical material");
                    expected.get(chosen).put(ingredient,expected.get(chosen).get(ingredient)-1);ranks.put(chosen,++donationSequence);
                }
                f.check(PotionCrafting.craft(f.p(0),f.run,"minecraft:healing_splash",true),"Repeated shared native craft succeeds");
                for(int owner=1;owner<6;owner++) {
                    for(var ingredient:expected.get(owner).keySet())f.check(f.count(owner,ingredient)==expected.get(owner).get(ingredient),"Actual donor units follow the independent LRU decision");
                    f.check(DungeonMetricsService.donationRank(f.level.getServer(),f.run,f.p(owner).getUUID())==ranks.get(owner),"Only actual donor ranks advance after successful commit");
                }
                f.check(DungeonMetricsService.resourceSequence(f.level.getServer(),f.run)==craft+1
                        &&DungeonMetricsService.donationCursor(f.level.getServer(),f.run)==donationSequence,"Shared sequence and individual donated units settle once per serialized craft");
            }
            f.check(f.products()==5&&f.charges(0)==5&&total(f,Items.POTION)==5&&total(f,Items.NETHER_WART)==5
                    &&total(f,Items.GLISTERING_MELON_SLICE)==5&&total(f,Items.GUNPOWDER)==5&&total(f,Items.BLAZE_POWDER)==4
                    &&donationSequence==21,"Five shared outputs consume five bottles/reagents and one powder with fifteen exact fuel steps");
            var donated=new ArrayList<Long>();
            for(int owner=1;owner<6;owner++) {
                long used=0;for(var item:expected.get(owner).keySet())used+=(item==Items.BLAZE_POWDER?1:2)-expected.get(owner).get(item);
                donated.add(used);f.check(f.metrics(owner).contributed().values().stream().mapToLong(Long::longValue).sum()==used,
                        "Every donor's durable metric records exactly its physical donated units");
            }
            f.check(Collections.max(donated)-Collections.min(donated)<=1,"Five equally stocked native donors rotate fairly within one donated unit");
            com.mojang.logging.LogUtils.getLogger().info("BATCH24_CRAFT_FAIRNESS members=6 donors=5 crafts=5 outputs=5 steps=15 fuel_powders=1 remaining_charges=5 resource_sequence=5 donated_units=21 donor_units={} exact_lru_ranks=true",donated);
        }
    }

    private static void sixPlayerContention(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3273,6,true)) {
            String healing="minecraft:healing_splash";f.clear();f.healing(5,1);openAll(f);
            var queue=new ArrayList<net.goui.cosmicdungeon.network.PotionCraftPayloads.Action>();
            for(int i=0;i<6;i++) {
                var view=sent(f,i);
                f.check(view.entries().stream().anyMatch(e->e.targetId().equals(healing)&&e.shared()==1),
                        "All six actual subscriber quotes see the same single physical route without reserving it");
                queue.add(new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,view.revision(),healing,false));
            }
            for(int i=0;i<6;i++)PotionCraftActions.action(f.p(i),queue.get(i));
            f.check(f.p(0).inventoryMenu.getCarried().is(Items.SPLASH_POTION)&&f.p(0).inventoryMenu.getCarried().getCount()==1
                    &&f.charges(0)==17&&f.revision(0)==1,"First queued action commits the only actual recipe");
            for(int i=1;i<6;i++)f.check(f.p(i).inventoryMenu.getCarried().isEmpty()&&f.charges(i)==0&&f.revision(i)==0,
                    "Later serialized stale-affordable actions cannot consume already spent resources or fuel");
            f.check(total(f,Items.POTION)==0&&total(f,Items.NETHER_WART)==0&&total(f,Items.GLISTERING_MELON_SLICE)==0
                    &&total(f,Items.GUNPOWDER)==0&&total(f,Items.BLAZE_POWDER)==0
                    &&DungeonMetricsService.resourceSequence(f.level.getServer(),f.run)==1
                    &&DungeonMetricsService.donationCursor(f.level.getServer(),f.run)==5
                    &&f.metrics(5).contributed().values().stream().mapToLong(Long::longValue).sum()==5
                    &&SharedCraftData.get(f.level.getServer()).pendingRun(f.run)==null,
                    "Six competing palette requests produce one output, one powder debit, three fuel steps and one metrics sequence");
            com.mojang.logging.LogUtils.getLogger().info("BATCH24_CRAFT_CONTENTION members=6 subscribers=6 prequeued_affordable=6 physical_routes=1 committed=1 outputs=1 fuel_powders=1 steps=3 remaining_charges=17 resource_sequence=1 shared_units=5 stale_actions=5 no_reservations=true");
        }
    }
    private static void freshStockGuard(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3274,6,true)) {
            String healing="minecraft:healing_splash";f.clear();f.healing(5,1);openAll(f);
            var view=sent(f,0);var input=f.p(5).getInventory().getItem(0);int dirty=f.p(5).getInventory().getTimesChanged();
            f.check(input.is(Items.POTION)&&view.entries().stream().anyMatch(e->e.targetId().equals(healing)&&e.shared()==1),
                    "Stale-stock fixture begins with one authoritative affordable shared route");
            input.setCount(0); // An unannounced component/count mutation within the existing five-tick inventory snapshot window.
            f.check(f.p(5).getInventory().getTimesChanged()==dirty,"Adversarial raw stack mutation intentionally sends no inventory dirty notification");
            f.check(PotionCrafting.view(f.p(0),view.revision()).entries().stream().anyMatch(e->e.targetId().equals(healing)&&e.shared()==1),
                    "Bounded availability may retain the former count until invalidation, and promises no reservation");
            var before=f.images();var fuel=fuelImages(f);
            PotionCraftActions.action(f.p(0),new net.goui.cosmicdungeon.network.PotionCraftPayloads.Action(f.run,view.revision(),healing,false));
            noSpend(f,before,fuel,0,"Fresh server debit rejects an unannounced missing donor input despite stale cached quote");
            for(var player:f.current)f.check(player.inventoryMenu.getCarried().isEmpty(),"Stale affordable preview cannot produce any unpaid cursor output");
            f.check(total(f,Items.POTION)==0&&total(f,Items.NETHER_WART)==1&&total(f,Items.GLISTERING_MELON_SLICE)==1
                    &&total(f,Items.GUNPOWDER)==1&&total(f,Items.BLAZE_POWDER)==1
                    &&sent(f,0).entries().stream().noneMatch(e->e.targetId().equals(healing)&&e.shared()>0),
                    "Rejected execution keeps every remaining reagent/powder and acknowledges the fresh unavailable quote");
            com.mojang.logging.LogUtils.getLogger().info("BATCH24_CRAFT_FRESH_GUARD members=6 subscribers=6 dirty_notified=false cached_affordable=true physical_input_removed=true committed=0 outputs=0 fuel_used=0 metrics_sequence=0 fresh_debit_authority=true");
        }
    }

    private static void nativeMemo(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-3280,1,true)) {
            f.clear();f.healing(0,2);String gunpowder=PotionRecipeSelection.itemKey(Items.GUNPOWDER);
            String water="potion/minecraft:potion/minecraft:water";
            var first=SharedCrafting.potionStock(f.p(0));
            f.check(SharedCrafting.potionStock(f.p(0))==first&&first.get(water)==2&&first.get(gunpowder)==2,
                    "Native owner stock projection reuses the same immutable identity within its inventory snapshot");
            boolean immutable=false;try{first.put(gunpowder,99);}catch(UnsupportedOperationException expected){immutable=true;}
            f.check(immutable&&first.get(gunpowder)==2,"A viewer cannot mutate another viewer's memoized resource counts");
            var detached=SharedCrafting.inventory(f.p(0),false);detached.get(0).setCount(0);
            f.check(SharedCrafting.potionStock(f.p(0))==first&&first.get(water)==2&&f.count(0,Items.POTION)==2,
                    "Detached transaction inventory edits cannot change live items or the shared memo");
            f.p(0).getInventory().getItem(5).shrink(1);f.p(0).getInventory().setChanged();
            var dirty=SharedCrafting.potionStock(f.p(0));
            f.check(dirty!=first&&dirty.get(gunpowder)==1&&first.get(gunpowder)==2,"Native dirty notification rebuilds the projection and preserves prior immutable snapshots");
            SharedCrafting.forget(f.p(0));var forgotten=SharedCrafting.potionStock(f.p(0));
            f.check(forgotten!=dirty&&forgotten.equals(dirty),"Existing lifecycle forget invalidates the inventory projection without changing counts");
            detached=SharedCrafting.inventory(f.p(0),true);var fresh=SharedCrafting.potionStock(f.p(0));
            f.check(fresh!=forgotten&&fresh.equals(forgotten),"Fresh transaction snapshot invalidates the attached memo on the same existing lifecycle");
            detached.get(5).setCount(0);
            f.check(SharedCrafting.potionStock(f.p(0))==fresh&&fresh.get(gunpowder)==1&&f.count(0,Items.GUNPOWDER)==1,
                    "Fresh detached plan mutation also cannot alias the retained memo or live inventory");
            int changes=f.p(0).getInventory().getTimesChanged();f.p(0).getInventory().getItem(5).grow(1);
            f.check(f.p(0).getInventory().getTimesChanged()==changes&&SharedCrafting.potionStock(f.p(0))==fresh&&fresh.get(gunpowder)==1,
                    "Unannounced live changes may retain a bounded preview until its existing invalidation window");
            var freshRead=SharedCrafting.inventory(f.p(0),true);var rebuilt=SharedCrafting.potionStock(f.p(0));
            f.check(freshRead.get(5).getCount()==2&&rebuilt.get(gunpowder)==2&&rebuilt!=fresh&&fresh.get(gunpowder)==1,
                    "Fresh authoritative inventory rereads the real stock and rebuilds the memo without rewriting old snapshots");
            com.mojang.logging.LogUtils.getLogger().info("BATCH24_CRAFT_MEMO immutable=true same_snapshot_identity=true dirty_rebuild=true forget_rebuild=true fresh_detached_no_alias=true quiet_preview_bounded=true fresh_read_authoritative=true");
        }
    }

}
