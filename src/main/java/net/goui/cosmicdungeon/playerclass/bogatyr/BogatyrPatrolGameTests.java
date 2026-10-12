package net.goui.cosmicdungeon.playerclass.bogatyr;
import java.util.*;
import net.goui.cosmicdungeon.network.*;
import net.goui.cosmicdungeon.playerclass.api.ClassData;
import net.goui.cosmicdungeon.transaction.PlayerSaveProof;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
/** Native goals, saves, packets and recovery boundaries; CI only. Retired skills are tested as denials. */
public final class BogatyrPatrolGameTests {
    private BogatyrPatrolGameTests(){}
    private static BogatyrModes.StandGround stay(Wolf wolf){
        return (BogatyrModes.StandGround)wolf.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal)
                .filter(g->g instanceof BogatyrModes.StandGround).findFirst().orElseThrow();
    }
    private static Mob enemy(BogatyrCommandGameTests.Fixture f,EntityType<? extends Mob> type){
        var mob=type.create(f.level,EntitySpawnReason.MOB_SUMMONED);f.check(mob!=null,"Native enemy factory");
        mob.snapTo(f.origin.x+2,f.origin.y,f.origin.z+1,0,0);mob.setNoAi(true);
        f.check(f.level.addFreshEntity(mob),"Native enemy admission");f.extras.add(mob);return mob;
    }
    private static void singleReset(BogatyrCommandGameTests.Fixture f,Runnable action,String phase){
        var probe=BogatyrWork.begin();try{
            action.run();
            f.check(probe.targetTraversals==1&&probe.targetGoals>0&&probe.memoryResets>0,
                    phase+" performs one actual target-selector traversal and clears native goal memory");
        }finally{BogatyrWork.end();}
    }
    private static void noReset(BogatyrCommandGameTests.Fixture f,Runnable action,String phase){
        var probe=BogatyrWork.begin();try{
            action.run();
            f.check(probe.targetTraversals==0&&probe.targetGoals==0&&probe.memoryResets==0,
                    phase+" preserves protection without repeating target-selector traversal");
        }finally{BogatyrWork.end();}
    }
    /** An external TARGET goal added after Stay selection; native GoalSelector owns its lifecycle. */
    private static final class InjectedTarget extends Goal implements BogatyrTargetMemory {
        private final Wolf wolf;private final Mob hostile;
        int starts,stops,ticks,forgets;
        InjectedTarget(Wolf wolf,Mob hostile){this.wolf=wolf;this.hostile=hostile;setFlags(EnumSet.of(Flag.TARGET));}
        @Override public boolean canUse(){return true;}
        @Override public boolean canContinueToUse(){return true;}
        @Override public void start(){starts++;wolf.setTarget(hostile);}
        @Override public void tick(){ticks++;wolf.setTarget(hostile);}
        @Override public void stop(){stops++;wolf.setTarget(null);}
        @Override public void cosmicdungeon$forgetOwnerThreat(){forgets++;}
    }
    private static void selectorTransitions(BogatyrCommandGameTests.Fixture f,Wolf wolf,Mob hostile){
        singleReset(f,()->f.check(BogatyrModes.select(f.p(),f.run,WolfMode.STAY),"Stay transition accepted"),"Patrol to Stay");
        stay(wolf).start();var anchor=BogatyrModes.anchor(wolf);
        f.check(anchor!=null&&wolf.isOrderedToSit(),"Transition fixture starts settled");
        noReset(f,()->{
            for(int i=0;i<40;i++){
                // Both direct/native target proposals and external hurt/anger writes remain blocked.
                wolf.tickCount=600+i;wolf.setLastHurtByMob(hostile);wolf.setLastHurtMob(hostile);
                wolf.setRemainingPersistentAngerTime(100);wolf.setPersistentAngerTarget(hostile.getUUID());
                wolf.setTarget(hostile);f.check(wolf.getTarget()==null,"Stay target-write guard is immediate");
                BogatyrModes.apply(wolf,false);stay(wolf).tick();
                f.check(wolf.getLastHurtByMob()==null&&wolf.getLastHurtMob()==null&&!wolf.isAngry(),
                        "Constant Stay clearing rejects externally written retaliation state");
            }
            f.check(BogatyrModes.select(f.p(),f.run,WolfMode.STAY),"Redundant Stay selection accepted");
        },"Settled Stay and redundant command");
        f.check(anchor.equals(BogatyrModes.anchor(wolf))&&wolf.isOrderedToSit()
                &&wolf.getNavigation().isDone(),"External writes preserve fixed anchor and seating");

        var injected=new InjectedTarget(wolf,hostile);wolf.targetSelector.addGoal(-1,injected);
        var wrapped=wolf.targetSelector.getAvailableGoals().stream().filter(g->g.getGoal()==injected).findFirst().orElseThrow();
        wrapped.start();f.check(wrapped.isRunning()&&wolf.getTarget()==null,"Externally started target goal cannot install a Stay target");
        noReset(f,()->{
            BogatyrModes.apply(wolf,false);
            for(int i=0;i<4;i++)wolf.targetSelector.tick();
        },"New mid-Stay target goal");
        f.check(!wrapped.isRunning()&&injected.starts==1&&injected.stops==1&&injected.ticks==0,
                "Native selector stops a TARGET goal injected during Stay and never restarts it");
        singleReset(f,()->f.check(BogatyrModes.select(f.p(),f.run,WolfMode.PATROL),"Patrol transition accepted"),"Stay to Patrol");
        f.check(injected.forgets==1,"Patrol transition resets newly injected target memory exactly once");
        for(int i=0;i<4;i++)wolf.targetSelector.tick();
        f.check(wrapped.isRunning()&&injected.starts==2&&injected.ticks>0,
                "Patrol restores native TARGET scheduling after Stay");

        // A flag disabled by another system before Stay must stay disabled after returning to Patrol.
        wolf.targetSelector.disableControlFlag(Goal.Flag.TARGET);
        singleReset(f,()->f.check(BogatyrModes.select(f.p(),f.run,WolfMode.STAY),"Stay with prior target restriction"),"Prior-disabled Stay entry");
        singleReset(f,()->f.check(BogatyrModes.select(f.p(),f.run,WolfMode.PATROL),"Patrol with prior target restriction"),"Prior-disabled Stay exit");
        int starts=injected.starts;for(int i=0;i<4;i++)wolf.targetSelector.tick();
        f.check(!wrapped.isRunning()&&injected.starts==starts,"Patrol preserves another system's preexisting TARGET disable");
        wolf.targetSelector.enableControlFlag(Goal.Flag.TARGET);wolf.targetSelector.removeGoal(injected);

        singleReset(f,()->f.check(BogatyrModes.select(f.p(),f.run,WolfMode.STAY),"Stay before identity checks"),"Identity fixture Stay entry");
        stay(wolf).start();
        singleReset(f,()->{
            wolf.tame(f.current.get(1));wolf.getPersistentData().putString(BogatyrWolfEvents.OWNER,f.current.get(1).getStringUUID());
            BogatyrModes.apply(wolf,false);
        },"Changed native owner");
        f.check(BogatyrModes.mode(wolf)==WolfMode.PATROL&&!wolf.isOrderedToSit(),"Changed owner does not inherit prior owner's Stay");
        singleReset(f,()->{
            wolf.tame(f.p());wolf.getPersistentData().putString(BogatyrWolfEvents.OWNER,f.p().getStringUUID());BogatyrModes.apply(wolf,false);
        },"Restored native owner");
        f.check(BogatyrModes.mode(wolf)==WolfMode.STAY,"Restored owner resolves its own selected mode");
        singleReset(f,()->{
            wolf.getPersistentData().putLong(BogatyrWolfEvents.RUN,f.run+1);BogatyrModes.apply(wolf,false);
        },"Changed run");
        f.check(BogatyrModes.mode(wolf)==WolfMode.PATROL&&!wolf.isOrderedToSit(),"New run does not inherit prior-run Stay");
        singleReset(f,()->{
            wolf.getPersistentData().putLong(BogatyrWolfEvents.RUN,f.run);BogatyrModes.apply(wolf,false);
        },"Restored run");
        f.check(BogatyrModes.mode(wolf)==WolfMode.STAY,"Restored run resolves its own selected mode");
        noReset(f,()->{for(int i=0;i<40;i++)BogatyrModes.apply(wolf,false);},"Settled restored identity");
        singleReset(f,()->f.check(BogatyrModes.select(f.p(),f.run,WolfMode.PATROL),"Identity fixture returns to Patrol"),"Final Patrol");
    }
    public static void retiredCommands(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2701)){
            var wolf=f.wolf(0,0,2,0);wolf.setHealth(5);f.amount(600);var before=f.root().copy();var image=BogatyrRecovery.image(wolf);
            for(var kind:java.util.List.of(BogatyrPayloads.Kind.BREED,BogatyrPayloads.Kind.SUMMON,BogatyrPayloads.Kind.REGROUP,BogatyrPayloads.Kind.HEAL)){
                f.check(!f.execute(kind),"Retired direct command denied: "+kind);
                BogatyrActions.sync(f.p(),true);var view=f.view();
                f.check(view.quotes().stream().noneMatch(BogatyrPayloads.Quote::enabled),"No old skill quote");
                BogatyrActions.action(f.p(),new BogatyrPayloads.Action(f.run,view.revision(),kind));
            }
            f.check(f.root().equals(before)&&f.pack().size()==1&&BogatyrRecovery.image(wolf).equals(image),
                    "Retired packets/direct calls cannot debit Kibble, heal, breed, summon or regroup");
            for(var mode:WolfMode.values())if(!mode.active())f.check(!BogatyrModes.select(f.p(),f.run,mode),"Legacy mode denied");
            helper.succeed();
        }
    }
    public static void nativeBreeding(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2702)){
            var a=f.wolf(0,0,1,0);var b=f.wolf(1,0,-1,0);
            f.check(BogatyrModes.mode(a)==WolfMode.PATROL&&!a.isOrderedToSit(),"Default Patrol stands native acquired wolves");
            f.check(BogatyrWolfEvents.breedingAllowed(a,b),"Healthy same-run dogs can breed across owners");
            f.check(a.mobInteract(f.current.get(1),net.minecraft.world.InteractionHand.MAIN_HAND)!=null&&!a.isOrderedToSit(),
                    "Ally empty hand cannot sit another owner's dog");
            f.current.get(1).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.BEEF,2));
            a.mobInteract(f.current.get(1),net.minecraft.world.InteractionHand.MAIN_HAND);
            f.check(a.isInLove(),"Ally can feed healthy adult into native breeding");
            f.check(f.current.get(1).getMainHandItem().getCount()==1,"One native food item consumed");
            b.setInLove(f.current.get(1));f.check(a.canMate(b),"Native healthy allies can mate");
            var before=f.pack().stream().map(Wolf::getUUID).collect(java.util.stream.Collectors.toSet());
            var bounds=a.getBoundingBox().inflate(2);
            var priorOrbs=f.level.getEntitiesOfClass(ExperienceOrb.class,bounds).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
            a.spawnChildFromBreeding(f.level,b);
            f.level.getEntitiesOfClass(ExperienceOrb.class,bounds).stream().filter(orb->!priorOrbs.contains(orb.getUUID())).forEach(f.extras::add);
            var pups=f.pack().stream().filter(w->!before.contains(w.getUUID())).toList();
            f.check(pups.size()==1,"Exactly one native pup enrolled");var pup=pups.getFirst();f.remember(pup);
            f.check(pup.isBaby()&&pup.isTame()&&pup.isOwnedBy(f.p())&&pup.getPersistentData().getLongOr(BogatyrWolfEvents.RUN,0)==f.run,
                    "Birth preserves first parent ownership and active-run enrollment");
            f.check(a.getAge()>0&&b.getAge()>0&&!a.isInLove()&&!b.isInLove(),"Native breeding cooldown and love reset preserved");
            helper.succeed();
        }
    }
    public static void recallStay(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2703)){
            var wolf=f.wolf(0,0,6,0);
            for(int dx=3;dx<=6;dx++){
                var pos=net.minecraft.core.BlockPos.containing(f.origin).offset(dx,-1,0);
                f.blocks.putIfAbsent(pos,f.level.getBlockState(pos));f.level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
            f.check(BogatyrModes.select(f.p(),f.run,WolfMode.STAY),"Stay selected");
            var goal=stay(wolf);f.check(goal.canUse(),"Recall goal claims native movement");goal.start();
            f.check(!wolf.isOrderedToSit()&&!wolf.getNavigation().isDone()&&BogatyrModes.anchor(wolf)==null,
                    "Outside five blocks recalls using loaded native navigation before sitting");
            wolf.setPos(f.origin.add(4,0,0));goal.tick();
            var anchor=BogatyrModes.anchor(wolf);
            f.check(anchor!=null&&wolf.isOrderedToSit()&&wolf.isInSittingPose()&&wolf.distanceToSqr(f.p())<=25,"Arrival records a settled anchor inside five blocks");
            f.p().setPos(f.origin.add(-8,0,0));goal.tick();
            f.check(anchor.equals(BogatyrModes.anchor(wolf))&&wolf.position().equals(anchor),"Moving master leaves fixed Stay anchor");
            f.check(BogatyrModes.select(f.p(),f.run,WolfMode.STAY),"Repeated radio selection accepted");
            f.check(anchor.equals(BogatyrModes.anchor(wolf)),"Repeated Stay never re-recalls pack");
            f.check(BogatyrModes.select(f.p(),f.run,WolfMode.PATROL)&&!wolf.isOrderedToSit()&&!wolf.isInSittingPose(),"Patrol releases seating");
            helper.succeed();
        }
    }
    public static void patrolDefense(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2704)){
            var wolf=f.wolf(0,0,1,0);var hostile=enemy(f,EntityType.SKELETON);
            wolf.setTarget(hostile);f.check(wolf.getTarget()==null,"Nearby skeleton is not proactively attacked");
            wolf.tickCount=10;wolf.setLastHurtByMob(hostile);
            var hurt=wolf.targetSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).filter(g->g instanceof HurtByTargetGoal).findFirst().orElseThrow();
            f.check(hurt.canUse(),"Native hurt target goal sees new attack");hurt.start();
            f.check(wolf.getTarget()==hostile,"Native retaliation can target a genuine attacker");
            wolf.tickCount=500;wolf.setLastHurtByMob(null);wolf.setTarget(hostile);
            f.check(wolf.getTarget()==hostile,"Existing native fight survives short damage-history expiry");
            f.check(BogatyrModes.select(f.p(),f.run,WolfMode.STAY),"Stay clears active native fight");stay(wolf).start();
            f.check(wolf.getTarget()==null&&wolf.getLastHurtByMob()==null&&!wolf.isAngry(),"Stay forgets previous target and anger");
            var health=wolf.getHealth();wolf.invulnerableTime=0;
            f.check(wolf.hurtServer(f.level,hostile.damageSources().mobAttack(hostile),2)&&wolf.getHealth()<health,"Hostile damage still hurts a seated wolf");
            f.check(wolf.isOrderedToSit()&&wolf.isInSittingPose()&&wolf.getTarget()==null&&wolf.getLastHurtByMob()==null,
                    "Native damage mixin preserves Stay without retaliation");
            wolf.tickCount=501;wolf.setLastHurtByMob(hostile);wolf.setTarget(hostile);BogatyrModes.apply(wolf,false);
            f.check(wolf.getTarget()==null&&wolf.getLastHurtByMob()==null,"Damage while Stay cannot retaliate");
            f.check(BogatyrModes.select(f.p(),f.run,WolfMode.PATROL),"Return to Patrol");wolf.setTarget(hostile);
            f.check(wolf.getTarget()==null&&!hurt.canUse(),"Old threat cannot restart after Patrol");
            wolf.tickCount=502;wolf.setLastHurtByMob(hostile);
            f.check(hurt.canUse(),"New hit restarts native retaliation");hurt.start();
            f.check(wolf.getTarget()==hostile,"Post-Stay fresh threat is defended");
            selectorTransitions(f,wolf,hostile);
            helper.succeed();
        }
    }
    public static void modePersistence(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2705)){
            var wolf=f.wolf(0,0,2,0);BogatyrModes.select(f.p(),f.run,WolfMode.STAY);stay(wolf).start();
            var anchor=BogatyrModes.anchor(wolf);f.check(anchor!=null&&wolf.isOrderedToSit(),"Stay settles before persistence snapshot");
            wolf.getPersistentData().getCompoundOrEmpty(BogatyrModes.STAY).putString("future","keep");
            var image=f.savedWolf(wolf);wolf.discard();
            f.check(PlayerSaveProof.save(f.p()),"Owner native save");f.reloadOwner();f.p().setPos(f.origin.add(-6,0,0));
            var loaded=f.readWolf(image);
            singleReset(f,()->{
                f.check(f.level.addFreshEntity(loaded),"Native same-run wolf reload");BogatyrModes.apply(loaded,false);stay(loaded).start();
            },"Native entity reload");
            noReset(f,()->{for(int i=0;i<40;i++){BogatyrModes.apply(loaded,false);stay(loaded).tick();}},"Reloaded fixed Stay");
            f.check(anchor.equals(BogatyrModes.anchor(loaded))&&loaded.isOrderedToSit(),"Relog/unload preserves fixed Stay anchor");
            f.check(loaded.getPersistentData().getCompoundOrEmpty(BogatyrModes.STAY).getStringOr("future","").equals("keep"),"Unknown anchor fields survive");
            var data=BogatyrCompanionData.get(f.level.getServer());f.check(data.flushVerified(f.level.getServer()),"Native mode directory save");
            var round=BogatyrCompanionData.CODEC.parse(NbtOps.INSTANCE,data.image()).getOrThrow();
            f.check(round.mode(f.p().getUUID(),f.run).mode()==WolfMode.STAY,"Saved selected mode round trips");
            BogatyrActions.sync(f.p(),true);var view=f.view();
            BogatyrActions.mode(f.p(),new BogatyrPayloads.ModeAction(f.run,view.revision()-1,WolfMode.PATROL));
            f.check(BogatyrModes.mode(loaded)==WolfMode.STAY,"Stale packet denied");
            var original=f.root().copy();var wrong=f.root().copy();wrong.putString(ClassData.KEY_CLASS_ID,"theurgist");f.p().getPersistentData().put(ClassData.ROOT_TAG,wrong);
            f.check(!BogatyrModes.select(f.p(),f.run,WolfMode.PATROL),"Class authority is live");f.p().getPersistentData().put(ClassData.ROOT_TAG,original);
            helper.succeed();
        }
    }
    public static void legacyModes(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2706)){
            var data=BogatyrCompanionData.get(f.level.getServer());
            for(var mode:WolfMode.values())if(!mode.active()){
                var image=data.image();var legacy=new CompoundTag();legacy.putInt("schema",1);var entry=new CompoundTag();
                entry.putString("mode",mode.name());entry.putString("future","keep");legacy.put(f.p().getUUID()+"/"+f.run,entry);image.put(WolfModeData.LEGACY,legacy);
                var loaded=BogatyrCompanionData.CODEC.parse(NbtOps.INSTANCE,image).getOrThrow();
                f.check(loaded.mode(f.p().getUUID(),f.run).mode()==WolfMode.PATROL,"Legacy mode retires to Patrol");
                f.check(loaded.setMode(f.p().getUUID(),f.run,WolfMode.STAY),"New overlay selection");
                f.check(loaded.image().get(WolfModeData.LEGACY).equals(legacy),"Legacy saved data remains byte-shaped");
            }
            helper.succeed();
        }
    }
    public static void lateLoadEpoch(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2707)){
            var wolf=f.wolf(0,0,2,0);BogatyrModes.select(f.p(),f.run,WolfMode.STAY);stay(wolf).start();
            var before=wolf.getPersistentData().getCompoundOrEmpty(BogatyrModes.STAY).copy();var image=f.savedWolf(wolf);wolf.discard();
            BogatyrModes.select(f.p(),f.run,WolfMode.PATROL);BogatyrModes.select(f.p(),f.run,WolfMode.STAY);
            f.p().setPos(f.origin.add(-6,0,0));
            var loaded=f.readWolf(image);
            singleReset(f,()->{
                f.check(f.level.addFreshEntity(loaded),"Late unloaded wolf admitted");BogatyrModes.apply(loaded,false);
            },"Late-load changed Stay epoch");
            noReset(f,()->{for(int i=0;i<40;i++)BogatyrModes.apply(loaded,false);},"Unsettled same-epoch recall");
            f.check(BogatyrModes.anchor(loaded)==null&&!loaded.isOrderedToSit(),"Latest Stay recalls late-loaded wolf rather than old anchor");
            f.check(loaded.getPersistentData().getCompoundOrEmpty(BogatyrModes.STAY).getLongOr("epoch",0)>before.getLongOr("epoch",0),"Saved epoch distinguishes identical mode after intervening Patrol");
            f.p().setHealth(0);stay(loaded).tick();
            f.check(loaded.isOrderedToSit()&&BogatyrModes.anchor(loaded)==null,"Unavailable master pauses recall without inventing settled location");
            f.p().setHealth(20);f.p().setPos(loaded.position());stay(loaded).tick();
            f.check(BogatyrModes.anchor(loaded)!=null,"Available master resumes and settles recall");
            helper.succeed();
        }
    }
    public static void unknownAnchor(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2708)){
            var wolf=f.wolf(0,0,1,0);var future=new CompoundTag();future.putInt("schema",7);future.putString("future","keep");
            wolf.getPersistentData().put(BogatyrModes.STAY,future.copy());BogatyrModes.apply(wolf,false);
            f.check(wolf.isOrderedToSit()&&BogatyrModes.standing(wolf),"Unknown per-wolf state fails passive");
            BogatyrModes.select(f.p(),f.run,WolfMode.STAY);stay(wolf).tick();BogatyrModes.select(f.p(),f.run,WolfMode.PATROL);BogatyrModes.apply(wolf,false);
            f.check(future.equals(wolf.getPersistentData().get(BogatyrModes.STAY))&&wolf.isOrderedToSit(),"Mode switches preserve unsupported saved anchor unchanged");
            helper.succeed();
        }
    }
    public static void workScaling(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2709)){
            var wolves=new ArrayList<Wolf>();for(int i=0;i<32;i++)wolves.add(f.wolf(0,0,i%3-1,i%3-1));
            for(var wolf:wolves){
                f.check(wolf.targetSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof BogatyrThreats.ProtectOwner),"No proactive scan goal is installed");
                f.check(wolf.targetSelector.getAvailableGoals().stream().anyMatch(g->g.getGoal() instanceof OwnerHurtByTargetGoal),"Native owner defense remains installed");
            }
            BogatyrWork.begin();try{
                BogatyrModes.select(f.p(),f.run,WolfMode.STAY);
                for(var wolf:wolves)stay(wolf).start();
                long traversals=BogatyrWork.probe.targetTraversals,goals=BogatyrWork.probe.targetGoals,resets=BogatyrWork.probe.memoryResets;
                f.check(traversals==wolves.size()&&goals>0&&resets>0,"Stay selection traverses each actual native target selector once");
                for(int tick=0;tick<40;tick++)for(var wolf:wolves){BogatyrModes.apply(wolf,false);stay(wolf).tick();}
                f.check(BogatyrWork.probe.scans==0&&BogatyrWork.probe.decisions==0,"Settled pack performs no custom target scans or decisions");
                f.check(BogatyrWork.probe.targetTraversals==traversals&&BogatyrWork.probe.targetGoals==goals
                        &&BogatyrWork.probe.memoryResets==resets,"Settled uncapped pack never repeats target-selector traversal");
                f.check(wolves.stream().allMatch(Wolf::isOrderedToSit),"Entire uncapped player pack settles");
            }finally{BogatyrWork.end();}
            helper.succeed();
        }
    }
    public static void ownerThreatMemory(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-2710)){
            var wolf=f.wolf(0,0,1,0);var hostile=enemy(f,EntityType.ZOMBIE);
            var goal=wolf.targetSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).filter(g->g instanceof OwnerHurtByTargetGoal).findFirst().orElseThrow();
            f.p().tickCount=20;f.p().setLastHurtByMob(hostile);
            f.check(goal.canUse(),"Native owner-defense goal initially sees threat");
            BogatyrModes.select(f.p(),f.run,WolfMode.STAY);stay(wolf).start();
            f.p().tickCount=30;f.p().setLastHurtByMob(hostile);BogatyrModes.apply(wolf,false);
            BogatyrModes.select(f.p(),f.run,WolfMode.PATROL);
            f.check(!goal.canUse()&&wolf.getTarget()==null,"Before/during Stay owner timestamps are forgotten without changing owner state");
            f.check(f.p().getLastHurtByMob()==hostile,"Other owner combat systems keep their own state");
            f.p().setClientLoaded(true);f.p().tickCount=40;f.p().invulnerableTime=0;float health=f.p().getHealth();
            f.check(f.p().hurtServer(f.level,hostile.damageSources().mobAttack(hostile),1)&&f.p().getHealth()<health,
                    "Client-ready native fixture accepts hostile damage");
            f.check(f.p().getLastHurtByMob()==hostile&&f.p().getLastHurtByMobTimestamp()==40,
                    "Fresh hit records the actual attacker and current timestamp");
            f.check(goal.canUse(),"Fresh incoming owner damage restores native defense");goal.start();
            f.check(wolf.getTarget()==hostile,"Fresh owner attacker is accepted by damage-history guard");
            helper.succeed();
        }
    }
}
