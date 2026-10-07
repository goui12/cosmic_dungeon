package net.goui.cosmicdungeon.npc.tamsin;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.*;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.*;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.effect.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.*;

public final class PartyInspectionGameTests {
    private PartyInspectionGameTests() {}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final long id;
        final ServerPlayer viewer,subject;final D1RunData data;
        final DungeonRunRegistryData.RunRecord run;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs;
        final Map<UUID,ServerPlayer> players;final List<ServerPlayer> online;
        final io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel();
        @SuppressWarnings("unchecked") Fixture(GameTestHelper h,long id) {
            this.h=h;this.id=id;level=h.getLevel();
            viewer=player("InspectViewer");subject=player("InspectSubject");
            run=new DungeonRunRegistryData.RunRecord(id,"dungeon_1","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,
                    List.of(viewer.getUUID(),subject.getUUID()),List.of(),List.of());
            data=D1RunData.get(level.getServer());
            try {
                var field=DungeonRunRegistryData.class.getDeclaredField("runsById");field.setAccessible(true);
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)field.get(DungeonRunRegistryData.get(level.getServer()));
                field=PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
                players=(Map<UUID,ServerPlayer>)field.get(level.getServer().getPlayerList());
                field=PlayerList.class.getDeclaredField("players");field.setAccessible(true);
                online=(List<ServerPlayer>)field.get(level.getServer().getPlayerList());
            }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            if(runs.containsKey(id))throw new IllegalStateException("Fixture collision");
            runs.put(id,run);
            for(var p:List.of(viewer,subject)){players.put(p.getUUID(),p);online.add(p);}
        }
        ServerPlayer player(String name) {
            var server=level.getServer();var profile=new GameProfile(UUID.randomUUID(),name);
            var player=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND) {
                @Override public io.netty.channel.Channel channel(){return channel;}
                @Override public boolean isConnected(){return true;}
            };
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)) {
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener){}
            };
            var tag=new CompoundTag();tag.putString(ClassData.KEY_CLASS_ID,"metalmancer");tag.putBoolean("run_temp",true);
            player.getPersistentData().put(ClassData.ROOT_TAG,tag);
            player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(.5,5,.5)));
            return player;
        }
        PartyInspectionPayloads.View view(){return PartyInspectionService.snapshot(viewer,new PartyInspectionPayloads.Request(id,1,subject.getUUID()));}
        double metric(String key){return RunMemberStats.value(data,id,subject.getUUID(),key);}
        void check(boolean test,String message){h.assertTrue(test,Component.literal(message));}
        @Override public void close(){
            for(var p:List.of(viewer,subject)){players.remove(p.getUUID());online.remove(p);p.discard();}
            runs.remove(id);data.clearRun(id);channel.finishAndReleaseAll();
        }
    }
    public static void access(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-200)) {
            f.subject.setHealth(12);f.subject.addEffect(new MobEffectInstance(MobEffects.STRENGTH,200,1));
            f.data.setCount(f.id,"kills:"+f.subject.getUUID(),7);
            var root=f.subject.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG);
            var mm=new CompoundTag();mm.putLong("cd_magnet_l",f.level.getGameTime()+40);root.put("metalmancer",mm);
            f.subject.getPersistentData().put(ClassData.ROOT_TAG,root);
            var view=f.view();
            f.check(view.allowed()&&view.vitals().health()==12&&view.vitals().effects().size()==1,"Same-run member includes native HP/effects");
            f.check(view.readings().contains(new PartyInspectionPayloads.Reading("Run kills","7")),"Run kills use existing current-run counters");
            f.check(view.readings().contains(new PartyInspectionPayloads.Reading("Magnet attack","2s")),"Cooldown uses existing authoritative action timer");
            f.check(!PartyInspectionService.snapshot(f.viewer,new PartyInspectionPayloads.Request(f.id,2,UUID.randomUUID())).allowed(),"Outsider UUID cannot be inspected");
            f.check(!PartyInspectionService.snapshot(f.viewer,new PartyInspectionPayloads.Request(f.id-1,2,f.subject.getUUID())).allowed(),"Stale run denied");
            f.players.remove(f.subject.getUUID());
            f.check(f.view().vitals().equals(PartyVitals.OFFLINE)&&f.view().readings().stream().noneMatch(r->r.label().equals("Magnet attack")),"Offline response clears live timers and health");
            f.players.put(f.subject.getUUID(),f.subject);
            f.runs.put(f.id,f.run.withCompletionExited(f.subject.getUUID()));
            f.check(!f.view().allowed(),"Exited target denied");
            f.runs.put(f.id,f.run.withCompletionExited(f.viewer.getUUID()));
            f.check(!f.view().allowed(),"Exited viewer denied");
            f.runs.put(f.id,f.run.withState(DungeonRunState.RESETTING,DungeonResetReason.ABANDONED));
            f.check(!f.view().allowed(),"Reset denies stale inspection");
        }
        h.succeed();
    }
    @SuppressWarnings("unchecked") public static void counters(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-201)) {
            f.subject.setHealth(10);f.subject.heal(50);
            f.check(f.metric("healing_received")==10,"Native overheal records only actual health restored");
            java.util.function.Consumer<LivingHealEvent> cancelHeal=e->{if(e.getEntity()==f.subject)e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(cancelHeal);
            try {f.subject.setHealth(10);f.subject.heal(5);f.check(f.metric("healing_received")==10,"Cancelled heal gives no credit");}
            finally {NeoForge.EVENT_BUS.unregister(cancelHeal);}
            var enemy=new Zombie(EntityType.ZOMBIE,f.level);enemy.setPos(f.subject.position().add(2,0,0));enemy.setNoAi(true);
            try {
                enemy.hurtServer(f.level,f.subject.damageSources().playerAttack(f.subject),4);
                f.check(f.metric("damage")>0,"Actual hostile damage records run damage");
            } finally {enemy.discard();}
            java.util.function.Consumer<LivingDeathEvent> cancelDeath=e->{if(e.getEntity()==f.subject)e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(cancelDeath);
            try {f.subject.setHealth(0);f.subject.die(f.subject.damageSources().generic());f.check(f.metric("deaths")==0,"Cancelled native death is not counted");}
            finally {NeoForge.EVENT_BUS.unregister(cancelDeath);}
            f.subject.setHealth(0);f.subject.die(f.subject.damageSources().generic());
            f.check(f.metric("deaths")==1,"Real native death is counted");
            RunMemberStats.died(f.subject);f.check(f.metric("deaths")==1,"Duplicate death callback does not count twice");
            try {
                var field=D1RunData.class.getDeclaredField("CODEC");field.setAccessible(true);var codec=(Codec<D1RunData>)field.get(null);
                var encoded=codec.encodeStart(JsonOps.INSTANCE,f.data).getOrThrow();
                var decoded=codec.parse(JsonOps.INSTANCE,encoded).getOrThrow();
                f.check(RunMemberStats.value(decoded,f.id,f.subject.getUUID(),"healing_received")==10,"Counters survive save/load round trip");
                f.check(RunMemberStats.value(decoded,f.id-1,f.subject.getUUID(),"healing_received")==0,"Another run cannot inherit counters");
                var legacy=codec.parse(JsonOps.INSTANCE,com.google.gson.JsonParser.parseString("{\"runs\":[]}")).getOrThrow();
                f.check(RunMemberStats.value(legacy,f.id,f.subject.getUUID(),"damage")==0,"Old save defaults are zero");
            }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            f.data.clearRun(f.id);
            f.check(f.metric("deaths")==0&&f.metric("damage")==0&&f.metric("healing_received")==0,"Existing run retirement clears all new counters");
        }
        h.succeed();
    }
}
