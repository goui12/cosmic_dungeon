package net.goui.cosmicdungeon.playerclass.bogatyr;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.network.BogatyrPayloads;
import net.goui.cosmicdungeon.network.BogatyrPayloads.Kind;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.resource.*;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Synchronous native entity/player saves in one already loaded chunk; fixture owners and blocks are restored. */
public final class BogatyrCommandGameTests {
    private BogatyrCommandGameTests(){}
    static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;final ServerLevel level;final Vec3 origin;final long run;
        final List<ServerPlayer> current=new ArrayList<>(),owned=new ArrayList<>(),online;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs;final Map<UUID,ServerPlayer> players;
        final Map<UUID,?> stats,advancements;final List<Path> files=new ArrayList<>();
        final List<io.netty.channel.embedded.EmbeddedChannel> channels=new ArrayList<>();
        final Map<UUID,List<Packet<?>>> packets=new HashMap<>();
        final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();
        final Set<UUID> bonds=new HashSet<>();final List<Wolf> wolves=new ArrayList<>();
        final List<Entity> extras=new ArrayList<>();
        final Set<Long> extraRuns=new HashSet<>();
        final CompoundTag journalBefore;
        Fixture(GameTestHelper helper,long run){this(helper,run,"dungeon_2");}
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper,long run,String dungeon){
            this.helper=helper;this.level=helper.getLevel();this.run=run;var server=level.getServer();
            var anchor=helper.absoluteVec(new Vec3(1.5,10.5,1.5));var chunk=new ChunkPos(BlockPos.containing(anchor));
            origin=new Vec3(chunk.getMinBlockX()+8.5,Math.floor(anchor.y),chunk.getMinBlockZ()+8.5);
            try{
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)field(DungeonRunRegistryData.class,"runsById",DungeonRunRegistryData.get(server));
                players=(Map<UUID,ServerPlayer>)field(PlayerList.class,"playersByUUID",server.getPlayerList());
                online=(List<ServerPlayer>)field(PlayerList.class,"players",server.getPlayerList());
                stats=(Map<UUID,?>)field(PlayerList.class,"stats",server.getPlayerList());
                advancements=(Map<UUID,?>)field(PlayerList.class,"advancements",server.getPlayerList());
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            journalBefore=BogatyrCommandData.get(server).image();
            check(!runs.containsKey(run)&&!BogatyrCompanionData.get(server).runRetired(run),"Unique wolf fixture run");
            for(int i=0;i<2;i++){
                var profile=new GameProfile(UUID.randomUUID(),"WolfTest"+i);String id=profile.id().toString();
                var ownerFiles=List.of(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat"),
                        server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat_old"),
                        server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(id+".json"),
                        server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(id+".json"));
                check(!players.containsKey(profile.id())&&ownerFiles.stream().noneMatch(Files::exists),"New fixture owner");
                files.addAll(ownerFiles);var player=create(profile);var root=new CompoundTag();
                root.putString(ClassData.KEY_CLASS_ID,"bogatyr");root.putString("future_marker","preserved");
                player.getPersistentData().put(ClassData.ROOT_TAG,ClassResourceLedger.forRun(root,run).applyTo(root));
                current.add(player);players.put(profile.id(),player);online.add(player);
            }
            runs.put(run,new DungeonRunRegistryData.RunRecord(run,dungeon,"minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,
                    current.stream().map(ServerPlayer::getUUID).toList(),List.of(),List.of()));
            var center=BlockPos.containing(origin);
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=-1;dy<=2;dy++){
                var pos=center.offset(dx,dy,dz);check(level.hasChunkAt(pos),"Fixture never loads a chunk");
                blocks.put(pos,level.getBlockState(pos));level.setBlockAndUpdate(pos,dy==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            }
            for(var player:current){
                level.addNewPlayer(player);
                check(level.getEntity(player.getUUID())==player&&!player.isRemoved(),"Fixture owner enters native entity lookup");
            }
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
        ServerPlayer p(){return current.getFirst();}
        CompoundTag root(){return p().getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);}
        ClassResourceLedger ledger(){return ClassResourceLedger.forRun(root(),run);}
        void amount(int value){p().getPersistentData().put(ClassData.ROOT_TAG,ledger().withAmount(ClassResourceKind.KIBBLE,value).applyTo(root()));}
        int amount(){return ledger().amount(ClassResourceKind.KIBBLE);}
        List<Wolf> pack(){return BogatyrCommands.loaded(p(),run);}
        BogatyrCommands.Plan plan(Kind kind){return BogatyrCommands.plan(p(),run,kind,pack(),amount());}
        boolean execute(Kind kind){return BogatyrCommands.execute(p(),plan(kind));}
        Wolf wolf(int owner,int age,int dx,int dz){
            var wolf=EntityType.WOLF.create(level,EntitySpawnReason.MOB_SUMMONED);check(wolf!=null,"Native wolf factory");
            wolf.tame(current.get(owner));BogatyrIdentity.fresh(wolf);
            wolf.getPersistentData().putLong(BogatyrWolfEvents.RUN,run);
            wolf.getPersistentData().putString(BogatyrWolfEvents.OWNER,current.get(owner).getStringUUID());
            wolf.snapTo(origin.x+dx,origin.y,origin.z+dz,0,0);wolf.setNoAi(true);wolf.setOnGround(true);
            wolf.setOrderedToSit(false);wolf.setInSittingPose(false);wolf.setAge(age);
            check(level.addFreshEntity(wolf)&&wolf.isAddedToLevel()&&!wolf.isRemoved(),"Native wolf accepted in loaded fixture chunk");
            BogatyrWolfEvents.register(wolf,current.get(owner).getUUID(),run);wolf.setHealth(wolf.getMaxHealth());
            remember(wolf);return wolf;
        }
        void remember(Wolf wolf){wolves.add(wolf);bonds.add(BogatyrIdentity.id(wolf));}
        void reloadOwner(){
            var old=p();CompoundTag image;
            try{image=NbtIo.readCompressed(level.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(old.getStringUUID()+".dat"),NbtAccounter.create(64L*1024*1024));}
            catch(IOException error){throw new IllegalStateException(error);}
            BogatyrActions.forget(old);ClassResourceService.forget(old);
            level.removePlayerImmediately(old,Entity.RemovalReason.DISCARDED);
            check(old.isRemoved()&&level.getEntity(old.getUUID())==null,"Native disconnect invalidates cached owner references");
            var next=create(old.getGameProfile());next.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),image));
            online.remove(old);online.add(next);players.put(next.getUUID(),next);current.set(0,next);next.connection.player=next;next.setPos(origin);
            level.addRespawnedPlayer(next);
            check(level.getEntity(next.getUUID())==next&&!next.isRemoved(),"Reloaded owner replaces native UUID lookup");
        }
        CompoundTag savedWolf(Wolf wolf){
            try{
                level.save(null,true,false);var chunk=new ChunkPos(wolf.blockPosition());
                return ReadOnlyEntityRegion.find(ReadOnlyEntityRegion.read(BogatyrRecovery.entityFolder(level),chunk.x,chunk.z),wolf.getUUID()).orElseThrow();
            }catch(Exception error){throw new IllegalStateException(error);}
        }
        Wolf readWolf(CompoundTag image){
            var wolf=EntityType.WOLF.create(level,EntitySpawnReason.LOAD);check(wolf!=null,"Native reload factory");
            wolf.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),image));remember(wolf);return wolf;
        }
        BogatyrPayloads.View view(){
            var list=packets.get(p().getUUID());
            for(int i=list.size()-1;i>=0;i--)if(list.get(i) instanceof ClientboundCustomPayloadPacket packet
                    &&packet.payload() instanceof BogatyrPayloads.View value)return value;
            throw new IllegalStateException("No Wolfpack snapshot");
        }
        void check(boolean condition,String message){helper.assertTrue(condition,Component.literal(message));}
        @Override @SuppressWarnings("unchecked") public void close(){
            var server=level.getServer();var directory=BogatyrCompanionData.get(server);
            for(var player:owned){BogatyrActions.forget(player);ClassResourceService.forget(player);BogatyrRescue.forget(player);}
            for(var player:current)for(var entry:directory.forOwner(player.getUUID())){
                if(entry.run()!=run&&!extraRuns.contains(entry.run()))throw new IllegalStateException("Foreign fixture wolf");
                bonds.add(entry.wolf());if(level.getEntity(entry.entityUuid()) instanceof Wolf wolf)wolf.discard();
                directory.retireRecord(entry);
            }
            for(var wolf:wolves)if(!wolf.isRemoved())wolf.discard();
            for(var entity:extras)if(!entity.isRemoved())entity.discard();
            var journal=BogatyrCommandData.get(server);for(var player:current)journal.remove(player.getUUID());
            check(journal.image().equals(journalBefore),"Fixture preserves unrelated command journals");
            if(!journal.flushVerified())throw new IllegalStateException("Fixture command journal cleanup");
            try{
                var retired=(Set<Long>)field(BogatyrCompanionData.class,"retiredRuns",directory);
                retired.remove(run);retired.removeAll(extraRuns);
                var original=(CompoundTag)field(BogatyrCompanionData.class,"original",directory);
                var audit=original.getCompoundOrEmpty("retired_records");for(var bond:bonds)audit.remove(bond.toString());
                var modes=original.getCompoundOrEmpty(WolfModeData.KEY);for(var player:current){modes.remove(player.getUUID()+"/"+run);BogatyrThreats.invalidate(player.getUUID());}
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            if(!directory.flushVerified(server))throw new IllegalStateException("Fixture companion cleanup");
            for(var player:current){
                var id=player.getUUID();level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);
                check(level.getEntity(id)==null,"Fixture owner leaves native entity lookup");
                players.remove(id);online.removeIf(p->p.getUUID().equals(id));
                if(advancements.get(id) instanceof net.minecraft.server.PlayerAdvancements progress)progress.stopListening();
                stats.remove(id);advancements.remove(id);
            }
            runs.remove(run);D1RunData.get(server).clearRun(run);
            for(long old:extraRuns){runs.remove(old);D1RunData.get(server).clearRun(old);}
            blocks.forEach(level::setBlockAndUpdate);channels.forEach(io.netty.channel.embedded.EmbeddedChannel::finishAndReleaseAll);
            try{for(var path:files)Files.deleteIfExists(path);}catch(IOException error){throw new IllegalStateException(error);}
        }
    }

    public static void careAndBreeding(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2301)){
            var adult=f.wolf(0,0,-2,-2);var mate=f.wolf(0,0,0,-2);
            var juvenile=f.wolf(0,-200,2,-2);var cooldown=f.wolf(0,100,-2,0);
            var lowest=f.wolf(0,0,0,0);lowest.setHealth(1);
            var injured=f.wolf(0,0,2,0);injured.setHealth(injured.getMaxHealth()-2);
            var foreign=f.wolf(1,0,2,2);foreign.setHealth(1);
            adult.setOrderedToSit(true);adult.setInSittingPose(true);
            mate.setOrderedToSit(true);mate.setInSittingPose(true);
            f.amount(5);f.check(f.plan(Kind.BREED).count()==1&&f.plan(Kind.BREED).cost()==5,"Five Kibble permits one eligible adult");
            f.check(f.execute(Kind.BREED)&&f.amount()==0,"Paid partial Breed spends exactly five");
            var chosen=adult.isInLove()?adult:mate;var other=chosen==adult?mate:adult;
            f.check(chosen.isInLove()&&!other.isInLove()&&!chosen.isOrderedToSit()&&!chosen.isInSittingPose(),"Paid adult enters native love mode and can move");
            f.check(!juvenile.isInLove()&&!cooldown.isInLove()&&!lowest.isInLove()&&!foreign.isInLove(),"Juvenile, cooldown, wounded and other-owner wolves are excluded");
            other.setOrderedToSit(false);other.setInSittingPose(false);other.setInLove(f.p());
            f.check(BogatyrWolfEvents.breedingAllowed(chosen,other)&&chosen.canMate(other),"Generic active-run wolves can use native mating");
            var before=f.pack().stream().map(Wolf::getUUID).collect(java.util.stream.Collectors.toSet());
            var orbBounds=chosen.getBoundingBox().inflate(2);
            var priorOrbs=f.level.getEntitiesOfClass(ExperienceOrb.class,orbBounds).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            chosen.spawnChildFromBreeding(f.level,other);
            f.level.getEntitiesOfClass(ExperienceOrb.class,orbBounds).stream().filter(orb->!priorOrbs.contains(orb.getUUID())).forEach(f.extras::add);
            var pups=f.pack().stream().filter(w->!before.contains(w.getUUID())).toList();
            f.check(pups.size()==1,"Native birth adds exactly one enrolled pup");var pup=pups.getFirst();f.remember(pup);
            f.check(pup.isBaby()&&pup.isTame()&&pup.isOwnedBy(f.p())&&pup.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0)==f.run,"Native offspring is a tamed current-run companion");
            f.check(chosen.getAge()>0&&other.getAge()>0&&!chosen.isInLove()&&!other.isInLove(),"Native breeding cooldowns and love reset remain intact");
            f.amount(5);f.check(f.execute(Kind.HEAL)&&f.amount()==0,"Partial Heal pays for exactly one wolf");
            f.check(lowest.getHealth()==lowest.getMaxHealth()&&injured.getHealth()==injured.getMaxHealth()-2&&foreign.getHealth()==1,"Lowest-health own wolf heals fully; other targets remain unchanged");
            f.amount(10);f.check(f.execute(Kind.HEAL)&&f.amount()==5,"Only the remaining injured wolf costs five");
            f.check(f.plan(Kind.HEAL).count()==0&&!f.execute(Kind.HEAL)&&f.amount()==5,"Full-health no-op cannot consume Kibble");
            f.check(f.packets.values().stream().flatMap(List::stream).noneMatch(p->p instanceof ClientboundContainerClosePacket),"Care commands keep native inventory open");
            helper.succeed();
        }
    }
    public static void summonAndRegroup(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2302)){
            f.amount(30);f.p().setPos(f.origin.x,f.level.getMinY()-32,f.origin.z);
            f.check(f.plan(Kind.SUMMON).count()==0&&!f.execute(Kind.SUMMON)&&f.amount()==30,"No safe loaded placement means no summon or debit");
            f.p().setPos(f.origin);f.check(f.execute(Kind.SUMMON)&&f.amount()==0,"One accepted native summon costs exactly thirty");
            f.check(f.pack().size()==1&&f.pack().getFirst().isOwnedBy(f.p()),"Summon creates exactly one tamed current-run wolf");
            var summoned=f.pack().getFirst();f.remember(summoned);summoned.setNoAi(true);
            var loaded=f.wolf(0,0,3,3);var unloaded=f.wolf(0,0,-3,3);var unloadedId=unloaded.getUUID();unloaded.discard();
            var dead=f.wolf(0,0,3,-3);dead.setHealth(0);
            var foreign=f.wolf(1,0,-3,-3);var foreignPosition=foreign.position();
            f.check(f.pack().size()==2&&f.level.getEntity(unloadedId)==null,"Loaded roster excludes absent, dead and other-owner wolves");
            summoned.snapTo(f.origin.x+2,f.origin.y,f.origin.z+2);
            loaded.snapTo(f.origin.x+3,f.origin.y,f.origin.z+3);
            BogatyrCompanions.track(summoned,true);BogatyrCompanions.track(loaded,true);
            f.amount(600);f.check(loaded.startRiding(foreign,true,false),"Native restrained-pack fixture mounts one own wolf");
            f.check(f.plan(Kind.REGROUP).count()==0,"One restrained loaded wolf disables Regroup for the entire pack");
            loaded.stopRiding();loaded.snapTo(f.origin.x+3,f.origin.y,f.origin.z+3);BogatyrCompanions.track(loaded,true);
            f.amount(0);f.check(f.plan(Kind.REGROUP).count()==0,"Regroup never offers a free partial pack");
            f.amount(600);var full=f.plan(Kind.REGROUP);f.check(full.count()==2&&full.cost()==2,"Both living loaded own wolves form the affected pack");
            f.amount(1);f.check(f.plan(Kind.REGROUP).count()==0&&!f.execute(Kind.REGROUP)&&f.amount()==1,"Below whole-pack affordability changes nothing");
            f.amount(2);full=f.plan(Kind.REGROUP);f.check(BogatyrCommands.execute(f.p(),full)&&f.amount()==0,"Whole affected pack costs one per actual teleport");
            for(var target:full.targets()){
                var wolf=(Wolf)f.level.getEntity(target.entity());var pos=target.destination();
                f.check(wolf.position().equals(new Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5))&&f.level.hasChunkAt(pos),"Regroup uses quoted safe positions in loaded current dimension");
            }
            f.check(f.level.getEntity(unloadedId)==null&&!dead.isAlive()&&foreign.position().equals(foreignPosition),"Regroup does not revive, retrieve or move another owner's wolf");
            f.amount(30);BogatyrActions.sync(f.p(),true);var view=f.view();
            var action=new BogatyrPayloads.Action(view.run(),view.revision(),Kind.SUMMON);
            BogatyrActions.action(f.p(),action);int once=f.amount();BogatyrActions.action(f.p(),action);
            f.check(once==0&&f.amount()==0&&f.view().revision()>action.revision(),"Exact generation and authoritative acknowledgment prevent double summon");
            helper.succeed();
        }
    }
    public static void commandSaveRecovery(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2303)){
            var wolf=f.wolf(0,0,2,2);wolf.setHealth(1);f.amount(40);
            f.check(PlayerSaveProof.save(f.p()),"Original owner balance saved");
            var source=BogatyrRecovery.image(wolf);var id=UUID.randomUUID();
            wolf.setHealth(wolf.getMaxHealth());wolf.getPersistentData().putString(BogatyrCommands.RECEIPT,id.toString());
            var outcome=f.savedWolf(wolf);
            var pending=new CompoundTag();pending.putString("id",id.toString());pending.putLong("run",f.run);
            pending.putString("kind",Kind.HEAL.name());pending.putString("phase","entities_saved");
            pending.put("ledger",f.ledger().image());pending.putInt("cost",5);
            var sources=new ListTag();sources.add(source);pending.put("wolves",sources);
            var outcomes=new ListTag();outcomes.add(outcome);pending.put("outcomes",outcomes);
            var journal=BogatyrCommandData.get(f.level.getServer());journal.put(f.p().getUUID(),pending);
            f.check(journal.flushVerified(),"Native effect outcome journal saved before owner debit");
            var corrupt=journal.image();corrupt.getCompoundOrEmpty("pending").getCompoundOrEmpty(f.p().getStringUUID()).putInt("cost",10);
            boolean rejected=false;try{new BogatyrCommandData(corrupt);}catch(IllegalArgumentException expected){rejected=true;}
            f.check(rejected,"Corrupt cost cannot disagree with exact saved outcomes");
            f.reloadOwner();f.check(f.amount()==40&&BogatyrCommands.blocked(f.p()),"Reconnect retains original balance while command is pending");
            f.check(BogatyrCommands.reconcile(f.p())&&f.amount()==35,"Durable native outcome settles exactly one debit");
            f.reloadOwner();journal.put(f.p().getUUID(),pending);f.check(journal.flushVerified(),"Simulate crash after paid owner receipt before world acknowledgment");
            f.check(BogatyrCommands.reconcile(f.p())&&f.amount()==35,"Persisted owner receipt prevents replay debit");
            f.check(wolf.isOwnedBy(f.p())&&wolf.getOwner()==f.p(),"Cached native wolf owner resolves the current reloaded player");
            f.check(wolf.getHealth()==wolf.getMaxHealth(),"Recovery preserves the already saved full-health outcome");
            f.check(f.pack().size()==1,"Recovery never creates replacement wolves; loaded own pack="+f.pack().size());
            f.check(f.root().getStringOr("future_marker","").equals("preserved"),"Unknown owner fields survive payment");
            var prepared=pending.copy();prepared.putString("id",UUID.randomUUID().toString());prepared.putString("phase","prepared");
            prepared.put("ledger",f.ledger().image());prepared.putInt("cost",0);prepared.remove("outcomes");
            journal.put(f.p().getUUID(),prepared);f.check(journal.flushVerified(),"Ambiguous prepared command remains durable");
            f.check(!BogatyrCommands.reconcile(f.p())&&f.amount()==35&&BogatyrCommands.blocked(f.p()),"Unproven effects are held without a refund, debit or replay");
            helper.succeed();
        }
    }
    public static void runRetirement(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2304,"dungeon_1")){
            var wolf=f.wolf(0,0,2,2);wolf.getPersistentData().putString("future_wolf_marker","preserved");
            var image=f.savedWolf(wolf);var bond=BogatyrIdentity.id(wolf);wolf.discard();
            f.check(BogatyrCompanionData.get(f.level.getServer()).find(bond).isPresent()&&f.pack().isEmpty(),"Unloaded active-run companion remains in its native directory without being a care target");
            var restored=f.readWolf(image);f.check(f.level.addFreshEntity(restored)&&!restored.isRemoved(),"Active-run native save reload remains accepted");
            f.check(restored.isOwnedBy(f.p())&&restored.getOwner()==f.p(),"UUID-backed native wolf owner resolves the current fixture player");
            f.check(BogatyrIdentity.id(restored).equals(bond),"Ordinary native reload preserves stable bond");
            f.check(restored.getPersistentData().getStringOr("future_wolf_marker","").equals("preserved"),"Ordinary native reload preserves unknown entity fields");
            var nativeImage=f.savedWolf(restored);restored.discard();
            var data=BogatyrCompanionData.get(f.level.getServer());var entry=data.find(bond).orElseThrow();
            long oldRun=Long.MAX_VALUE-2391;f.check(!f.runs.containsKey(oldRun)&&!data.runRetired(oldRun),"Unique legacy source run");
            f.extraRuns.add(oldRun);
            var oldRecord=new DungeonRunRegistryData.RunRecord(oldRun,"dungeon_1","minecraft:overworld",0,
                    List.of(f.level.dimension().location().toString()),1,"FAILED","",0,List.of(f.p().getUUID()),List.of(),List.of());
            f.runs.put(oldRun,oldRecord);
            var sourceImage=nativeImage.copy();sourceImage.getCompoundOrEmpty("NeoForgeData").putLong(BogatyrWolfEvents.RUN,oldRun);
            data.remember(new BogatyrCompanionData.Companion(bond,f.p().getUUID(),oldRun,entry.dimension(),
                    entry.position(),true,entry.entityUuid()));
            var delivery=new WolfArchive(WolfArchive.RELEASING,UUID.randomUUID(),oldRun,entry.dimension(),entry.position(),
                    entry.dimension(),entry.position(),sourceImage,f.run);
            data.putArchive(bond,delivery);f.check(data.flushVerified(f.level.getServer()),"Interrupted active-target identity is durable before observation");
            var oldSource=f.readWolf(sourceImage);f.check(!BogatyrRunLifecycle.admit(oldSource)&&oldSource.isRemoved(),"Ended native source is rejected");
            f.check(data.find(bond).isPresent()&&data.archive(bond).filter(delivery::equals).isPresent(),"Rejecting source preserves unobserved active destination identity");
            f.check(BogatyrRunLifecycle.retire(f.level.getServer(),oldRecord)&&data.archive(bond).filter(delivery::equals).isPresent(),"Old source cleanup cannot erase active destination delivery");
            var targetImage=nativeImage.copy();targetImage.store("UUID",net.minecraft.core.UUIDUtil.CODEC,bond);
            var destination=f.readWolf(targetImage);BogatyrRecovery.pin(destination,delivery);
            f.check(f.level.addFreshEntity(destination)&&!destination.isRemoved()&&destination.isOwnedBy(f.p())
                    &&BogatyrIdentity.id(destination).equals(bond),"Actual native target load preserves the stable bond and owner");
            f.check(data.find(bond).filter(e->e.run()==f.run&&e.entityUuid().equals(bond)).isPresent()&&data.archive(bond).isEmpty(),
                    "Observed active native target supersedes its retired archive without duplicate delivery");
            nativeImage=f.savedWolf(destination);destination.discard();entry=data.find(bond).orElseThrow();
            var archive=new WolfArchive(WolfArchive.STORED,UUID.randomUUID(),f.run,entry.dimension(),entry.position(),"",0,nativeImage);
            data.putArchive(bond,archive);f.check(data.flushVerified(f.level.getServer()),"Legacy archive is represented durably before retirement");
            f.check(BogatyrRunLifecycle.retire(f.level.getServer(),f.runs.get(f.run)),"Run-only retirement is durably recorded");
            f.check(data.runRetired(f.run)&&data.find(bond).isEmpty()&&data.archive(bond).isEmpty(),"Ended-run archive cannot occupy a live slot or be delivered again");
            var late=f.readWolf(nativeImage);f.level.addFreshEntity(late);
            f.check(late.isRemoved()&&f.level.getEntity(late.getUUID())==null,"Late native load of ended-run wolf is discarded before readmission");
            f.check(BogatyrRunLifecycle.resetBlocker(f.level).isEmpty(),"Retired legacy archive cannot block source reset");
            helper.succeed();
        }
    }

    private static net.minecraft.world.entity.Mob hostile(Fixture f,EntityType<? extends net.minecraft.world.entity.Mob> type,int dx,int dz){
        var mob=type.create(f.level,EntitySpawnReason.MOB_SUMMONED);f.check(mob!=null,"Native hostile factory");
        mob.setNoAi(true);mob.snapTo(f.origin.x+dx,f.origin.y,f.origin.z+dz,0,0);
        f.check(f.level.addFreshEntity(mob),"Native hostile accepted");f.extras.add(mob);return mob;
    }
    private static void decide(Wolf wolf){
        var goal=wolf.targetSelector.getAvailableGoals().stream().map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                .filter(g->g instanceof BogatyrThreats.ProtectOwner).findFirst().orElseThrow();
        try{var field=goal.getClass().getDeclaredField("nextChoice");field.setAccessible(true);field.setLong(goal,Long.MIN_VALUE);}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        if(goal.canUse()){goal.start();goal.tick();}
    }
    public static void modeStandGround(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2401)){
            var wolf=f.wolf(0,0,-2,0);var foreign=f.wolf(1,0,2,0);var hostile=hostile(f,EntityType.ZOMBIE,2,2);
            hostile.setTarget(f.p());wolf.setTarget(hostile);
            var melee=wolf.goalSelector.getAvailableGoals().stream()
                    .filter(g->g.getGoal() instanceof net.minecraft.world.entity.ai.goal.MeleeAttackGoal).findFirst().orElseThrow();
            melee.start();wolf.getNavigation().moveTo(hostile,1);
            f.amount(0);BogatyrActions.sync(f.p(),true);var before=f.view();
            f.check(before.modesEnabled(),"Zero Kibble still permits free core modes");
            BogatyrActions.mode(f.p(),new BogatyrPayloads.ModeAction(before.run(),before.revision(),WolfMode.STAND_GROUND));
            f.check(f.view().mode()==WolfMode.STAND_GROUND&&f.amount()==0,"Mode acknowledgment is authoritative and free");
            f.check(wolf.isOrderedToSit()&&wolf.isInSittingPose()&&wolf.getNavigation().isDone()
                    &&wolf.getTarget()==null&&!melee.isRunning(),"Stand Ground immediately stops running movement, path and target");
            f.check(!foreign.isOrderedToSit(),"Another owner's pack is untouched");
            wolf.setOrderedToSit(false);wolf.setInSittingPose(false);
            f.check(wolf.isOrderedToSit()&&wolf.isInSittingPose(),"Vanilla sit cleanup cannot override Stand Ground");
            float health=wolf.getHealth();f.check(wolf.hurtServer(f.level,hostile.damageSources().mobAttack(hostile),1),"Real native damage is accepted");
            f.check(wolf.getHealth()<health&&wolf.isOrderedToSit()&&wolf.isInSittingPose()
                    &&wolf.getTarget()==null&&wolf.getLastHurtByMob()==null&&wolf.getLastHurtMob()==null,"Damage preserves sitting and clears retaliation immediately");
            wolf.setTarget(hostile);f.check(wolf.getTarget()==null,"Native retaliation proposals are blocked");
            f.p().setLastHurtByMob(hostile);
            var hold=wolf.goalSelector.getAvailableGoals().stream().map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                    .filter(g->g instanceof BogatyrModes.StandGround).findFirst().orElseThrow();
            f.check(hold.canUse(),"Stand Ground holds even when the nearby master was attacked");
            f.p().setPos(f.origin.x+256,f.origin.y,f.origin.z);
            f.check(!wolf.shouldTryTeleportToOwner(),"Stand Ground suppresses far-owner auto teleport");f.p().setPos(f.origin);
            f.amount(30);f.check(f.plan(Kind.BREED).count()==0&&!f.execute(Kind.BREED)&&f.amount()==30,"Stand Ground cannot charge Breed without allowing native movement");
            var current=f.view();BogatyrActions.mode(f.p(),new BogatyrPayloads.ModeAction(current.run(),current.revision(),WolfMode.DEFENSIVE));
            f.check(!wolf.isOrderedToSit()&&!wolf.isInSittingPose()&&f.view().mode()==WolfMode.DEFENSIVE,"A new mode stands the pack up without a care cooldown");
            helper.succeed();
        }
    }
    public static void modeTargets(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2402)){
            var wolf=f.wolf(0,0,-3,0);var mate=f.wolf(0,0,-2,0);
            var near=hostile(f,EntityType.ZOMBIE,1,0);var far=hostile(f,EntityType.SKELETON,3,0);
            f.check(BogatyrModes.mode(wolf)==WolfMode.DEFENSIVE,"Existing saves start Defensive");
            f.p().setLastHurtMob(far);wolf.setTarget(far);
            f.check(wolf.getTarget()==null,"Defensive ignores master's outgoing-only offense and native skeleton hunting");
            near.setTarget(f.p());wolf.setTarget(near);f.check(wolf.getTarget()==near,"Defensive protects a threatened master");
            near.setTarget(null);wolf.setTarget(null);f.p().setLastHurtByMob(far);wolf.setTarget(far);
            f.check(wolf.getTarget()==far,"Defensive reacts to an actual master's attacker");f.p().setLastHurtByMob(null);
            mate.hurtServer(f.level,near.damageSources().mobAttack(near),1);wolf.setTarget(near);
            f.check(wolf.getTarget()==near,"A wolf's attacker becomes a pack-wide defensive threat");
            wolf.setTarget(f.current.get(1));f.check(wolf.getTarget()==null,"Another friendly player remains protected");
            f.check(BogatyrModes.select(f.p(),f.run,WolfMode.AGGRESSIVE),"Aggressive selected");
            decide(wolf);f.check(wolf.getTarget()==near,"Aggressive starts nearest the master, ahead of a farther skeleton");
            near.setHealth(0);decide(wolf);f.check(wolf.getTarget()==far,"Aggressive works outward after the nearest threat dies");
            var passive=EntityType.SHEEP.create(f.level,EntitySpawnReason.MOB_SUMMONED);f.check(passive!=null,"Native passive factory");
            passive.snapTo(f.origin);f.check(f.level.addFreshEntity(passive),"Native passive accepted");f.extras.add(passive);
            wolf.setTarget(passive);f.check(wolf.getTarget()==null,"Aggressive never hunts passive prey");
            var melee=wolf.goalSelector.getAvailableGoals().stream().map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                    .filter(g->g instanceof net.minecraft.world.entity.ai.goal.MeleeAttackGoal).findFirst().orElseThrow();
            melee.start();
            try{f.check((int)Fixture.field(net.minecraft.world.entity.ai.goal.MeleeAttackGoal.class,"ticksUntilNextPathRecalculation",melee)>=20,
                    "Native melee path rebuild is staggered at roughly one second");}
            catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            helper.succeed();
        }
    }
    public static void modePersistenceAuthority(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2403)){
            var wolf=f.wolf(0,0,2,2);wolf.getPersistentData().putString("future_mode_wolf","keep");
            var image=f.savedWolf(wolf);wolf.discard();f.check(f.pack().isEmpty(),"Unloaded pack fixture has no command target");
            f.amount(0);BogatyrActions.sync(f.p(),true);var initial=f.view();
            var request=new BogatyrPayloads.ModeAction(initial.run(),initial.revision(),WolfMode.STAND_GROUND);
            BogatyrActions.mode(f.p(),request);f.check(f.view().mode()==WolfMode.STAND_GROUND,"Owner/run selection persists with no loaded wolves");
            BogatyrActions.mode(f.p(),new BogatyrPayloads.ModeAction(initial.run(),initial.revision(),WolfMode.AGGRESSIVE));
            f.check(f.view().mode()==WolfMode.STAND_GROUND,"Stale generation cannot replace current selection");
            f.check(!BogatyrModes.select(f.current.get(1),f.run+1,WolfMode.AGGRESSIVE),"Foreign or inactive run is rejected");
            f.check(BogatyrCompanionData.get(f.level.getServer()).mode(f.current.get(1).getUUID(),f.run).mode()==WolfMode.DEFENSIVE,"Mode does not leak between owners");
            f.check(PlayerSaveProof.save(f.p()),"Owner reconnect image saved");f.reloadOwner();BogatyrActions.sync(f.p(),true);
            f.check(f.view().mode()==WolfMode.STAND_GROUND&&f.view().revision()>initial.revision(),"Reconnect retains selection with a fresh session generation");
            var restored=f.readWolf(image);f.check(f.level.addFreshEntity(restored),"Native unloaded wolf returns");
            f.check(restored.isOrderedToSit()&&restored.isInSittingPose()&&BogatyrModes.mode(restored)==WolfMode.STAND_GROUND,
                    "Unloaded wolf adopts persisted mode immediately on native load");
            f.check(restored.getPersistentData().getStringOr("future_mode_wolf","").equals("keep"),"Unknown wolf data survives the mode upgrade");
            var directory=BogatyrCompanionData.get(f.level.getServer());f.check(directory.flushVerified(f.level.getServer()),"Mode is verified in native SavedData");
            var round=BogatyrCompanionData.CODEC.parse(NbtOps.INSTANCE,directory.image()).getOrThrow();
            f.check(round.mode(f.p().getUUID(),f.run).mode()==WolfMode.STAND_GROUND,"SavedData reload retains owner/run mode");
            f.check(round.mode(f.p().getUUID(),f.run+1).mode()==WolfMode.DEFENSIVE,"A later run does not inherit the prior run's mode");
            var before=f.view();f.p().getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).putString(ClassData.KEY_CLASS_ID,"theurgist");
            BogatyrActions.mode(f.p(),new BogatyrPayloads.ModeAction(before.run(),before.revision(),WolfMode.AGGRESSIVE));
            f.check(f.view().run()==0&&directory.mode(f.p().getUUID(),f.run).mode()==WolfMode.STAND_GROUND,"Execution rechecks actual class before changing mode");
            helper.succeed();
        }
    }
}
