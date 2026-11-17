package net.goui.cosmicdungeon.playerclass.skill;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.goui.cosmicdungeon.effect.ModMobEffects;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.mercenary.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Measures real attack callbacks while keeping the existing ten-tick decision and native wolf goal. */
public final class MeleeHasteGameTests {
    private MeleeHasteGameTests(){}
    private static int mercAttacks(ChargedClassGameTests.Fixture f,MercenaryEntity merc,Mob target,boolean hasted,boolean ranged){
        merc.removeEffect(ModMobEffects.FLURRY_OF_FANGS);
        if(hasted)f.check(merc.addEffect(new MobEffectInstance(ModMobEffects.FLURRY_OF_FANGS,36000,0),f.player),
                "Native mercenary accepts Flurry");
        f.check(ClassBuffs.hasted(merc)==hasted,"Mercenary haste predicate follows native effect");
        merc.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ranged?Items.BOW:Items.IRON_SWORD));
        for(int i=0;i<merc.supplies().size();i++)merc.supplies().set(i,ItemStack.EMPTY);
        if(ranged)merc.supplies().set(0,new ItemStack(Items.ARROW,64));
        var brain=new MercenaryBrain();var hits=new AtomicInteger();
        Consumer<LivingDamageEvent.Post> count=event->{
            if(event.getEntity()==target&&event.getSource().getEntity()==merc&&event.getNewDamage()>0)hits.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(count);
        try{
            for(int i=1;i<=200;i++){
                target.setHealth(1024);target.invulnerableTime=0;target.setPos(f.origin.add(1,0,0));
                merc.setPos(f.origin);merc.tickCount=i*10-merc.getId();brain.tick(merc,f.level);
            }
            return ranged?64-merc.supplies().getFirst().getCount():hits.get();
        }finally{
            NeoForge.EVENT_BUS.unregister(count);
            f.level.getEntitiesOfClass(Projectile.class,merc.getBoundingBox().inflate(16),p->p.getOwner()==merc)
                    .forEach(Entity::discard);
        }
    }
    public static void mercenary(GameTestHelper h){
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-14101,"dragoon")){
            var contract=new MercenaryContract(UUID.randomUUID(),f.player.getUUID(),"dragoon",2,50);
            f.runs.put(f.run,f.runs.get(f.run).withMercenaries(List.of(contract)));
            var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),f.level);
            merc.initialize(f.run,contract);merc.setNoAi(true);merc.setPos(f.origin);f.entities.add(merc);
            f.check(f.level.addFreshEntity(merc)&&MercenaryBrain.hirer(merc)==f.player,"Active mercenary contract fixture");
            var target=f.mob(EntityType.ZOMBIE,1);
            f.check(mercAttacks(f,merc,target,false,false)==100,"Unbuffed mercenary keeps twenty-tick melee cadence");
            f.check(mercAttacks(f,merc,target,true,false)==110,"Flurry produces ten percent more real mercenary melee hits");
            f.check(mercAttacks(f,merc,target,false,true)==50,"Native mercenary ranged cadence remains forty ticks");
            f.check(mercAttacks(f,merc,target,true,true)==50,"Melee haste does not speed mercenary bow shots");
            merc.removeEffect(ModMobEffects.FLURRY_OF_FANGS);
            f.check(mercAttacks(f,merc,target,false,false)==100,"Removing Flurry restores original mercenary cadence");
        }
        h.succeed();
    }
    private static int wolfAttacks(ChargedClassGameTests.Fixture f,Wolf wolf,Mob target,boolean hasted){
        wolf.removeEffect(ModMobEffects.FLURRY_OF_FANGS);
        if(hasted)f.check(wolf.addEffect(new MobEffectInstance(ModMobEffects.FLURRY_OF_FANGS,36000,0),f.player),
                "Native allied wolf accepts Flurry");
        f.check(ClassBuffs.hasted(wolf)==hasted,"Wolf haste predicate follows native effect");
        wolf.setTarget(target);
        var goal=wolf.goalSelector.getAvailableGoals().stream().map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                .filter(g->g instanceof MeleeAttackGoal).map(g->(MeleeAttackGoal)g).findFirst().orElseThrow();
        var hits=new AtomicInteger();
        Consumer<LivingDamageEvent.Post> count=event->{
            if(event.getEntity()==target&&event.getSource().getEntity()==wolf&&event.getNewDamage()>0)hits.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(count);
        try{
            goal.start();
            for(int i=0;i<2200;i++){
                target.setHealth(1024);target.invulnerableTime=0;target.setPos(f.origin.add(.9,0,0));
                wolf.setPos(f.origin);wolf.tickCount++;goal.tick();
            }
            return hits.get();
        }finally{goal.stop();NeoForge.EVENT_BUS.unregister(count);}
    }
    public static void wolf(GameTestHelper h){
        try(var f=new ChargedClassGameTests.Fixture(h,Long.MAX_VALUE-14102,"dragoon")){
            var wolf=EntityType.WOLF.create(f.level,EntitySpawnReason.COMMAND);f.check(wolf!=null,"Native wolf created");
            wolf.tame(f.player);wolf.setNoAi(true);wolf.setPos(f.origin);wolf.setOrderedToSit(false);wolf.setInSittingPose(false);
            f.entities.add(wolf);f.check(f.level.addFreshEntity(wolf),"Allied wolf admitted");
            var target=f.mob(EntityType.ZOMBIE,.9);
            f.check(wolfAttacks(f,wolf,target,false)==110,"Native unbuffed wolf has twenty-tick melee cadence");
            f.check(wolfAttacks(f,wolf,target,true)==121,"Flurry produces ten percent more real native wolf melee hits");
            f.check(wolfAttacks(f,wolf,target,false)==110,"Flurry removal restores native wolf cadence");
        }
        h.succeed();
    }
}
