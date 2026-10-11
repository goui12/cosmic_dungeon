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

public final class GroupResourceConsentGameTests {
    private GroupResourceConsentGameTests() {}
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
    public static void eligibility(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-230)) {
            var key="resource_sharing_v1:"+f.subject.getUUID();
            f.check(!GroupResourceConsent.canContribute(f.viewer,f.subject,f.id),"Missing choice never authorizes a donor");
            f.check(GroupResourceConsent.choose(f.subject,f.id,Long.toString(f.id),true)==null,"Personal YES accepted");
            f.subject.setPos(f.viewer.position().add(500,0,0));
            f.check(GroupResourceConsent.canContribute(f.viewer,f.subject,f.id),"Same-run consenting donor works at any distance");
            f.check(!GroupResourceConsent.canContribute(f.viewer,f.subject,f.id-1),"Stale run cannot authorize a debit");
            f.check(GroupResourceConsent.choose(f.subject,f.id,"stale-group",false)!=null,"Wrong context cannot mutate choice");
            f.check(GroupResourceConsent.choose(f.subject,f.id-1,Long.toString(f.id),false)!=null,"Wrong revision cannot mutate choice");
            f.players.remove(f.subject.getUUID());
            f.check(!GroupResourceConsent.canContribute(f.viewer,f.subject,f.id),"Retained offline player reference cannot donate");
            f.players.put(f.subject.getUUID(),f.subject);
            f.subject.setHealth(0);
            f.check(!GroupResourceConsent.canContribute(f.viewer,f.subject,f.id),"Dead donor cannot donate");
            f.subject.setHealth(20);
            f.runs.put(f.id,f.run.withCompletionExited(f.subject.getUUID()));
            f.check(!GroupResourceConsent.canContribute(f.viewer,f.subject,f.id),"Exited donor invalidated immediately");
            f.runs.put(f.id,f.run.withState(DungeonRunState.RESETTING,DungeonResetReason.ABANDONED));
            f.check(!GroupResourceConsent.canContribute(f.viewer,f.subject,f.id),"Reset invalidates authorization");
            f.runs.put(f.id,f.run);
            f.data.setValue(f.id,"watson_outcome","success");
            f.check(!GroupResourceConsent.canContribute(f.viewer,f.subject,f.id),"Sealed run cannot donate");
            f.data.setValue(f.id,"watson_outcome","");
            f.check(GroupResourceConsent.choose(f.subject,f.id,Long.toString(f.id),false)==null,"Personal NO accepted");
            f.check(!GroupResourceConsent.canContribute(f.viewer,f.subject,f.id),"Opt-out revokes authorization immediately");
            f.check(f.data.values(f.id,key).equals(List.of("NO")),"Only subject choice was written");
            D1PartyService.action(f.viewer,new PartyPayloads.Action(-1,f.id,"resources_yes",Long.toString(f.id)));
            f.check(GroupResourceConsent.choice(f.data,f.id,f.viewer.getUUID()).equals("YES"),"HUD packet route updates only sender");
            f.check(GroupResourceConsent.choice(f.data,f.id,f.subject.getUUID()).equals("NO"),"Sender cannot toggle another member");
        }
        h.succeed();
    }
    @SuppressWarnings("unchecked") public static void persistence(GameTestHelper h) {
        try(var f=new Fixture(h,Long.MAX_VALUE-231)) {
            var lobby=new D1PartyLobby();var anchor=new D1PartyLobby.Anchor(UUID.randomUUID(),"minecraft:overworld",0);
            lobby.create(f.viewer.getUUID(),anchor,0,"Consent");
            var party=lobby.party(f.viewer.getUUID());
            lobby.resourceSharing(f.viewer.getUUID(),party.revision(),party.id().toString(),true);
            lobby.invite(f.viewer.getUUID(),f.subject.getUUID(),anchor,0,100,6);
            lobby.accept(f.subject.getUUID(),lobby.invitation(f.subject.getUUID()).token(),1);
            lobby.joinAccepted(f.subject.getUUID(),2,6,true);
            lobby.resourceSharing(f.subject.getUUID(),party.revision(),party.id().toString(),false);
            f.data.setValue(f.id,"future_existing_key","retain me");
            GroupResourceConsent.carryIntoRun(f.level.getServer(),party);
            lobby.complete(party);
            f.check(GroupResourceConsent.choice(f.data,f.id,f.viewer.getUUID()).equals("YES"),"Entry carries leader YES");
            f.check(GroupResourceConsent.choice(f.data,f.id,f.subject.getUUID()).equals("NO"),"Entry carries member NO");
            try {
                var field=D1RunData.class.getDeclaredField("CODEC");field.setAccessible(true);var codec=(Codec<D1RunData>)field.get(null);
                var encoded=codec.encodeStart(JsonOps.INSTANCE,f.data).getOrThrow();
                var decoded=codec.parse(JsonOps.INSTANCE,encoded).getOrThrow();
                f.check(GroupResourceConsent.choice(decoded,f.id,f.viewer.getUUID()).equals("YES"),"Explicit choice survives restart codec");
                f.check(decoded.values(f.id,"future_existing_key").equals(List.of("retain me")),"Unrelated stored values retained");
                f.check(GroupResourceConsent.choice(decoded,f.id-1,f.viewer.getUUID()).equals("UNKNOWN"),"New run never inherits opt-in");
                var legacy=codec.parse(JsonOps.INSTANCE,com.google.gson.JsonParser.parseString("{\"runs\":[{\"run_id\":1}]}")).getOrThrow();
                f.check(GroupResourceConsent.choice(legacy,1,f.viewer.getUUID()).equals("UNKNOWN"),"Old shape requires explicit choice");
                decoded.setValue(f.id,"resource_sharing_v1:"+f.viewer.getUUID(),"FUTURE_VALUE");
                var future=codec.parse(JsonOps.INSTANCE,codec.encodeStart(JsonOps.INSTANCE,decoded).getOrThrow()).getOrThrow();
                f.check(GroupResourceConsent.choice(future,f.id,f.viewer.getUUID()).equals("UNKNOWN"),"Unknown value fails closed");
                f.check(future.values(f.id,"resource_sharing_v1:"+f.viewer.getUUID()).equals(List.of("FUTURE_VALUE")),"Unknown value preserved on load");
                f.data.clearRun(f.id);
                f.check(GroupResourceConsent.choice(f.data,f.id,f.subject.getUUID()).equals("UNKNOWN"),"Run retirement removes choice");
            }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        }
        h.succeed();
    }
}
