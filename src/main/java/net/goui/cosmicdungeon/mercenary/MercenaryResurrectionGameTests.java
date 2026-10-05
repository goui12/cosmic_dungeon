package net.goui.cosmicdungeon.mercenary;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.network.PartyPayloads;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Synchronous CI fixtures use native death, clone and respawn without a game client or production bypass. */
public final class MercenaryResurrectionGameTests {
    private MercenaryResurrectionGameTests(){}
    private static final class Fixture implements AutoCloseable{
        final GameTestHelper helper;final ServerLevel level;final long id;
        final ServerPlayer player;final MercenaryContract contract;final MercenaryEntity merc;
        final DungeonRunRegistryData.RunRecord run;final D1RunData data;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs;final Map<UUID,ServerPlayer> players;
        final List<ServerPlayer> online;final Vec3 origin;
        final io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel();
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper,long id,boolean nativePlayer){
            this.helper=helper;this.id=id;level=helper.getLevel();var server=level.getServer();
            origin=helper.absoluteVec(new Vec3(.125,10.75,.625));
            var profile=new GameProfile(UUID.randomUUID(),"ResurrectionTest");
            var fake=FakePlayerFactory.get(level,profile);
            player=nativePlayer?new ServerPlayer(server,level,profile,ClientInformation.createDefault()):fake;
            if(nativePlayer){
                // FakePlayer's shared dummy connection has no channel. Native clone hooks query attributes.
                // A private in-memory channel supplies those attributes without sockets or authenticated clients.
                var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
                    @Override public io.netty.channel.Channel channel(){return channel;}
                };
                net.neoforged.neoforge.network.registration.ChannelAttributes.setConnectionType(connection,net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
                net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,net.neoforged.neoforge.network.registration.NetworkPayloadSetup.empty());
                player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,player,
                        net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
                    @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
                    @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener){}
                    @Override public void resetPosition(){}
                    @Override public void teleport(double x,double y,double z,float yaw,float pitch){}
                };
            }else player.connection=fake.connection;
            player.connection.player=player;
            player.setPos(origin);player.setYRot(137.5f);player.setXRot(-31.25f);player.setHealth(player.getMaxHealth());
            var root=new CompoundTag();root.putString(ClassData.KEY_CLASS_ID,"theurgist");root.putBoolean("run_temp",true);
            player.getPersistentData().put(ClassData.ROOT_TAG,root);
            contract=new MercenaryContract(UUID.randomUUID(),player.getUUID(),"theurgist",2,50);
            run=new DungeonRunRegistryData.RunRecord(id,"dungeon_1","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,List.of(player.getUUID()),List.of(),List.of())
                    .withMercenaries(List.of(contract));
            var registry=DungeonRunRegistryData.get(server);data=D1RunData.get(server);
            try{
                var rf=DungeonRunRegistryData.class.getDeclaredField("runsById");rf.setAccessible(true);
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)rf.get(registry);
                var pf=PlayerList.class.getDeclaredField("playersByUUID");pf.setAccessible(true);
                players=(Map<UUID,ServerPlayer>)pf.get(server.getPlayerList());
                var lf=PlayerList.class.getDeclaredField("players");lf.setAccessible(true);
                online=(List<ServerPlayer>)lf.get(server.getPlayerList());
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Native fixture unavailable",error);}
            if(runs.containsKey(id)||players.containsKey(player.getUUID()))throw new IllegalStateException("Fixture collision");
            runs.put(id,run);players.put(player.getUUID(),player);online.add(player);
            merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);merc.initialize(id,contract);
            merc.setNoAi(true);merc.setPos(origin.add(1,0,0));level.addFreshEntity(merc);
        }
        void level(int successes){data.setCount(id,MercenarySkills.key(contract.id(),MercenarySkill.POSITIVE_POTIONS),successes);}
        PartyPayloads.Resurrection view(){return MercenaryResurrection.snapshot(level.getServer(),runs.get(id),contract,player.getUUID());}
        void check(boolean condition,String message){helper.assertTrue(condition,Component.literal(message));}
        @Override public void close(){
            var current=players.remove(player.getUUID());online.removeIf(p->p.getUUID().equals(player.getUUID()));
            if(current!=null&&current!=player)level.removePlayerImmediately(current,Entity.RemovalReason.DISCARDED);
            if(!player.isRemoved())level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);
            merc.discard();runs.remove(id);data.clearRun(id);channel.finishAndReleaseAll();
        }
    }
    public static void eligibility(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-120,false)){
            f.player.setHealth(0);MercenaryResurrection.died(f.player);
            f.level(44);f.data.setCount(f.id,MercenarySkills.key(f.contract.id(),MercenarySkill.NEGATIVE_POTIONS),1225);
            f.check(f.view().seconds()==-1&&!f.view().offered(),"Positive level 9 stays locked even with Negative level 50");
            f.level(45);
            f.check(f.view().offered()&&MercenaryBrain.hirer(f.merc)==null,"Positive level 10 offers resurrection to its own dead hirer without weakening combat gate");
            f.merc.die(f.merc.damageSources().generic());
            f.check(!f.view().offered(),"Dormant Theurgist cannot resurrect");
            f.merc.resumeAfterRest();f.check(f.view().offered(),"Living Theurgist regains offer");
            f.players.remove(f.player.getUUID());f.check(!f.view().offered(),"Offline hirer cannot offer");
            f.players.put(f.player.getUUID(),f.player);
            f.runs.put(f.id,f.run.withState(DungeonRunState.RESETTING,DungeonResetReason.ABANDONED));
            f.check(!f.view().offered(),"Forfeit reset withdraws offer");
            f.runs.put(f.id,f.run.withCompletionExited(f.player.getUUID()));
            f.check(!f.view().offered(),"Completed member cannot accept");
            f.runs.put(f.id,f.run);f.data.setValue(f.id,"watson_outcome","success");
            f.check(!f.view().offered(),"Sealed outcome cannot resurrect");
            f.data.setValue(f.id,"watson_outcome",null);
            f.player.setHealth(20);f.check(!f.view().offered(),"Ordinary respawn or living target withdraws offer");
            helper.succeed();
        }
    }
    public static void respawn(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-121,true)){
            var server=f.level.getServer();f.level(45);
            var keep=f.level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);boolean previous=keep.get();
            java.util.function.Consumer<LivingDeathEvent> cancel=event->{if(event.getEntity()==f.player)event.setCanceled(true);};
            keep.set(false,server);
            var drops=new ArrayList<ItemEntity>();
            try{
                NeoForge.EVENT_BUS.addListener(cancel);
                try{
                    f.player.setHealth(0);f.player.die(f.player.damageSources().generic());
                    f.check(MercenaryResurrectionState.death(f.data,f.id,f.player.getUUID())==null,"Cancelled native death never creates an offer");
                }finally{NeoForge.EVENT_BUS.unregister(cancel);}
                f.player.setHealth(20);
                DungeonLifecycleService.setPlayerRespawnTo(f.player,f.level,f.player.blockPosition().offset(20,0,0),24,0);
                var normalRespawn=f.player.getRespawnConfig();
                f.player.getInventory().setItem(0,new ItemStack(Items.DIAMOND,2));
                f.player.setHealth(0);f.player.die(f.player.damageSources().generic());
                var death=MercenaryResurrectionState.death(f.data,f.id,f.player.getUUID());
                f.check(death!=null&&death.position().equals(f.origin),"Native final death records exact fractional coordinates");
                drops.addAll(f.level.getEntitiesOfClass(ItemEntity.class,f.player.getBoundingBox().inflate(4),
                        item->item.getItem().is(Items.DIAMOND)));
                f.check(drops.stream().mapToInt(item->item.getItem().getCount()).sum()==2,"Native death drops the original inventory once");
                f.player.setPos(f.origin.add(0,-2,0));
                var request=new PartyPayloads.Resurrect(f.id,f.contract.id(),death.id());
                MercenaryResurrection.accept(f.player,request);
                var revived=f.players.get(f.player.getUUID());
                f.check(revived!=f.player&&revived.isAlive()&&revived.connection.player==revived,"Native replacement and connection are both updated");
                f.check(revived.position().equals(f.origin)&&revived.getYRot()==137.5f&&revived.getXRot()==-31.25f,"Resurrection uses death position and rotation, not falling body or bed");
                f.check(normalRespawn.equals(revived.getRespawnConfig()),"Future bed/respawn setting is unchanged");
                f.check(ClassData.getClassId(revived).equals("theurgist")
                        &&revived.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).getBooleanOr("run_temp",false),"Native clone preserves class and active run data");
                f.check(revived.getInventory().countItem(Items.DIAMOND)==0
                        &&drops.stream().mapToInt(item->item.getItem().getCount()).sum()==2,"Resurrection does not copy dropped inventory");
                f.check(f.view().seconds()==180&&!f.view().offered(),"Cooldown immediately appears for the living player");
                f.check(MercenaryResurrection.destination(f.player)==null,"Forced destination cannot leak to ordinary respawn");
                MercenaryResurrection.accept(f.player,request);MercenaryResurrection.accept(revived,request);
                f.check(f.players.get(f.player.getUUID())==revived,"Duplicate and stale old-player requests cannot respawn twice");
                var damage=new EntityInvulnerabilityCheckEvent(revived,revived.damageSources().fellOutOfWorld(),false);
                NeoForge.EVENT_BUS.post(damage);f.check(damage.isInvulnerable(),"Five-second protection includes void damage");
                long tick=server.overworld().getGameTime();
                revived.getPersistentData().putLong("cosmicdungeon_resurrection_protected_until",tick);
                var expired=new EntityInvulnerabilityCheckEvent(revived,revived.damageSources().generic(),false);
                NeoForge.EVENT_BUS.post(expired);f.check(!expired.isInvulnerable(),"Expired protection no longer overrides native vulnerability");
                helper.succeed();
            }catch(RuntimeException error){
                com.mojang.logging.LogUtils.getLogger().error("Native resurrection fixture failed",error);throw error;
            }finally{keep.set(previous,server);drops.forEach(Entity::discard);}
        }
    }
}
