package net.goui.cosmicdungeon.playerclass.skill;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.gametest.DurableRecoveryGameTestSupport;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.progression.PlayerProgressionData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Real native damage/death and network action paths; synchronous isolated server fixtures only. */
public final class ChargedClassGameTests {
    private ChargedClassGameTests(){}
    static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerLevel level;final long run;final Vec3 origin;final ServerPlayer player;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs;final Map<UUID,ServerPlayer> players;
        final List<Entity> entities=new ArrayList<>();final List<Packet<?>> packets=new ArrayList<>();
        final io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel();
        final CompoundTag progressionBefore;
        @SuppressWarnings("unchecked") Fixture(GameTestHelper h,long run,String cls){
            this.h=h;this.run=run;level=h.getLevel();var server=level.getServer();
            var anchor=h.absolutePos(BlockPos.ZERO);var chunk=new ChunkPos(anchor);
            origin=new Vec3(chunk.getMinBlockX()+8.5,anchor.getY()+10,chunk.getMinBlockZ()+8.5);
            var profile=new GameProfile(UUID.randomUUID(),"ChargedSkillTest");
            player=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
                @Override public io.netty.channel.Channel channel(){return Fixture.this.channel;}
                @Override public boolean isConnected(){return true;}
            };
            net.neoforged.neoforge.network.registration.ChannelAttributes.setConnectionType(connection,
                    net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
            net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,
                    net.neoforged.neoforge.network.registration.NetworkPayloadSetup.empty());
            player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
                @Override public void send(Packet<?> packet){packets.add(packet);}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){send(packet);}
                @Override public void resetPosition(){}
                @Override public void teleport(double x,double y,double z,float yaw,float pitch){}
            };
            // GameTest servers default to Creative, whose damage bypasses native invulnerability.
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setPos(origin);player.setHealth(20);classId(cls);
            check(!player.isCreative(),"Fixture uses production survival damage semantics");
            try{
                var rf=DungeonRunRegistryData.class.getDeclaredField("runsById");rf.setAccessible(true);
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)rf.get(DungeonRunRegistryData.get(server));
                var pf=PlayerList.class.getDeclaredField("playersByUUID");pf.setAccessible(true);
                players=(Map<UUID,ServerPlayer>)pf.get(server.getPlayerList());
            }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
            check(!runs.containsKey(run)&&!players.containsKey(player.getUUID()),"Unique charged fixture");
            runs.put(run,new DungeonRunRegistryData.RunRecord(run,"dungeon_1","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,List.of(player.getUUID()),List.of(),List.of()));
            players.put(player.getUUID(),player);level.addNewPlayer(player);
            progressionBefore=(CompoundTag)DurableRecoveryGameTestSupport.image(PlayerProgressionData.class,PlayerProgressionData.get(server));
            check(ClassSkills.run(player)!=null,"Native actor is active");
        }
        void classId(String cls){
            var tag=player.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();tag.putString(ClassData.KEY_CLASS_ID,cls);
            player.getPersistentData().put(ClassData.ROOT_TAG,tag);
        }
        <T extends Mob> T mob(EntityType<T> type,double x){
            var mob=type.create(level,EntitySpawnReason.COMMAND);check(mob!=null,"Native target created");
            mob.setNoAi(true);mob.setPos(origin.add(x,0,0));mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);mob.setHealth(1024);
            entities.add(mob);check(level.addFreshEntity(mob),"Native target admitted");return mob;
        }
        void hit(Mob mob,float damage){mob.invulnerableTime=0;mob.hurtServer(level,player.damageSources().playerAttack(player),damage);}
        void fill(SignatureSkill skill){while(ChargedClassSkills.charge(player,skill)<ChargedSkillState.limit(skill))
            check(ChargedClassSkills.addCharge(player,skill),"Fixture charge advances");}
        void press(){press(ClassSkillSlot.PRIMARY);}
        void press(ClassSkillSlot slot){
            ClassSkillActions.forget(player);ClassSkillActions.sync(player,true);
            try{
                var sf=ClassSkillActions.class.getDeclaredField("SESSIONS");sf.setAccessible(true);
                var session=(ClassSkillActions.Session)((Map<?,?>)sf.get(null)).get(player);
                check(session!=null&&session.sent!=null,"Native capability snapshot sent");
                ClassSkillActions.action(player,new net.goui.cosmicdungeon.network.ClassSkillPayloads.Action(
                        run,session.sent.revision(),ClassData.getClassId(player),slot));
            }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        }
        boolean message(String text){return packets.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket c
                &&c.overlay()&&c.content().getString().equals(text));}
        void check(boolean ok,String why){h.assertTrue(ok,Component.literal(why));}
        @SuppressWarnings("unchecked") @Override public void close(){
            entities.forEach(Entity::discard);ClassSkillActions.forget(player);
            level.removePlayerImmediately(player,Entity.RemovalReason.DISCARDED);players.remove(player.getUUID());runs.remove(run);
            D1RunData.get(level.getServer()).clearRun(run);channel.finishAndReleaseAll();
            try{
                var f=PlayerProgressionData.class.getDeclaredField("CODEC");f.setAccessible(true);
                var codec=(com.mojang.serialization.Codec<PlayerProgressionData>)f.get(null);
                level.getServer().overworld().getDataStorage().set(PlayerProgressionData.TYPE,
                        codec.parse(NbtOps.INSTANCE,progressionBefore).getOrThrow());
                check(PlayerProgressionData.get(level.getServer()).flushVerified(),"Restore native progression fixture");
            }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
        }
    }
    public static void lightning(GameTestHelper h){
        try(var f=new Fixture(h,Long.MAX_VALUE-14001,"dragoon")){
            var first=f.mob(EntityType.ZOMBIE,1);var second=f.mob(EntityType.ZOMBIE,2);
            f.player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));f.hit(first,1);
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.CHAIN_LIGHTNING)==0,"Wrong weapon earns no lightning charge");
            f.player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.TRIDENT));
            first.setInvulnerable(true);f.hit(first,1);first.setInvulnerable(false);
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.CHAIN_LIGHTNING)==0,"Denied native hit earns no charge");
            for(int i=0;i<9;i++)f.hit(first,1);
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.CHAIN_LIGHTNING)==9&&second.getHealth()==1024,"9 valid hits, no random proc");
            var shot=new ThrownTrident(f.level,f.player,new ItemStack(Items.TRIDENT));shot.setPos(f.origin);f.entities.add(shot);
            f.check(f.level.addFreshEntity(shot),"Native trident admitted and captured");
            first.invulnerableTime=0;first.hurtServer(f.level,f.player.damageSources().trident(shot,f.player),1);
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.CHAIN_LIGHTNING)==10&&!ChargedClassSkills.armed(f.player),
                    "Thrown trident fills 10 but never auto-arms a player");
            f.press();f.check(ChargedClassSkills.armed(f.player)&&f.message("Chain Lightning armed for your next trident strike."),
                    "Generic primary network action arms and confirms skill");
            second.setInvulnerable(true);f.hit(first,1);
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.CHAIN_LIGHTNING)==10&&ChargedClassSkills.armed(f.player),
                    "Wholly ineffective chain retains armed charge");
            second.setInvulnerable(false);f.hit(first,1);
            f.check(second.getHealth()<1024&&ChargedClassSkills.charge(f.player,SignatureSkill.CHAIN_LIGHTNING)==0
                    &&!ChargedClassSkills.armed(f.player),"Next native strike releases and secondary damage cannot refill");
            f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.CHAIN_LIGHTNING)==1,"One effective chain earns one proficiency");
            f.fill(SignatureSkill.CHAIN_LIGHTNING);var before=second.getHealth();f.hit(first,1);
            f.check(second.getHealth()==before&&ChargedClassSkills.charge(f.player,SignatureSkill.CHAIN_LIGHTNING)==10,"Full unarmed bar holds without random proc/overflow");
        }
        h.succeed();
    }
    public static void smite(GameTestHelper h){
        try(var f=new Fixture(h,Long.MAX_VALUE-14002,"judicator")){
            f.player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.MACE));
            var cancelled=f.mob(EntityType.ZOMBIE,1);cancelled.setHealth(1);
            java.util.function.Consumer<LivingDeathEvent> cancel=e->{if(e.getEntity()==cancelled)e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(cancel);
            try{f.hit(cancelled,100);f.check(ChargedClassSkills.charge(f.player,SignatureSkill.SMITE)==0,"Cancelled native death cannot charge Smite");}
            finally{NeoForge.EVENT_BUS.unregister(cancel);cancelled.discard();}
            var admin=f.mob(EntityType.ZOMBIE,1);f.hit(admin,1);
            admin.hurtServer(f.level,admin.damageSources().genericKill(),Float.MAX_VALUE);
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.SMITE)==0,"Administrative finish cannot charge remembered attacker");
            for(int i=0;i<10;i++){var target=f.mob(EntityType.ZOMBIE,1);f.hit(target,2000);target.discard();}
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.SMITE)==10&&SignatureSkills.playerProgress(f.player,SignatureSkill.SMITE)==0,
                    "Ten undead deaths charge Smite without granting activation proficiency");
            f.press();
            f.check(f.message(ChargedClassSkills.EMPTY_SMITE)&&ChargedClassSkills.charge(f.player,SignatureSkill.SMITE)==10,
                    "Empty G activation preserves all charge and sends the exact required overlay");
            var undead=f.mob(EntityType.ZOMBIE,1);var hostile=f.mob(EntityType.CREEPER,1);
            f.press();
            f.check(undead.getHealth()<1024&&hostile.getHealth()==1024&&ChargedClassSkills.charge(f.player,SignatureSkill.SMITE)==0,
                    "Generic primary action damages only undead and spends once");
            f.check(SignatureSkills.playerProgress(f.player,SignatureSkill.SMITE)==1,"Successful player Smite earns exactly one activation");
            var data=PlayerProgressionData.get(f.level.getServer());data.addSignatureProgress(f.player.getUUID(),SignatureSkill.SMITE,59,60);
            f.fill(SignatureSkill.SMITE);undead.invulnerableTime=0;f.press();
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.SMITE)==0&&SignatureSkills.playerLevel(f.player,SignatureSkill.SMITE)==4,
                    "Maximum proficiency still consumes a successful Smite");
            var interfering=f.mob(EntityType.SKELETON,8);
            undead.invulnerableTime=0;undead.hurtServer(f.level,interfering.damageSources().mobAttack(interfering),1);
            f.check(undead.getKillCredit()==f.player,"Native player credit survives an intervening hostile hit");
            undead.setHealth(1);undead.invulnerableTime=0;
            undead.hurtServer(f.level,undead.damageSources().generic(),100);
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.SMITE)==0,
                    "Environmental finish of remembered secondary damage cannot refill Smite");
            var recovered=f.mob(EntityType.ZOMBIE,1);
            SecondarySkillDamage.run(()->{f.hit(recovered,1);return 1;});
            f.hit(recovered,1);recovered.setHealth(1);recovered.invulnerableTime=0;
            recovered.hurtServer(f.level,recovered.damageSources().generic(),100);
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.SMITE)==1,
                    "Later effective primary hit replaces secondary provenance for native kill credit");
        }
        h.succeed();
    }
    public static void persistence(GameTestHelper h){
        try(var f=new Fixture(h,Long.MAX_VALUE-14003,"dragoon")){
            f.fill(SignatureSkill.CHAIN_LIGHTNING);f.press();
            var data=D1RunData.get(f.level.getServer());data.setValue(f.run,"future_charge_field","retained");
            f.check(data.flushVerified(),"Native charge store saved");
            data=DurableRecoveryGameTestSupport.reload(f.level.getServer(),D1RunData.class,"cosmicdungeon_d1_objectives_v1");
            f.check(ChargedClassSkills.charge(f.player,SignatureSkill.CHAIN_LIGHTNING)==10&&ChargedClassSkills.armed(f.player),
                    "Native SavedData reload retains charge and armed state");
            f.classId("judicator");f.check(ChargedClassSkills.charge(f.player,SignatureSkill.SMITE)==0&&!ChargedClassSkills.armed(f.player),
                    "Class switch cannot spend another class bar");
            f.classId("dragoon");f.check(ChargedClassSkills.armed(f.player),"Returning to class retains its run bar");
            f.player.setHealth(0);f.check(!ChargedClassSkills.arm(f.player)&&ClassSkillActions.slots(f.player).stream().noneMatch(s->s.enabled()),
                    "Dead players cannot activate retained charge");
            f.player.setHealth(20);f.check(ChargedClassSkills.armed(f.player),"Recovery retains the run bar");
            f.check(data.values(f.run,"future_charge_field").equals(List.of("retained")),"Unknown run entries survive charge writes/reload");
            data.clearRun(f.run);f.check(ChargedClassSkills.charge(f.player,SignatureSkill.CHAIN_LIGHTNING)==0,
                    "Normal run cleanup removes all bars");
        }
        h.succeed();
    }
}
