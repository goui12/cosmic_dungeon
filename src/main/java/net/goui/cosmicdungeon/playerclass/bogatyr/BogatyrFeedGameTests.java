package net.goui.cosmicdungeon.playerclass.bogatyr;
import java.util.*;
import net.goui.cosmicdungeon.network.BogatyrPayloads;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
/** Native inventory, healing, breeding and save boundaries, using the registered CI-only fixture. */
public final class BogatyrFeedGameTests {
    private BogatyrFeedGameTests(){}
    private static BogatyrModes.StandGround stay(Wolf w){
        return (BogatyrModes.StandGround)w.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal)
                .filter(g->g instanceof BogatyrModes.StandGround).findFirst().orElseThrow();
    }
    private static BreedGoal breeding(Wolf w){
        return (BreedGoal)w.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal)
                .filter(g->g instanceof BreedGoal).findFirst().orElseThrow();
    }
    private static void food(BogatyrCommandGameTests.Fixture f,int slot,Item item,int count){
        f.p().getInventory().setItem(slot,new ItemStack(item,count));
    }
    public static void priorityCap(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2801)){
            for(int i=0;i<3;i++)f.wolf(0,1,i-1,0).setHealth(1);
            food(f,0,Items.COOKED_BEEF,8);food(f,1,Items.COOKED_RABBIT,8);food(f,2,Items.BEEF,1);
            food(f,3,Items.CHICKEN,1);food(f,4,Items.ROTTEN_FLESH,1);food(f,5,Items.BONE,1);
            var plan=BogatyrFeed.plan(f.p(),f.run,f.pack());
            f.check(plan.inputs()==10&&plan.meals().stream().allMatch(BogatyrFeed.Meal::healing),"Ten actual healing inputs projected first");
            f.check(plan.meals().subList(0,4).stream().map(m->m.slots().getFirst()).toList().equals(List.of(5,4,3,2)),
                    "Bones/rotten then raw nutrition order despite reversed inventory slots");
            f.check(BogatyrFeed.execute(f.p(),f.run)==10,"At most ten actual inputs spent");
            f.check(f.p().getInventory().getItem(0).getCount()==8&&f.p().getInventory().getItem(1).getCount()==2,
                    "Lower player-nutrition cooked meat spent before steak");
            f.check(f.pack().stream().mapToDouble(Wolf::getHealth).sum()==57,"Raw4/cooked8 healing clips only to actual max health");
            f.check(f.pack().stream().noneMatch(Wolf::isInLove),"Healing does not initiate ineligible breeding");
            helper.succeed();
        }
    }
    public static void noOpAndLimited(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2802)){
            var a=f.wolf(0,0,1,0);food(f,0,Items.BONE,1);
            f.check(BogatyrFeed.execute(f.p(),f.run)==0&&f.p().getInventory().getItem(0).getCount()==1,
                    "Healthy adult without a mate consumes nothing");
            var pup=f.wolf(0,-1000,-1,0);var cooling=f.wolf(0,1000,0,1);
            f.check(BogatyrFeed.execute(f.p(),f.run)==0,"Pups/cooldowns cannot satisfy a mate");
            a.setHealth(5);f.check(BogatyrFeed.execute(f.p(),f.run)==1&&a.getHealth()==9,"One actual bone heals through ability only");
            f.check(f.p().getInventory().getItem(0).isEmpty()&&pup.getAge()==-1000&&cooling.getAge()==1000,"No stock underflow or incidental growth");
            f.check(!a.isFood(new ItemStack(Items.BONE))&&new ItemStack(Items.BONE).get(net.minecraft.core.component.DataComponents.FOOD)==null,
                    "Ordinary bones never become vanilla food");
            a.setHealth(a.getMaxHealth());var b=f.wolf(0,0,2,0);b.setOrderedToSit(true);b.setInSittingPose(true);food(f,0,Items.BEEF,2);
            f.check(BogatyrFeed.execute(f.p(),f.run)==0,"Individually seated Patrol dogs cannot consume a failed mating pair");
            helper.succeed();
        }
    }
    public static void pairedFeeding(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2803)){
            var a=f.wolf(0,0,1,0);var b=f.wolf(0,0,-1,0);food(f,0,Items.BEEF,1);
            f.check(BogatyrFeed.execute(f.p(),f.run)==0&&!a.isInLove()&&!b.isInLove(),"Insufficient pair stock feeds neither dog");
            food(f,0,Items.BEEF,2);b.setHealth(16);
            f.check(BogatyrFeed.execute(f.p(),f.run)==1&&b.getHealth()==20&&!a.isInLove()&&!b.isInLove(),
                    "Injured wolf healed first; last item is not wasted on an unaffordable pair");
            food(f,0,Items.BONE,2);f.check(BogatyrFeed.execute(f.p(),f.run)==2&&a.isInLove()&&b.isInLove(),"Two bones start eligible native mating");
            food(f,0,Items.BEEF,2);f.check(BogatyrFeed.execute(f.p(),f.run)==0,"Already fed pair consumes nothing twice");
            a.resetLove();f.check(BogatyrFeed.execute(f.p(),f.run)==1&&a.isInLove(),"An already-loving mate needs no duplicate item");
            helper.succeed();
        }
    }
    public static void canceledHeal(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2804)){
            var wolf=f.wolf(0,0,1,0);wolf.setHealth(10);food(f,0,Items.BEEF,2);
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingHealEvent> cancel=event->{
                if(event.getEntity()==wolf)event.setCanceled(true);
            };
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
            try{f.check(BogatyrFeed.execute(f.p(),f.run)==0&&wolf.getHealth()==10&&f.p().getInventory().getItem(0).getCount()==2,
                    "Canceled healing never spends an item");}
            finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
            helper.succeed();
        }
    }
    public static void actionAuthority(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2805)){
            var a=f.wolf(0,0,1,0);var foreign=f.wolf(1,0,-1,0);a.setHealth(1);foreign.setHealth(1);food(f,0,Items.BEEF,20);
            BogatyrActions.sync(f.p(),true);var v=f.view();f.check(v.feedInputs()==5,"Only caller's injured wolf is quoted");
            var stale=new BogatyrPayloads.Action(f.run,v.revision()-1,BogatyrPayloads.Kind.FEED_PACK);
            BogatyrActions.action(f.p(),stale);f.check(a.getHealth()==1,"Stale generation cannot feed");
            v=f.view();var original=f.root().copy();var changed=original.copy();changed.putString(ClassData.KEY_CLASS_ID,"theurgist");
            f.p().getPersistentData().put(ClassData.ROOT_TAG,changed);
            BogatyrActions.action(f.p(),new BogatyrPayloads.Action(f.run,v.revision(),BogatyrPayloads.Kind.FEED_PACK));
            f.check(a.getHealth()==1&&f.p().getInventory().getItem(0).getCount()==20,"Class changes revalidated before debit");
            f.p().getPersistentData().put(ClassData.ROOT_TAG,original);BogatyrActions.sync(f.p(),true);v=f.view();
            f.p().inventoryMenu.setCarried(new ItemStack(Items.STONE));
            f.check(BogatyrFeed.execute(f.p(),f.run)==0,"Carried stack/inventory guard rejects Feed Pack");
            f.p().inventoryMenu.setCarried(ItemStack.EMPTY);
            f.check(!BogatyrFeed.authorized(f.p(),f.run-1),"Wrong dungeon generation denied");
            BogatyrActions.sync(f.p(),true);v=f.view();var request=new BogatyrPayloads.Action(f.run,v.revision(),BogatyrPayloads.Kind.FEED_PACK);
            // A fresh session (as after reconnect) replaces the same-tick denied snapshot.
            BogatyrActions.forget(f.p());BogatyrActions.sync(f.p(),true);v=f.view();
            request=new BogatyrPayloads.Action(f.run,v.revision(),BogatyrPayloads.Kind.FEED_PACK);
            BogatyrActions.action(f.p(),request);int remaining=f.p().getInventory().getItem(0).getCount();
            BogatyrActions.action(f.p(),request);
            f.check(a.getHealth()==20&&foreign.getHealth()==1&&remaining==15&&f.p().getInventory().getItem(0).getCount()==15,
                    "One generation spends once; another owner's dog never fed by ability");
            helper.succeed();
        }
    }
    public static void stayBirth(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2806)){
            var a=f.wolf(0,0,-2,0);var b=f.wolf(0,0,2,0);BogatyrModes.select(f.p(),f.run,WolfMode.STAY);stay(a).start();stay(b).start();
            var aa=BogatyrModes.anchor(a);var ab=BogatyrModes.anchor(b);food(f,0,Items.BONE,2);
            f.check(BogatyrFeed.execute(f.p(),f.run)==2&&BogatyrBreeding.active(a)&&BogatyrBreeding.active(b),"Feed Pack temporarily releases both Stay parents");
            f.check(!a.isOrderedToSit()&&!b.isInSittingPose()&&!stay(a).canUse()&&a.canMate(b),"Native mate eligibility and movement released");
            var goal=breeding(a);f.check(goal.canUse(),"Native BreedGoal finds the real partner");goal.start();goal.tick();
            f.check(!a.getNavigation().isDone(),"Native breeding starts loaded path to mate");
            a.setPos(f.origin.add(1,0,0));var prior=f.pack().stream().map(Wolf::getUUID).collect(java.util.stream.Collectors.toSet());
            var bounds=a.getBoundingBox().inflate(3);var priorOrbs=f.level.getEntitiesOfClass(ExperienceOrb.class,bounds).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            for(int i=0;i<120&&a.isInLove();i++)goal.tick();
            f.level.getEntitiesOfClass(ExperienceOrb.class,bounds).stream().filter(e->!priorOrbs.contains(e.getUUID())).forEach(f.extras::add);
            var pups=f.pack().stream().filter(w->!prior.contains(w.getUUID())).toList();f.check(pups.size()==1,"Real native goal births exactly one enrolled pup");
            var pup=pups.getFirst();f.remember(pup);f.check(pup.isBaby()&&pup.isOwnedBy(f.p())&&BogatyrThreats.run(pup)==f.run,"Pup ownership/run preserved");
            BogatyrModes.apply(a,false);BogatyrModes.apply(b,false);stay(a).start();stay(b).start();
            f.check(!BogatyrBreeding.active(a)&&aa.equals(BogatyrModes.anchor(a))&&ab.equals(BogatyrModes.anchor(b)),"Successful mating retains both original anchors");
            f.check(!a.getNavigation().isDone(),"Displaced parent paths back to its held position");
            f.p().setPos(f.origin.add(-7,0,0));a.setPos(aa);stay(a).tick();
            f.check(a.isOrderedToSit()&&a.isInSittingPose()&&a.getTarget()==null,"Parent returns and sits regardless of moved master");
            helper.succeed();
        }
    }
    public static void allyTimeoutReload(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2807)){
            var wolf=f.wolf(0,0,1,0);BogatyrModes.select(f.p(),f.run,WolfMode.STAY);stay(wolf).start();var anchor=BogatyrModes.anchor(wolf);
            var ally=f.current.get(1);wolf.mobInteract(ally,InteractionHand.MAIN_HAND);
            f.check(wolf.isOrderedToSit(),"Ally empty hand cannot toggle seating");
            ally.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BEEF,2));wolf.mobInteract(ally,InteractionHand.MAIN_HAND);
            f.check(wolf.isInLove()&&BogatyrBreeding.active(wolf)&&!wolf.isInSittingPose()&&ally.getMainHandItem().getCount()==1,
                    "Actual ally food interaction uses temporary Stay mating");
            long deadline=BogatyrBreeding.deadline(wolf);f.check(deadline>f.level.getGameTime()&&deadline-f.level.getGameTime()<=1300,"Mate search has bounded deadline");
            wolf.setPos(f.origin.add(2,0,0));BogatyrBreeding.tick(wolf,deadline);BogatyrModes.apply(wolf,false);stay(wolf).start();
            f.check(!wolf.isInLove()&&!BogatyrBreeding.active(wolf)&&anchor.equals(BogatyrModes.anchor(wolf))&&!wolf.getNavigation().isDone(),
                    "No-mate timeout clears love and returns to unchanged anchor");
            wolf.setPos(anchor);stay(wolf).tick();wolf.setInLove(ally);
            var saved=f.savedWolf(wolf);wolf.discard();var loaded=f.readWolf(saved);f.check(f.level.addFreshEntity(loaded),"Native Stay-attempt reload");
            BogatyrModes.apply(loaded,false);stay(loaded).start();
            f.check(!loaded.isInLove()&&!BogatyrBreeding.active(loaded)&&anchor.equals(BogatyrModes.anchor(loaded))&&loaded.isOrderedToSit(),
                    "Reload safely cancels transient mating while preserving saved anchor");
            var future=new CompoundTag();future.putInt("schema",99);future.putString("future","untouched");loaded.getPersistentData().put(BogatyrModes.STAY,future.copy());
            loaded.setHealth(4);food(f,0,Items.BEEF,3);f.check(BogatyrFeed.execute(f.p(),f.run)==0&&future.equals(loaded.getPersistentData().get(BogatyrModes.STAY)),
                    "Unknown saved state rejects care without overwrite or debit");
            helper.succeed();
        }
    }
}
