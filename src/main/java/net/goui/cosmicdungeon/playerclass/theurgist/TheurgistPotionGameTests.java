package net.goui.cosmicdungeon.playerclass.theurgist;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.mercenary.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.playerclass.skill.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Native recipient loops and entity serialization; bounded synchronous fixtures in an already loaded chunk. */
public final class TheurgistPotionGameTests {
    private TheurgistPotionGameTests(){}
    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;final ServerLevel level;final Vec3 origin;final long run;
        final ServerPlayer caster,teammate;final Map<Long,DungeonRunRegistryData.RunRecord> runs;
        final List<Entity> owned=new ArrayList<>();final MercenaryEntity merc;final Wolf pet;
        final net.minecraft.world.entity.monster.Creeper hostile;
        final net.minecraft.world.entity.monster.Zombie undead,alliedUndead;
        final net.minecraft.world.entity.animal.Cow neutral;
        final net.minecraft.world.scores.PlayerTeam team;
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper,long run){
            this.helper=helper;this.level=helper.getLevel();this.run=run;
            var anchor=helper.absoluteVec(new Vec3(1.5,10.5,1.5));var chunk=new ChunkPos(BlockPos.containing(anchor));
            origin=new Vec3(chunk.getMinBlockX()+8.5,anchor.y,chunk.getMinBlockZ()+8.5);
            caster=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"PotionCaster"));
            teammate=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"PotionAlly"));
            caster.setPos(origin.add(0,0,5));teammate.setPos(origin.add(1,0,0));
            caster.setHealth(20);teammate.setHealth(20);classId("theurgist");
            level.addNewPlayer(caster);level.addNewPlayer(teammate);
            var contract=new MercenaryContract(UUID.randomUUID(),caster.getUUID(),"theurgist",2,50);
            try{
                var field=DungeonRunRegistryData.class.getDeclaredField("runsById");field.setAccessible(true);
                runs=(Map<Long,DungeonRunRegistryData.RunRecord>)field.get(DungeonRunRegistryData.get(level.getServer()));
            }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            check(!runs.containsKey(run),"Unique potion fixture run");
            runs.put(run,new DungeonRunRegistryData.RunRecord(run,"dungeon_1","minecraft:overworld",0,
                    List.of(level.dimension().location().toString()),1,"ACTIVE","",0,
                    List.of(caster.getUUID(),teammate.getUUID()),List.of(),List.of()).withMercenaries(List.of(contract)));
            merc=new MercenaryEntity(ModEntities.MERCENARY.get(),level);merc.initialize(run,contract);add(merc,-1);
            pet=new Wolf(EntityType.WOLF,level);pet.setTame(true,true);
            pet.setOwnerReference(EntityReference.of(teammate.getUUID()));
            pet.getPersistentData().putString("cosmicdungeon.bogatyr_owner",teammate.getStringUUID());add(pet,0);
            hostile=add(EntityType.CREEPER.create(level,EntitySpawnReason.COMMAND),0);
            undead=add(EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND),-2);
            alliedUndead=add(EntityType.ZOMBIE.create(level,EntitySpawnReason.COMMAND),-1);
            neutral=add(EntityType.COW.create(level,EntitySpawnReason.COMMAND),2);
            team=level.getScoreboard().addPlayerTeam("p"+UUID.randomUUID().toString().substring(0,12));
            level.getScoreboard().addPlayerToTeam(caster.getScoreboardName(),team);
            level.getScoreboard().addPlayerToTeam(alliedUndead.getScoreboardName(),team);
        }
        <T extends Mob> T add(T entity,double x){
            check(entity!=null,"Native potion target exists");entity.setNoAi(true);entity.setPos(origin.add(x,0,0));
            owned.add(entity);check(level.addFreshEntity(entity),"Native potion target admitted");return entity;
        }
        void classId(String value){
            var root=caster.getPersistentData().getCompoundOrEmpty(ClassData.ROOT_TAG).copy();root.putString(ClassData.KEY_CLASS_ID,value);
            caster.getPersistentData().put(ClassData.ROOT_TAG,root);
        }
        <T extends AbstractThrownPotion> T admit(T shot){
            owned.add(shot);shot.setPos(origin);
            // Protection must work even where progression deliberately captured no eligible attack.
            shot.getPersistentData().put(ClassSkills.SHOT,new CompoundTag());
            check(level.addFreshEntity(shot),"Thrown item admitted through the real capture event");return shot;
        }
        ThrownSplashPotion shot(Holder<Potion> potion){
            return admit(new ThrownSplashPotion(level,caster,PotionContents.createItemStack(Items.SPLASH_POTION,potion)));
        }
        void splash(ThrownSplashPotion shot){shot.onHitAsPotion(level,shot.getItem(),new EntityHitResult(hostile));shot.discard();}
        void clearEffects(){
            for(var entity:List.of(teammate,merc,pet,hostile,undead,alliedUndead,neutral))entity.removeAllEffects();
        }
        AreaEffectCloud cloud(Holder<Potion> potion){
            var before=new HashSet<UUID>();level.getEntitiesOfClass(AreaEffectCloud.class,new AABB(origin,origin).inflate(4))
                    .forEach(c->before.add(c.getUUID()));
            var shot=admit(new ThrownLingeringPotion(level,caster,PotionContents.createItemStack(Items.LINGERING_POTION,potion)));
            shot.onHitAsPotion(level,shot.getItem(),new EntityHitResult(hostile));shot.discard();
            var created=level.getEntitiesOfClass(AreaEffectCloud.class,new AABB(origin,origin).inflate(4),
                    c->!before.contains(c.getUUID()));
            check(created.size()==1,"Native lingering impact creates one accessible cloud");
            var cloud=created.getFirst();owned.add(cloud);cloud.setWaitTime(0);cloud.setRadiusOnUse(0);
            cloud.setRadiusPerTick(0);cloud.setDuration(200);return cloud;
        }
        AreaEffectCloud reload(AreaEffectCloud cloud){
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());
            cloud.saveWithoutId(output);cloud.discard();
            var restored=new AreaEffectCloud(level,origin.x,origin.y,origin.z);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult()));
            owned.add(restored);return restored;
        }
        void tick(AreaEffectCloud cloud){cloud.tickCount=5;cloud.tick();}
        void check(boolean value,String message){helper.assertTrue(value,Component.literal(message));}
        @Override public void close(){
            owned.forEach(Entity::discard);level.removePlayerImmediately(caster,Entity.RemovalReason.DISCARDED);
            level.removePlayerImmediately(teammate,Entity.RemovalReason.DISCARDED);level.getScoreboard().removePlayerTeam(team);
            runs.remove(run);D1RunData.get(level.getServer()).clearRun(run);
        }
    }
    public static void splashSafety(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2201)){
            f.splash(f.shot(Potions.SWIFTNESS));
            f.check(!f.hostile.hasEffect(MobEffects.SPEED)&&!f.undead.hasEffect(MobEffects.SPEED),
                    "Positive splash does not buff hostile mobs");
            f.check(f.teammate.hasEffect(MobEffects.SPEED)&&f.pet.hasEffect(MobEffects.SPEED)
                    &&f.merc.hasEffect(MobEffects.SPEED)&&f.neutral.hasEffect(MobEffects.SPEED),
                    "Players, allied pets, mercenaries and neutral native recipients retain beneficial effects");
            var angry=f.add(new Wolf(EntityType.WOLF,f.level),3);angry.setTarget(f.teammate);
            f.splash(f.shot(Potions.SWIFTNESS));
            f.check(!angry.hasEffect(MobEffects.SPEED),"A normally neutral mob currently attacking an ally is hostile");
            angry.discard();
            f.clearEffects();f.hostile.setHealth(10);f.undead.setHealth(10);f.alliedUndead.setHealth(10);
            f.pet.setHealth(4);f.merc.setHealth(4);f.teammate.setHealth(4);
            f.splash(f.shot(Potions.HEALING));
            f.check(f.hostile.getHealth()==10&&f.undead.getHealth()==10&&f.alliedUndead.getHealth()==10,
                    "Positive healing neither helps hostiles nor harms hostile or allied undead by inversion");
            f.check(f.pet.getHealth()>4&&f.merc.getHealth()>4&&f.teammate.getHealth()>4,"Native healing still benefits living allies");
            f.splash(f.shot(Potions.POISON));
            f.check(f.hostile.hasEffect(MobEffects.POISON)&&!f.pet.hasEffect(MobEffects.POISON),
                    "Manually brewed harmful potions retain hostile effects and existing pet protection");
            var brewed=f.level.potionBrewing().mix(new ItemStack(Items.FERMENTED_SPIDER_EYE),
                    PotionContents.createItemStack(Items.POTION,Potions.HEALING));
            f.check(brewed.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).potion().orElseThrow().equals(Potions.HARMING),
                    "Vanilla manual negative brewing recipe remains available");
            f.clearEffects();var frozen=f.shot(Potions.SWIFTNESS);f.classId("judicator");
            TheurgistPotionProtection.capture(frozen);f.splash(frozen);
            f.check(!f.hostile.hasEffect(MobEffects.SPEED),"Theurgist throw does not lose protection after class change or recapture");
            var other=f.shot(Potions.SWIFTNESS);f.classId("theurgist");TheurgistPotionProtection.capture(other);f.splash(other);
            f.check(f.hostile.hasEffect(MobEffects.SPEED),"Other-class throw remains native after later becoming Theurgist");
            helper.succeed();
        }
    }
    public static void cloudAttribution(GameTestHelper helper){
        try(var f=new Fixture(helper,Long.MAX_VALUE-2202)){
            var cloud=f.cloud(Potions.REGENERATION);var marker=cloud.getPersistentData()
                    .getCompoundOrEmpty(TheurgistPotionProtection.SOURCE).copy();
            f.check(Boolean.TRUE.equals(TheurgistPotionProtection.marked(marker)),"Actual lingering cloud inherits throw-time identity");
            cloud.getPersistentData().putString("future_potion_field","retained");
            var restored=f.reload(cloud);f.classId("judicator");restored.setOwner(null);f.tick(restored);
            f.check(restored.getPersistentData().getCompoundOrEmpty(TheurgistPotionProtection.SOURCE).equals(marker)
                    &&"retained".equals(restored.getPersistentData().getStringOr("future_potion_field","")),
                    "Native cloud save preserves marker and unknown fields");
            f.check(!f.hostile.hasEffect(MobEffects.REGENERATION)&&f.pet.hasEffect(MobEffects.REGENERATION)
                    &&f.merc.hasEffect(MobEffects.REGENERATION),"Owner-absent reloaded cloud still filters each recipient");
            f.clearEffects();f.classId("theurgist");f.undead.setHealth(10);
            var healing=f.reload(f.cloud(Potions.HEALING));healing.setOwner(null);f.tick(healing);
            f.check(f.undead.getHealth()==10,"Reloaded owner-absent healing cloud cannot invert on hostile undead");
            var negative=f.cloud(Potions.POISON);f.tick(negative);
            f.check(f.hostile.hasEffect(MobEffects.POISON)&&!f.pet.hasEffect(MobEffects.POISON),
                    "Manual harmful lingering effects remain native while existing allied pet protection is preserved");
            var legacy=new ThrownSplashPotion(f.level,f.caster,PotionContents.createItemStack(Items.SPLASH_POTION,Potions.SWIFTNESS));
            legacy.getPersistentData().put(ClassSkills.SHOT,SkillAttackSnapshot.create(f.caster.getUUID(),"theurgist","potions",f.run));
            f.classId("judicator");
            f.check(!TheurgistPotionProtection.allows(legacy,f.hostile,MobEffects.SPEED.value())
                    &&!legacy.getPersistentData().contains(TheurgistPotionProtection.SOURCE),"Legacy throw snapshot is read without rewriting its save");
            f.classId("theurgist");var old=new ThrownSplashPotion(f.level,f.caster,legacy.getItem());
            f.check(!TheurgistPotionProtection.allows(old,f.hostile,MobEffects.SPEED.value())
                    &&!old.getPersistentData().contains(TheurgistPotionProtection.SOURCE),"Unmarked legacy potion uses native caster without save migration");
            f.classId("judicator");f.clearEffects();var other=f.cloud(Potions.SWIFTNESS);f.classId("theurgist");f.tick(other);
            f.check(f.hostile.hasEffect(MobEffects.SPEED),"False class snapshot also survives cloud inheritance");
            helper.succeed();
        }
    }
}
