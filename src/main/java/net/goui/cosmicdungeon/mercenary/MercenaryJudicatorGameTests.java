package net.goui.cosmicdungeon.mercenary;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.playerclass.judicator.SmiteCombat;
import net.goui.cosmicdungeon.playerclass.skill.*;
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
        void fillSmite(){
            for(int charge=ChargedClassSkills.charge(merc,SignatureSkill.SMITE);charge<10;charge++)
                ChargedClassSkills.addCharge(merc,SignatureSkill.SMITE);
        }
        int smiteProgress(){return data.count(id,SignatureSkills.smiteKey(contract.id()));}
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
            f.merc.setPos(f.origin);
            var primary=f.mob(EntityType.ZOMBIE,1,0);var edge=f.mob(EntityType.SKELETON,0,3);
            var outside=f.mob(EntityType.ZOMBIE,3.01,0);var diagonal=f.mob(EntityType.ZOMBIE,2.4,2.4);
            var immune=f.mob(EntityType.HUSK,0,-1);immune.setInvulnerable(true);
            var creeper=f.mob(EntityType.CREEPER,1.5,0);var cow=f.mob(EntityType.COW,-1,0);
            var wolf=f.mob(EntityType.WOLF,-1,1);wolf.setOwnerReference(EntityReference.of(f.owner.getUUID()));
            var teammate=f.mob(EntityType.ZOMBIE,0,1);
            var board=f.level.getScoreboard();var team=board.addPlayerTeam("judge"+f.owner.getUUID().toString().substring(0,8));
            board.addPlayerToTeam(f.owner.getScoreboardName(),team);board.addPlayerToTeam(teammate.getScoreboardName(),team);
            try{
                f.kills(12);f.hit(primary,1);
                f.check(edge.getHealth()==100,"Ordinary Judicator hits no longer trigger the historical random Combat burst");
                f.reset();
                for(int i=0;i<9;i++)ChargedClassSkills.addCharge(f.merc,SignatureSkill.SMITE);
                MercenaryJudicator.tick(f.merc);
                f.check(primary.getHealth()==100&&ChargedClassSkills.charge(f.merc,SignatureSkill.SMITE)==9,
                        "Nine undead-kill charges cannot activate Smite");

                int[] thresholds={0,10,30,60};
                for(int skill=1;skill<=4;skill++){
                    var rank=SmiteCombat.rank(skill);f.reset();
                    primary.setPos(f.origin.add(1,0,0));edge.setPos(f.origin.add(0,0,rank.radius()));
                    outside.setPos(f.origin.add(rank.radius()+.01,0,0));
                    diagonal.setPos(f.origin.add(rank.radius()*.8,0,rank.radius()*.8));
                    f.data.setCount(f.id,SignatureSkills.smiteKey(f.contract.id()),thresholds[skill-1]);f.fillSmite();
                    MercenaryJudicator.tick(f.merc);
                    f.check(primary.getHealth()==100-rank.damage()&&edge.getHealth()==100-rank.damage(),
                            "Rank "+skill+" deals its exact native damage through the inclusive radius");
                    f.check(outside.getHealth()==100&&diagonal.getHealth()==100,"Smite uses a sphere, not a box");
                    f.check(immune.getHealth()==100&&creeper.getHealth()==100&&cow.getHealth()==100
                            &&wolf.getHealth()==100&&teammate.getHealth()==100&&f.merc.getHealth()==20&&f.owner.getHealth()==20,
                            "Smite protects immune targets, non-undead, companions, allies and caster");
                    f.check(ChargedClassSkills.charge(f.merc,SignatureSkill.SMITE)==0
                            &&f.smiteProgress()==Math.min(60,thresholds[skill-1]+1),
                            "One useful activation spends all ten charge and earns one proficiency, including at the level cap");
                    f.check(f.kills()==12,"Nonlethal Smite does not reinterpret historical Combat kill progress");
                }
                f.reset();f.fillSmite();primary.setInvulnerable(true);edge.setInvulnerable(true);
                MercenaryJudicator.tick(f.merc);
                f.check(ChargedClassSkills.charge(f.merc,SignatureSkill.SMITE)==10&&f.smiteProgress()==60,
                        "An all-immune burst preserves charge and proficiency");
                primary.setInvulnerable(false);edge.setInvulnerable(false);
                java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> cancel=
                        e->{if(e.getEntity()==primary||e.getEntity()==edge)e.setCanceled(true);};
                NeoForge.EVENT_BUS.addListener(cancel);
                try{
                    MercenaryJudicator.tick(f.merc);
                    f.check(primary.getHealth()==100&&edge.getHealth()==100
                            &&ChargedClassSkills.charge(f.merc,SignatureSkill.SMITE)==10,
                            "Cancelled native damage also preserves all charge");
                }finally{NeoForge.EVENT_BUS.unregister(cancel);}
                f.owner.setHealth(0);MercenaryJudicator.tick(f.merc);
                f.check(primary.getHealth()==100,"An unavailable hirer cannot activate Smite");
                f.owner.setHealth(f.owner.getMaxHealth());
                f.check(ChargedClassSkills.charge(f.merc,SignatureSkill.SMITE)==10,
                        "The inactive owner's bank remains intact when the hirer returns");
                primary.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(10);
                primary.setAbsorptionAmount(10);edge.setInvulnerable(true);MercenaryJudicator.tick(f.merc);
                f.check(primary.getHealth()==100&&primary.getAbsorptionAmount()==5
                        &&ChargedClassSkills.charge(f.merc,SignatureSkill.SMITE)==0,
                        "Actual absorption damage is a useful Smite and spends charge");
                primary.setAbsorptionAmount(0);primary.invulnerableTime=0;edge.setInvulnerable(false);
                f.fillSmite();primary.setHealth(1);edge.setHealth(1);MercenaryJudicator.tick(f.merc);
                f.check(!primary.isAlive()&&!edge.isAlive()&&ChargedClassSkills.charge(f.merc,SignatureSkill.SMITE)==0,
                        "Fatal secondary Smite hits cannot recharge or recursively activate themselves");
                f.check(f.kills()==14&&f.data.count(f.id,"kills:"+f.owner.getUUID())==2,
                        "Smite retains normal owner kill attribution and historical Combat kill records");
                primary.die(f.merc.damageSources().indirectMagic(f.merc,f.merc));
                edge.die(f.merc.damageSources().indirectMagic(f.merc,f.merc));
                f.check(f.kills()==14&&ChargedClassSkills.charge(f.merc,SignatureSkill.SMITE)==0,
                        "Repeated native death callbacks cannot duplicate Smite charge or Combat credit");
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
