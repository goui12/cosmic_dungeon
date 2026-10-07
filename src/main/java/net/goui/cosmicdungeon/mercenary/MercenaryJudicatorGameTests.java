package net.goui.cosmicdungeon.mercenary;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Native CI only; all geometry stays in the helper's loaded chunk and fixtures restore synchronously. */
public final class MercenaryJudicatorGameTests {
    private MercenaryJudicatorGameTests(){}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;final ServerLevel level;final long id;
        final ServerPlayer owner;final MercenaryContract contract;final MercenaryEntity merc;
        final D1RunData data;final Vec3 origin;final AABB bounds;
        final Map<Long,DungeonRunRegistryData.RunRecord> runs;final Map<UUID,ServerPlayer> players;
        final List<Mob> mobs=new ArrayList<>();final Set<UUID> oldDrops=new HashSet<>();
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper,long id){
            this.helper=helper;this.id=id;level=helper.getLevel();var server=level.getServer();
            var anchor=helper.absoluteVec(new Vec3(.5,14,.5));
            origin=new Vec3((net.minecraft.util.Mth.floor(anchor.x)&~15)+8.5,anchor.y,
                    (net.minecraft.util.Mth.floor(anchor.z)&~15)+8.5);
            bounds=new AABB(origin,origin).inflate(7,4,7);
            level.getEntitiesOfClass(ItemEntity.class,bounds).forEach(e->oldDrops.add(e.getUUID()));
            level.getEntitiesOfClass(ExperienceOrb.class,bounds).forEach(e->oldDrops.add(e.getUUID()));
            owner=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"JudicatorTest"));
            owner.setPos(origin.add(-4,0,0));owner.setHealth(owner.getMaxHealth());
            contract=new MercenaryContract(UUID.randomUUID(),owner.getUUID(),"judicator",2,50);
            var run=new DungeonRunRegistryData.RunRecord(id,"dungeon_1","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,List.of(owner.getUUID()),List.of(),List.of())
                    .withMercenaries(List.of(contract));
            data=D1RunData.get(server);
            try{
                var rf=DungeonRunRegistryData.class.getDeclaredField("runsById");rf.setAccessible(true);
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)rf.get(DungeonRunRegistryData.get(server));
                var pf=PlayerList.class.getDeclaredField("playersByUUID");pf.setAccessible(true);
                players=(Map<UUID,ServerPlayer>)pf.get(server.getPlayerList());
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Native fixture unavailable",error);}
            if(runs.containsKey(id)||players.containsKey(owner.getUUID()))throw new IllegalStateException("Fixture collision");
            runs.put(id,run);players.put(owner.getUUID(),owner);
            merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);merc.initialize(id,contract);
            merc.setNoAi(true);merc.setPos(origin.add(-4,0,0));level.addFreshEntity(merc);
            check(MercenaryJudicator.activeOwner(merc)==owner,"Judicator has a real admitted living owner");
        }
        <T extends Mob> T mob(EntityType<T> type,double x,double z){
            var mob=type.create(level,EntitySpawnReason.COMMAND);
            check(mob!=null,"Native fixture mob created");mob.setNoAi(true);mob.setPos(origin.add(x,0,z));
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);mob.setHealth(100);
            level.addFreshEntity(mob);mobs.add(mob);return mob;
        }
        int kills(){return data.count(id,MercenarySkills.key(contract.id(),MercenarySkill.COMBAT));}
        void kills(int value){data.setCount(id,MercenarySkills.key(contract.id(),MercenarySkill.COMBAT),value);}
        void hit(Mob target,float amount){target.invulnerableTime=0;target.hurtServer(level,merc.damageSources().mobAttack(merc),amount);}
        void reset(){for(var mob:mobs)if(mob.isAlive()){mob.invulnerableTime=0;mob.setHealth(100);}}
        void procSeed(){
            // Choose a deterministic seed whose first two rolls both proc at level four.
            // A broken recursive guard would therefore cause a visible second burst.
            for(long seed=0;seed<100000;seed++){
                var random=RandomSource.create(seed);
                if(random.nextInt(100)<4&&random.nextInt(100)<4){merc.getRandom().setSeed(seed);return;}
            }
            throw new IllegalStateException("No deterministic burst seed");
        }
        void check(boolean condition,String message){helper.assertTrue(condition,Component.literal(message));}
        @Override public void close(){
            mobs.forEach(Entity::discard);merc.discard();players.remove(owner.getUUID());
            level.removePlayerImmediately(owner,Entity.RemovalReason.DISCARDED);
            level.getEntitiesOfClass(ItemEntity.class,bounds,e->!oldDrops.contains(e.getUUID())).forEach(Entity::discard);
            level.getEntitiesOfClass(ExperienceOrb.class,bounds,e->!oldDrops.contains(e.getUUID())).forEach(Entity::discard);
            runs.remove(id);data.clearRun(id);
        }
    }
    public static void combat(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-1801)){
            var primary=f.mob(EntityType.CREEPER,0,0);var secondary=f.mob(EntityType.CREEPER,1.5,0);
            var edge=f.mob(EntityType.CREEPER,0,2);var outside=f.mob(EntityType.CREEPER,3.25,0);
            var diagonal=f.mob(EntityType.CREEPER,1.5,1.5);var immune=f.mob(EntityType.CREEPER,0,-1);
            immune.setInvulnerable(true);
            var cow=f.mob(EntityType.COW,-1,0);var wolf=f.mob(EntityType.WOLF,-1,1);
            wolf.setOwnerReference(EntityReference.of(f.owner.getUUID()));
            var angryWolf=f.mob(EntityType.WOLF,-1,-1);angryWolf.setTarget(f.owner);
            var angryBee=f.mob(EntityType.BEE,-.5,-1);angryBee.setTarget(f.owner);
            var angryBear=f.mob(EntityType.POLAR_BEAR,-.5,1);angryBear.setTarget(f.owner);
            var villager=f.mob(EntityType.VILLAGER,.5,0);var allay=f.mob(EntityType.ALLAY,.5,.5);
            var iron=f.mob(EntityType.IRON_GOLEM,.5,-.5);var snow=f.mob(EntityType.SNOW_GOLEM,0,.5);
            var teammate=f.mob(EntityType.CREEPER,0,1);
            var companion=new MercenaryEntity(ModEntities.MERCENARY.get(),f.level);
            companion.initialize(f.id,new MercenaryContract(UUID.randomUUID(),f.owner.getUUID(),"judicator",3,50));
            companion.setNoAi(true);companion.setPos(f.origin.add(.5,0,1));
            companion.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);companion.setHealth(100);
            f.level.addFreshEntity(companion);f.mobs.add(companion);
            var allies=List.of(cow,wolf,angryWolf,angryBee,angryBear,villager,allay,iron,snow,teammate,companion);
            var board=f.level.getScoreboard();var team=board.addPlayerTeam("judge"+f.owner.getUUID().toString().substring(0,8));
            board.addPlayerToTeam(f.owner.getScoreboardName(),team);board.addPlayerToTeam(teammate.getScoreboardName(),team);
            try{
                f.procSeed();f.hit(primary,1);
                f.check(secondary.getHealth()==100&&f.kills()==0,"Level zero cannot proc or earn from a nonlethal hit");
                f.kills(12);f.reset();f.procSeed();f.hit(primary,1);
                f.check(secondary.getHealth()==96&&edge.getHealth()==96,"Level four deals exactly four HP through the two-block boundary");
                f.check(outside.getHealth()==100&&diagonal.getHealth()==100,"Radius is spherical and secondary hits never chain another burst");
                f.check(immune.getHealth()==100&&allies.stream().allMatch(m->m.getHealth()==100)
                        &&f.merc.getHealth()==20&&f.owner.getHealth()==20,"All allied, neutral, owned, teammate and immune entities are protected");
                f.check(f.kills()==12,"Damage alone grants no Combat kill credit");

                f.reset();f.procSeed();primary.setInvulnerable(true);f.hit(primary,1);
                f.check(secondary.getHealth()==100,"Invulnerable primary cannot trigger a burst");primary.setInvulnerable(false);
                f.procSeed();f.hit(primary,0);f.check(secondary.getHealth()==100,"Zero damage cannot trigger a burst");
                f.reset();f.procSeed();f.hit(teammate,1);
                f.check(secondary.getHealth()==100&&teammate.getHealth()==100,"Allied direct hit cannot trigger");
                f.reset();f.procSeed();
                primary.hurtServer(f.level,f.merc.damageSources().indirectMagic(f.merc,f.merc),1);
                f.check(secondary.getHealth()==100,"Indirect potion or magic damage cannot trigger");

                f.reset();f.procSeed();
                var arrow=new Arrow(f.level,f.merc,new ItemStack(Items.ARROW),null);
                primary.hurtServer(f.level,f.merc.damageSources().arrow(arrow,f.merc),1);
                f.check(secondary.getHealth()==96,"The Judicator's own direct arrow hit triggers around the struck enemy");
                f.reset();f.procSeed();
                var foreign=new Arrow(f.level,f.owner,new ItemStack(Items.ARROW),null);
                primary.hurtServer(f.level,f.merc.damageSources().arrow(foreign,f.merc),1);
                f.check(secondary.getHealth()==100,"Mismatched arrow ownership cannot trigger a forged direct hit");

                f.reset();f.procSeed();primary.setHealth(1);secondary.setHealth(1);
                f.hit(primary,1);
                f.check(!primary.isAlive()&&!secondary.isAlive()&&f.kills()==14,
                        "Fatal direct hit still bursts; primary and secondary deaths each earn one Combat kill");
                f.check(f.data.count(f.id,"kills:"+f.owner.getUUID())==2,"Secondary deaths retain ordinary owner kill credit");
                primary.die(f.merc.damageSources().mobAttack(f.merc));secondary.die(f.merc.damageSources().mobAttack(f.merc));
                f.check(f.kills()==14,"Repeated native death callbacks cannot duplicate Combat credit");
            }finally{board.removePlayerTeam(team);}
            helper.succeed();
        }
    }
    public static void progression(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-1802)){
            f.check(MercenarySkills.level(f.merc,MercenarySkill.COMBAT)==0,"Judicator starts at Combat zero");
            for(int kills=1;kills<=24;kills++){
                var victim=f.mob(EntityType.CREEPER,4,0);victim.setHealth(1);f.hit(victim,1);
                f.check(f.kills()==kills,"Each native hostile death credits exactly once at total "+kills);
                int expected=kills<3?1:kills<6?2:kills<12?3:kills<24?4:5;
                f.check(MercenarySkills.level(f.merc,MercenarySkill.COMBAT)==expected,"Native progression matches cumulative kill boundary "+kills);
            }
            var cancelled=f.mob(EntityType.CREEPER,4,0);cancelled.setHealth(1);
            java.util.function.Consumer<LivingDeathEvent> cancel=e->{if(e.getEntity()==cancelled)e.setCanceled(true);};
            NeoForge.EVENT_BUS.addListener(cancel);
            try{f.hit(cancelled,1);f.check(f.kills()==24,"Cancelled death grants no Combat credit");}
            finally{NeoForge.EVENT_BUS.unregister(cancel);}
            cancelled.setHealth(1);f.hit(cancelled,1);f.check(f.kills()==25,"Later uncancelled death can earn exactly once");

            var environment=f.mob(EntityType.CREEPER,4,0);f.hit(environment,1);environment.invulnerableTime=0;
            environment.hurtServer(f.level,environment.damageSources().generic(),200);
            f.check(f.kills()==26,"A native credited environmental finish keeps the Judicator's hostile kill");
            var command=f.mob(EntityType.CREEPER,4,0);f.hit(command,1);
            command.hurtServer(f.level,command.damageSources().genericKill(),Float.MAX_VALUE);
            f.check(f.kills()==26,"Administrative /kill cannot turn remembered damage into Combat XP");
            var playerFinish=f.mob(EntityType.CREEPER,4,0);f.hit(playerFinish,1);playerFinish.invulnerableTime=0;
            playerFinish.hurtServer(f.level,f.owner.damageSources().playerAttack(f.owner),200);
            f.check(f.kills()==26,"Player finishing blow is not also credited to the earlier Judicator hit");
            var neutral=f.mob(EntityType.COW,4,0);neutral.setHealth(1);f.hit(neutral,10);
            f.check(neutral.isAlive()&&f.kills()==26,"A neutral victim grants no hostile kill credit");

            f.merc.supplies().set(0,new ItemStack(Items.DIAMOND,3));f.merc.die(f.merc.damageSources().generic());
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,f.level.registryAccess());
            f.merc.addAdditionalSaveData(output);
            var restored=new MercenaryEntity(ModEntities.MERCENARY.get(),f.level);
            restored.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),output.buildResult()));
            f.check(restored.dormant()&&MercenarySkills.level(restored,MercenarySkill.COMBAT)==5,
                    "Death and entity reload retain Combat level in the same run");
            restored.resumeAfterRest();
            f.check(MercenarySkills.level(restored,MercenarySkill.COMBAT)==5&&restored.supplies().getFirst().getCount()==3,
                    "Revival retains Combat progress and original inventory");
            var fresh=new MercenaryEntity(ModEntities.MERCENARY.get(),f.level);fresh.initialize(f.id+1,f.contract);
            f.check(MercenarySkills.level(fresh,MercenarySkill.COMBAT)==0,"A different run resets Combat to zero");
            helper.succeed();
        }
    }
}
