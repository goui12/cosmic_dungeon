package net.goui.cosmicdungeon.dungeon.metrics;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Real native death/heal callbacks with isolated run/player fixtures; no game launches from tests. */
public final class RunMetricsGameTests {
    private RunMetricsGameTests() {}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final long id;
        final ServerPlayer player;final D1RunData data;final DungeonRunRegistryData.RunRecord run;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs;
        final Map<UUID,ServerPlayer> players;final List<ServerPlayer> online;
        final List<Entity> entities=new ArrayList<>();
        final io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel();
        @SuppressWarnings("unchecked") Fixture(GameTestHelper h,long id) {
            this.h=h;this.id=id;level=h.getLevel();player=player("MetricsMember");
            run=new DungeonRunRegistryData.RunRecord(id,"dungeon_1","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",System.currentTimeMillis(),
                    List.of(player.getUUID()),List.of(),List.of());
            data=D1RunData.get(level.getServer());
            try {
                var field=DungeonRunRegistryData.class.getDeclaredField("runsById");field.setAccessible(true);
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)field.get(DungeonRunRegistryData.get(level.getServer()));
                field=PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
                players=(Map<UUID,ServerPlayer>)field.get(level.getServer().getPlayerList());
                field=PlayerList.class.getDeclaredField("players");field.setAccessible(true);
                online=(List<ServerPlayer>)field.get(level.getServer().getPlayerList());
            }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            if(runs.containsKey(id))throw new IllegalStateException("Metrics fixture collision");
            runs.put(id,run);players.put(player.getUUID(),player);online.add(player);
            DungeonMetricsService.begin(level.getServer(),run,true);
        }
        ServerPlayer player(String name) {
            var server=level.getServer();var profile=new GameProfile(UUID.randomUUID(),name);
            var result=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public io.netty.channel.Channel channel(){return channel;}
                @Override public boolean isConnected(){return true;}
            };
            result.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,result,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener){}
            };
            var tag=new CompoundTag();tag.putString(ClassData.KEY_CLASS_ID,"metalmancer");tag.putBoolean("run_temp",true);
            result.getPersistentData().put(ClassData.ROOT_TAG,tag);
            result.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(.5,5,.5)));
            return result;
        }
        Zombie zombie() {
            var zombie=new Zombie(EntityType.ZOMBIE,level);zombie.setNoAi(true);
            zombie.setPos(player.position().add(2,0,0));entities.add(zombie);return zombie;
        }
        RunMetrics.Stats stats() {
            return DungeonMetricsService.snapshot(level.getServer(),run,"ACTIVE").members().stream()
                    .filter(m->m.id().equals(player.getUUID())).findFirst().orElseThrow().stats();
        }
        void check(boolean condition,String detail){h.assertTrue(condition,Component.literal(detail));}
        @Override public void close() {
            entities.forEach(Entity::discard);players.remove(player.getUUID());online.remove(player);player.discard();
            runs.remove(id);data.clearRun(id);channel.finishAndReleaseAll();
        }
    }
    public static void attribution(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-300)) {
            var direct=f.zombie();var source=f.player.damageSources().playerAttack(f.player);
            direct.hurtServer(f.level,source,100);
            f.check(f.stats().kills()==1&&f.stats().mobs().getOrDefault("minecraft:zombie",0L)==1,
                    "Native player kill updates existing credit and the exact zombie registry ID");
            D1KillStatistics.onDeath(new LivingDeathEvent(direct,source));
            f.check(f.stats().kills()==1&&f.stats().mobs().getOrDefault("minecraft:zombie",0L)==1,
                    "Repeated death callback cannot double count either metric");

            var delayed=f.zombie();delayed.hurtServer(f.level,source,2);
            delayed.setHealth(0);delayed.die(delayed.damageSources().generic());
            f.check(f.stats().kills()==2,"Environmental finish uses existing last-player attribution");
            var wolf=new Wolf(EntityType.WOLF,f.level);f.entities.add(wolf);
            wolf.setOwnerReference(EntityReference.of(f.player.getUUID()));
            var companion=f.zombie();companion.hurtServer(f.level,companion.damageSources().mobAttack(wolf),100);
            f.check(f.stats().kills()==3&&f.stats().mobs().getOrDefault("minecraft:zombie",0L)==3,
                    "Owned-companion kill uses the same authoritative owner credit");
            var canceled=f.zombie();canceled.hurtServer(f.level,source,2);
            java.util.function.Consumer<LivingDeathEvent> cancellation=e->{if(e.getEntity()==canceled)e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(cancellation);
            try {canceled.setHealth(0);canceled.die(canceled.damageSources().generic());}
            finally {NeoForge.EVENT_BUS.unregister(cancellation);}
            f.check(f.stats().kills()==3,"Canceled native death cannot add a metrics kill");
            confirmedSpawner(f);
            f.player.setHealth(10);f.player.heal(100);
            f.check(f.stats().healing()==10,"Actual health restored mirrors existing counter without overheal");
            f.runs.put(f.id,f.run.withState(DungeonRunState.RESETTING,DungeonResetReason.ABANDONED));
            f.zombie().hurtServer(f.level,source,100);
            f.check(f.stats().kills()==3,"Resetting instance cannot acquire new attributed kills");
            f.runs.put(f.id,f.run);
        }
        h.succeed();
    }
    private static void confirmedSpawner(Fixture f) {
        var anchor=f.h.absolutePos(net.minecraft.core.BlockPos.ZERO);
        var chunk=new net.minecraft.world.level.ChunkPos(anchor);
        var pos=new net.minecraft.core.BlockPos(chunk.getMinBlockX()+8,anchor.getY()+14,chunk.getMinBlockZ()+8);
        f.check(f.level.areEntitiesActuallyLoadedAndTicking(chunk),"Spawner fixture uses the native ticking chunk");
        var state=f.level.getBlockState(pos);var oldEntity=f.level.getBlockEntity(pos);
        CompoundTag saved=oldEntity==null?null:oldEntity.saveWithFullMetadata(f.level.registryAccess());
        var area=new net.minecraft.world.phys.AABB(pos).inflate(3);
        var existing=new HashSet<UUID>();
        f.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area).forEach(e->existing.add(e.getUUID()));
        var held=f.player.getMainHandItem().copy();
        f.player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        f.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> cancel=e->{
            if(e.getLevel()==f.level&&e.getPos().equals(pos))e.setCanceled(true);
        };
        try {
            f.level.setBlockAndUpdate(pos,net.goui.cosmicdungeon.block.ModBlocks.COSMIC_MOB_SPAWNER.get().defaultBlockState());
            NeoForge.EVENT_BUS.addListener(cancel);
            try {
                f.check(!f.player.gameMode.destroyBlock(pos)&&f.stats().spawners()==0,
                        "Canceled native harvest cannot count a spawner");
            }finally {NeoForge.EVENT_BUS.unregister(cancel);}
            f.check(f.player.gameMode.destroyBlock(pos)
                    &&!f.level.getBlockState(pos).is(net.goui.cosmicdungeon.block.ModBlocks.COSMIC_MOB_SPAWNER.get())
                    &&f.stats().spawners()==1,"Confirmed native harvest records exactly one spawner");
            f.player.gameMode.destroyBlock(pos);
            f.check(f.stats().spawners()==1,"Repeated empty-position break cannot duplicate spawner credit");
        } finally {
            f.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);
            f.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,area)
                    .stream().filter(e->!existing.contains(e.getUUID())).forEach(Entity::discard);
            f.level.setBlockAndUpdate(pos,state);
            if(saved!=null&&f.level.getBlockEntity(pos)!=null)f.level.getBlockEntity(pos).loadWithComponents(
                    net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess(),saved));
        }
    }
    public static void lifecycle(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-301)) {
            var server=f.level.getServer();
            f.zombie().hurtServer(f.level,f.player.damageSources().playerAttack(f.player),100);
            DungeonMetricsService.checkpoint(server,f.run);
            var store=DungeonMetricsService.archive(server);
            var active=store.read(f.id);
            f.check(active!=null&&active.outcome().equals("ACTIVE"),"Active instance has a durable checkpoint");
            f.check(active.coverage().equals("COMPLETE"),"Fresh recorded run has complete coverage");
            var expectedId=active.instanceId();
            f.data.clearRun(f.id);
            DungeonMetricsService.resume(server);
            f.check(f.stats().kills()==1&&f.stats().mobs().getOrDefault("minecraft:zombie",0L)==1,
                    "Restart restores ACTIVE archive counter minimums when SavedData lags behind");
            DungeonMetricsService.begin(server,f.run,false);
            f.check(DungeonMetricsService.snapshot(server,f.run,"ACTIVE").instanceId().equals(expectedId),
                    "Resume retains stable instance identity");
            f.check(DungeonMetricsService.finish(server,f.run,"ABANDONED"),"Aborted run archives before cleanup");
            var finished=store.read(f.id);var total=store.totals(f.player.getUUID());
            f.check(finished.outcome().equals("ABANDONED")&&total.runs()==1&&total.aborted()==1
                    &&total.stats().kills()==1,"Aborted history retains true counters and one lifetime projection");
            f.data.clearRun(f.id);
            f.check(store.read(f.id).equals(finished),"Run cleanup cannot erase the archival statistics");
            f.check(DungeonMetricsService.finish(server,f.run,"ABANDONED"),"Repeated cleanup observes durable completion");
            f.check(store.totals(f.player.getUUID()).equals(total),"Repeated cleanup never double totalizes");
            var reopened=new RunMetricsStore(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("Dungeon Instance Metrics"));
            reopened.recover();
            f.check(reopened.read(f.id).equals(finished)&&reopened.totals(f.player.getUUID()).equals(total),
                    "Restart recovers final instance and matching totals");
        } catch(java.io.IOException error) {throw new IllegalStateException(error);}
        h.succeed();
    }
}
