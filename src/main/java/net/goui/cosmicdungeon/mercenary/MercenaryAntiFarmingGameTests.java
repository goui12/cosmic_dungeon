package net.goui.cosmicdungeon.mercenary;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.gametest.DurableRecoveryGameTestSupport;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.dragoon.LightningCombat;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Synchronous native authority, real damage/death, splash and durable run-state regressions. */
public final class MercenaryAntiFarmingGameTests {
    private MercenaryAntiFarmingGameTests(){}
    private static final String TAG="cosmic_spawner_7_64_9";
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;final ServerLevel level;final long run;
        final Vec3 origin;final AABB bounds;final ServerPlayer owner;final MercenaryEntity merc;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs;final Map<UUID,ServerPlayer> players;
        final List<Entity> entities=new ArrayList<>();final List<Packet<?>> packets=new ArrayList<>();
        final Set<UUID> oldDrops=new HashSet<>();
        final io.netty.channel.embedded.EmbeddedChannel channel=new io.netty.channel.embedded.EmbeddedChannel();
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper,long run,String cls){
            this.helper=helper;this.run=run;level=helper.getLevel();var server=level.getServer();
            var anchor=helper.absolutePos(BlockPos.ZERO);var chunk=new ChunkPos(anchor);
            origin=new Vec3(chunk.getMinBlockX()+8.5,anchor.getY()+12,chunk.getMinBlockZ()+8.5);
            bounds=new AABB(origin,origin).inflate(7,4,7);
            level.getEntitiesOfClass(ItemEntity.class,bounds).forEach(e->oldDrops.add(e.getUUID()));
            level.getEntitiesOfClass(ExperienceOrb.class,bounds).forEach(e->oldDrops.add(e.getUUID()));
            var profile=new GameProfile(UUID.randomUUID(),"AntiFarmTest");
            owner=new ServerPlayer(server,level,profile,ClientInformation.createDefault());
            var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){
                @Override public io.netty.channel.Channel channel(){return Fixture.this.channel;}
                @Override public boolean isConnected(){return true;}
            };
            net.neoforged.neoforge.network.registration.ChannelAttributes.setConnectionType(connection,
                    net.neoforged.neoforge.network.connection.ConnectionType.NEOFORGE);
            net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,
                    net.neoforged.neoforge.network.registration.NetworkPayloadSetup.empty());
            owner.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,connection,owner,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
                @Override public void send(Packet<?> packet){packets.add(packet);}
                @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener){send(packet);}
                @Override public void resetPosition(){}
                @Override public void teleport(double x,double y,double z,float yaw,float pitch){}
            };
            owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);owner.setClientLoaded(true);owner.setOnGround(true);
            owner.setPos(origin);owner.setHealth(20);
            var tag=owner.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();
            tag.putString(ClassData.KEY_CLASS_ID,"theurgist");owner.getPersistentData().put(ClassData.ROOT_TAG,tag);
            var contract=new MercenaryContract(UUID.randomUUID(),owner.getUUID(),cls,2,50);
            try{
                var rf=DungeonRunRegistryData.class.getDeclaredField("runsById");rf.setAccessible(true);
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)rf.get(DungeonRunRegistryData.get(server));
                var pf=PlayerList.class.getDeclaredField("playersByUUID");pf.setAccessible(true);
                players=(Map<UUID,ServerPlayer>)pf.get(server.getPlayerList());
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            check(!runs.containsKey(run),"Unique native anti-farm run");
            runs.put(run,new DungeonRunRegistryData.RunRecord(run,"dungeon_1","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,
                    List.of(owner.getUUID()),List.of(),List.of()).withMercenaries(List.of(contract)));
            players.put(owner.getUUID(),owner);level.addNewPlayer(owner);
            merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);merc.initialize(run,contract);
            merc.setNoAi(true);merc.setPos(origin.add(-1,0,0));entities.add(merc);
            check(level.addFreshEntity(merc)&&MercenaryBrain.hirer(merc)==owner,"Admitted single hire with loaded survival owner");
        }
        D1RunData data(){return D1RunData.get(level.getServer());}
        <T extends Mob> T mob(EntityType<T> type,boolean tagged){
            var mob=type.create(level,EntitySpawnReason.COMMAND);check(mob!=null,"Native mob created");
            mob.setNoAi(true);mob.setPos(origin.add(2,0,0));
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);mob.setHealth(100);
            if(tagged)mob.addTag(TAG);entities.add(mob);check(level.addFreshEntity(mob),"Native mob admitted");return mob;
        }
        void hit(LivingEntity target,DamageSource source,float damage){
            target.invulnerableTime=0;target.hurtServer(level,source,damage);
        }
        String key(LivingEntity target){return MercenaryAntiFarming.spawnerKey(target);}
        int count(String key){return MercenaryAntiFarming.count(level,run,key);}
        void count(String key,int value){MercenaryAntiFarming.setCount(level,run,key,value);}
        long messages(String phrase){return packets.stream().filter(p->p instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket c
                &&c.content().getString().contains(phrase)).count();}
        void check(boolean condition,String message){helper.assertTrue(condition,Component.literal(message));}
        @Override public void close(){
            entities.forEach(Entity::discard);ClassSkillActions.forget(owner);
            players.remove(owner.getUUID());level.removePlayerImmediately(owner,Entity.RemovalReason.DISCARDED);
            level.getEntitiesOfClass(ItemEntity.class,bounds,e->!oldDrops.contains(e.getUUID())).forEach(Entity::discard);
            level.getEntitiesOfClass(ExperienceOrb.class,bounds,e->!oldDrops.contains(e.getUUID())).forEach(Entity::discard);
            runs.remove(run);data().clearRun(run);data().clearRun(run+1);channel.finishAndReleaseAll();
        }
    }
    public static void thresholds(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-17001,"judicator")){
            var victim=f.mob(EntityType.CREEPER,true);String key=f.key(victim);
            f.check(key!=null,"Existing native spawner tag supplies identity");f.count(key,29);
            f.hit(victim,f.merc.damageSources().mobAttack(f.merc),200);
            f.check(!victim.isAlive()&&f.count(key)==30,"Accepted mercenary death crosses thirty exactly once");
            f.check(f.messages("I grow weary of fighting these monsters alone.")==1,"One private thirty-kill warning");
            victim.die(f.merc.damageSources().mobAttack(f.merc));
            f.check(f.count(key)==30,"Repeated native die does not count the same victim twice");
            var cancelled=f.mob(EntityType.CREEPER,true);
            java.util.function.Consumer<LivingDeathEvent> deny=e->{if(e.getEntity()==cancelled)e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(deny);
            try{f.hit(cancelled,f.merc.damageSources().mobAttack(f.merc),200);
                f.check(f.count(key)==30,"Canceled native death never increments counter");}
            finally{NeoForge.EVENT_BUS.unregister(deny);cancelled.discard();}
            f.count(key,39);var fortieth=f.mob(EntityType.CREEPER,true);
            var arrow=new Arrow(f.level,f.merc,new ItemStack(Items.ARROW),null);MercenaryPotions.mark(arrow,f.merc);
            f.hit(fortieth,f.merc.damageSources().arrow(arrow,f.merc),200);
            f.check(f.count(key)==40,"Owned projectile finishing blow counts once");
            var refused=f.mob(EntityType.CREEPER,true);
            f.check(!MercenaryAntiFarming.allows(f.merc,refused),"Forty blocks further spawner encounters");
            f.hit(refused,f.merc.damageSources().mobAttack(f.merc),10);
            f.check(refused.getHealth()==100&&f.count(key)==40,"Native melee backstop preserves blocked target");
            for(int i=0;i<20;i++)MercenaryAntiFarming.allows(f.merc,refused);
            f.check(f.messages("You fight, I")<=1,"Repeated AI checks cannot flood refusal messages");
            var untagged=f.mob(EntityType.CREEPER,false);
            f.check(MercenaryAntiFarming.allows(f.merc,untagged),"Other hostiles remain eligible");
        }helper.succeed();
    }
    public static void damagePaths(GameTestHelper helper){
        int offset=0;
        for(String cls:List.of("judicator","dragoon","bogatyr","venefex","pyroclast")){
            try(var f=new Fixture(helper,Long.MAX_VALUE-17010-offset++,cls)){
                var target=f.mob(EntityType.ZOMBIE,true);f.count(f.key(target),40);
                var arrow=new Arrow(f.level,f.merc,new ItemStack(Items.ARROW),null);MercenaryPotions.mark(arrow,f.merc);
                f.hit(target,f.merc.damageSources().arrow(arrow,f.merc),10);
                f.check(target.getHealth()==100,"Projectile damage rejected for "+cls);
                if(cls.equals("judicator")){
                    for(int i=0;i<10;i++)ChargedClassSkills.addCharge(f.merc,SignatureSkill.SMITE);
                    f.check(!ChargedClassSkills.smite(f.merc)&&target.getHealth()==100
                            &&ChargedClassSkills.charge(f.merc,SignatureSkill.SMITE)==10,"Blocked Smite preserves charge and health");
                }else if(cls.equals("dragoon")){
                    var initial=f.mob(EntityType.ZOMBIE,false);
                    f.check(LightningCombat.cast(f.merc,initial,f.merc.damageSources().mobAttack(f.merc),10)==0
                            &&target.getHealth()==100,"Secondary lightning cannot hit refused spawner mob");
                }else if(cls.equals("bogatyr")){
                    var wolf=EntityType.WOLF.create(f.level,EntitySpawnReason.COMMAND);
                    f.check(wolf!=null,"Native owned wolf created");wolf.setNoAi(true);wolf.setPos(f.origin);
                    MercenaryWolves.prepare(wolf,f.merc,f.owner);f.entities.add(wolf);
                    f.check(f.level.addFreshEntity(wolf),"Valid mercenary wolf admitted");
                    f.hit(target,wolf.damageSources().mobAttack(wolf),10);
                    f.check(target.getHealth()==100,"Mercenary-owned wolf cannot bypass refusal");
                    f.count(f.key(target),0);target.setHealth(1);
                    f.hit(target,wolf.damageSources().mobAttack(wolf),10);
                    f.check(f.count(f.key(target))==1,"Owned wolf native death increments once under mercenary identity");
                }else if(cls.equals("venefex")){
                    var dose=new MercenaryPotionCredit.Dose(f.merc.getUUID(),f.run,new net.minecraft.nbt.CompoundTag());
                    dose.tick(f.level,target,()->{f.hit(target,target.damageSources().magic(),10);return true;});
                    f.check(target.getHealth()==100,"Previously applied delayed harmful dose respects current refusal");
                    f.check(!MercenaryPotions.allows(f.merc,target,MobEffects.WITHER.value()),"Per-effect harmful potion gate rejects blocked recipient");
                }else{
                    var rocket=new FireworkRocketEntity(f.level,new ItemStack(Items.FIREWORK_ROCKET),f.merc,
                            f.origin.x,f.origin.y,f.origin.z,true);MercenaryPotions.mark(rocket,f.merc);
                    f.hit(target,rocket.damageSources().fireworks(rocket,f.merc),10);
                    f.check(target.getHealth()==100,"Firework secondary damage cannot bypass refusal");
                }
            }
        }helper.succeed();
    }
    public static void persistence(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-17002,"bogatyr")){
            var target=f.mob(EntityType.CREEPER,true);String key=f.key(target);f.count(key,40);
            f.data().setValue(f.run,"future_antifarm_field","preserved");
            f.check(f.data().flushVerified(),"Save actual run counter");
            DurableRecoveryGameTestSupport.reload(f.level.getServer(),D1RunData.class,"cosmicdungeon_d1_objectives_v1");
            f.check(f.count(key)==40&&f.data().values(f.run,"future_antifarm_field").equals(List.of("preserved")),
                    "Native save/reload retains refusal and unrelated keys");
            f.players.remove(f.owner.getUUID());
            f.check(f.count(key)==40,"Logout does not erase run counter");f.players.put(f.owner.getUUID(),f.owner);
            f.merc.die(f.merc.damageSources().generic());f.merc.resumeAfterRest();
            f.check(f.count(key)==40&&!MercenaryAntiFarming.allows(f.merc,target),"Mercenary rest/revival retains refusal");
            f.check(MercenaryAntiFarming.count(f.level,f.run+1,key)==0,"Same source starts fresh in a different run");
            String anotherDimension="minecraft:the_nether|"+TAG;
            f.check(!key.equals(anotherDimension),"Fixture distinct dimension identity");
            MercenaryAntiFarming.setCount(f.level,f.run,anotherDimension,12);
            f.check(f.count(key)==40&&f.count(anotherDimension)==12,"Distinct source dimensions retain separate counts");
            MercenaryAntiFarming.playerDied(f.owner);
            f.check(f.count(key)==0&&f.count(anotherDimension)==0,"Accepted group-death callback resets all source counters");
            f.count(key,40);f.data().clearRun(f.run);
            f.check(f.count(key)==0,"Normal run cleanup clears anti-farming state");
        }helper.succeed();
    }
    public static void healing(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-17003,"theurgist")){
            var state=StationaryHealingState.begin(0,f.owner.getX(),f.owner.getZ());
            for(int t=0;t<=800;t+=100)state=state.damage(t,f.owner.getX(),f.owner.getZ());
            f.data().setCount(f.run,MercenaryAntiFarming.clockKey(f.owner.getUUID()),800);
            f.data().setValue(f.run,MercenaryAntiFarming.healingKey(f.owner.getUUID()),state.serialize());
            f.owner.setHealth(10);f.merc.setHealth(10);
            f.check(!MercenaryAntiFarming.healingAllowed(f.merc,f.owner),"Stored stationary recipient is blocked");
            f.check(MercenaryAntiFarming.healingAllowed(f.merc,f.merc),"Other recipients remain eligible");
            var potion=new ItemStack(Items.SPLASH_POTION);
            potion.set(DataComponents.POTION_CONTENTS,new PotionContents(Potions.HEALING));
            var shot=new ThrownSplashPotion(f.level,f.merc,potion);shot.setPos(f.origin);MercenaryPotions.mark(shot,f.merc);
            shot.onHitAsPotion(f.level,potion,new EntityHitResult(f.owner,f.origin));
            f.check(f.owner.getHealth()==10&&f.merc.getHealth()>10,"Actual splash heals eligible nearby ally but never blocked bystander");
            var target=f.mob(EntityType.CREEPER,true);String key=f.key(target);f.count(key,40);
            f.hit(target,f.owner.damageSources().playerAttack(f.owner),1);
            f.check(!MercenaryAntiFarming.healingAllowed(f.merc,f.owner)&&f.count(key)==40,
                    "Stationary successful player combat clears neither independent protection");
            f.owner.setPos(f.origin.add(0,0,1));MercenaryAntiFarming.observe(f.owner);
            f.check(MercenaryAntiFarming.healingAllowed(f.merc,f.owner)&&f.count(key)==40,
                    "One horizontal block resumes healing without proving spawner participation");
        }helper.succeed();
    }
    public static void effectPersistence(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-17005,"venefex")){
            var target=f.mob(EntityType.CREEPER,true);String key=f.key(target);
            var source=new ThrownSplashPotion(f.level,f.merc,new ItemStack(Items.SPLASH_POTION));
            MercenaryPotions.mark(source,f.merc);
            var weak=new net.minecraft.world.effect.MobEffectInstance(MobEffects.WITHER,240,0);
            ((MercenaryPotionCredit)(Object)weak).cosmicdungeon$dose(
                    new MercenaryPotionCredit.Dose(f.merc.getUUID(),f.run,source.getPersistentData()));
            f.check(target.addEffect(weak,f.merc),"Native weak mercenary wither admitted");
            f.check(target.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WITHER,1,1)),
                    "Stronger short external effect hides the mercenary dose");
            f.check(((MercenaryPotionCredit)(Object)target.getEffect(MobEffects.WITHER)).cosmicdungeon$dose()==null,
                    "External winning dose does not inherit mercenary ownership");
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                    net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess());
            target.saveWithoutId(output);
            target.discard();
            var restored=EntityType.CREEPER.create(f.level,EntitySpawnReason.COMMAND);
            f.check(restored!=null,"Native restored target created");
            restored.load(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING,f.level.registryAccess(),output.buildResult()));
            restored.setNoAi(true);f.entities.add(restored);f.check(f.level.addFreshEntity(restored),"Reloaded target admitted");
            f.check(key.equals(f.key(restored)),"Native spawner source tag survives entity save/load");
            var active=restored.getEffect(MobEffects.WITHER);
            f.check(active!=null&&active.getAmplifier()==1
                    &&((MercenaryPotionCredit)(Object)active).cosmicdungeon$dose()==null,
                    "Reload keeps the stronger external active effect independent");
            active.tickServer(f.level,restored,()->{});
            f.check(active.getAmplifier()==0&&((MercenaryPotionCredit)(Object)active).cosmicdungeon$dose()!=null,
                    "Hidden mercenary effect restores its own persisted origin after native downgrade");
            f.count(key,40);float health=restored.getHealth();
            for(int i=0;i<50;i++)active.tickServer(f.level,restored,()->{});
            f.check(restored.getHealth()==health,"Restored hidden harmful effect respects the spawner refusal");
            int progress=f.data().count(f.run,MercenarySkills.key(f.merc.contract().id(),MercenarySkill.NEGATIVE_POTIONS));
            f.count(key,0);
            for(int i=0;i<50;i++)active.tickServer(f.level,restored,()->{});
            f.check(restored.getHealth()<health,"Allowed restored effect still performs native damage");
            f.check(f.data().count(f.run,MercenarySkills.key(f.merc.contract().id(),MercenarySkill.NEGATIVE_POTIONS))==progress,
                    "Restoring dose ownership does not mint duplicate potion progression");
        }helper.succeed();
    }
    public static void participation(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-17004,"bogatyr")){
            var target=f.mob(EntityType.CREEPER,true);String key=f.key(target);f.count(key,40);
            MercenaryAntiFarming.observe(f.owner);
            f.owner.setPos(f.origin.add(0,0,1));MercenaryAntiFarming.observe(f.owner);
            f.hit(target,f.owner.damageSources().playerAttack(f.owner),1);
            f.check(f.count(key)==40,"Displacement without directional input cannot pretend to be player combat participation");
            f.owner.setLastClientInput(new Input(true,false,false,false,false,false,false));
            f.owner.setPos(f.origin.add(0,0,2));MercenaryAntiFarming.observe(f.owner);
            f.check(f.count(key)==40,"Walking alone does not reset spawner refusal");
            f.hit(target,f.owner.damageSources().playerAttack(f.owner),1);
            f.check(f.count(key)==0&&MercenaryAntiFarming.allows(f.merc,target),"Walking plus actual player combat resumes mercenary help");
        }helper.succeed();
    }
}
