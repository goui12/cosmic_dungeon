package net.goui.cosmicdungeon.playerclass.bogatyr;

import java.util.*;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.goui.cosmicdungeon.mercenary.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;

/** Native identity/count and scheduler regressions; all entities remain in a preloaded fixture chunk. */
public final class BogatyrAdmissionGameTests {
    private BogatyrAdmissionGameTests(){}
    private static Set<UUID> live(BogatyrCommandGameTests.Fixture f){
        return f.level.getEntitiesOfClass(Wolf.class,new AABB(f.origin,f.origin).inflate(7)).stream()
                .filter(w->f.p().getUUID().equals(BogatyrCompanions.owner(w)))
                .map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
    }
    public static void repeatedAdmission(GameTestHelper helper){
        for(int count:List.of(1,30,120))try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-3100-count)){
            var server=f.level.getServer();var data=BogatyrCompanionData.get(server);
            for(int i=0;i<count;i++)f.wolf(0,0,i%5-2,i/5%5-2);
            var expected=live(f);f.check(expected.size()==count,"Exact native player pack size "+count);
            for(int pass=0;pass<3;pass++){
                BogatyrCompanions.preserveRun(server,f.runs.get(f.run));
                for(var wolf:List.copyOf(f.wolves)){
                    BogatyrCompanions.added(wolf);
                    BogatyrWolfEvents.register(wolf,f.p().getUUID(),f.run);
                    BogatyrRecovery.onObserved(wolf);
                }
                BogatyrActions.sync(f.p(),true);
                f.check(BogatyrRecovery.recover(f.p())==0&&BogatyrRecovery.call(f.p())==0,"Retired recovery never creates wolves");
                f.check(expected.equals(live(f))&&f.pack().size()==count&&data.forOwner(f.p().getUUID()).size()==count,
                        "Repeated start/admission/recovery preserves exact identities for "+count+" wolves");
            }
            var wolf=f.wolves.getFirst();var bond=BogatyrIdentity.id(wolf);var image=BogatyrRecovery.image(wolf);
            wolf.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            f.check(!live(f).contains(wolf.getUUID())&&data.find(bond).isPresent(),"Unload retains directory without replacement");
            var restored=f.readWolf(image);
            f.check(f.level.addFreshEntity(restored)&&expected.equals(live(f))&&BogatyrIdentity.id(restored).equals(bond),
                    "Native reload restores exactly the same player identity above any old cap");
        }
        helper.succeed();
    }
    @SuppressWarnings("unchecked")
    public static void duplicateIdentity(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-3102)){
            var original=f.wolf(0,0,1,1);var bond=BogatyrIdentity.id(original);var expected=live(f);
            var image=BogatyrRecovery.image(original);var duplicateId=UUID.randomUUID();
            image.store("UUID",UUIDUtil.CODEC,duplicateId);var clone=f.readWolf(image);
            var data=BogatyrCompanionData.get(f.level.getServer());
            try{
                f.check(!f.level.addFreshEntity(clone)&&f.level.getEntity(duplicateId)==null,
                        "Conflicting copied bond is rejected before native insertion");
                f.check(expected.equals(live(f))&&data.find(bond).orElseThrow().entityUuid().equals(original.getUUID())
                        &&data.identityHeld(f.level.dimension().location().toString(),duplicateId),
                        "Original wolf and directory survive; duplicate is held for review");
                f.check(!f.level.addFreshEntity(clone)&&expected.equals(live(f)),"Repeated duplicate admission stays rejected");
            }finally{
                try{
                    var field=BogatyrCompanionData.class.getDeclaredField("identityHolds");field.setAccessible(true);
                    ((Set<String>)field.get(data)).remove(f.level.dimension().location()+"|"+duplicateId);
                }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            }
        }
        helper.succeed();
    }
    private static CompoundTag saved(MercenaryEntity merc){
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,merc.registryAccess());
        merc.addAdditionalSaveData(output);return output.buildResult();
    }
    private static void clock(MercenaryEntity merc,int ticks){
        var image=saved(merc);image.putInt("mercenary_wolf_ticks",ticks);
        merc.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,merc.registryAccess(),image));
    }
    public static void mercenaryScheduler(GameTestHelper helper){
        try(var f=new BogatyrCommandGameTests.Fixture(helper,Long.MAX_VALUE-3103,"dungeon_1")){
            var data=D1RunData.get(f.level.getServer());
            var contract=new MercenaryContract(UUID.randomUUID(),f.p().getUUID(),"bogatyr",2,50);
            f.runs.put(f.run,f.runs.get(f.run).withMercenaries(List.of(contract)));
            var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),f.level);
            merc.initialize(f.run,contract);merc.setPos(f.origin);merc.setNoAi(true);f.extras.add(merc);
            String key="mercenary_wolves:"+contract.id();
            try{
            var center=BlockPos.containing(f.origin);
            // Deny all candidate placements without changing chunks or authored data.
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=2;y++)
                f.level.setBlockAndUpdate(center.offset(x,y,z),Blocks.STONE.defaultBlockState());
            for(int tick=10;tick<=590;tick+=10){
                merc.tickCount=tick;MercenaryWolves.tick(merc,f.level,f.p(),10);
                f.check(data.values(f.run,key).isEmpty(),"No summon before thirty active seconds");
            }
            merc.tickCount=600;MercenaryWolves.tick(merc,f.level,f.p(),10);
            f.check(data.values(f.run,key).isEmpty()&&saved(merc).getIntOr("mercenary_wolf_ticks",-1)==20,
                    "Failed placement reserves no identity and retries after one second");
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=2;y++)
                f.level.setBlockAndUpdate(center.offset(x,y,z),Blocks.AIR.defaultBlockState());
            merc.tickCount+=10;MercenaryWolves.tick(merc,f.level,f.p(),10);
            f.check(data.values(f.run,key).isEmpty(),"Failure does not retry early");
            merc.tickCount+=10;MercenaryWolves.tick(merc,f.level,f.p(),10);
            f.check(data.values(f.run,key).size()==1,"One actual successful summon after retry");
            int before=saved(merc).getIntOr("mercenary_wolf_ticks",-1);
            for(int i=0;i<100;i++)MercenaryWolves.tick(merc,f.level,f.p(),Integer.MAX_VALUE);
            f.check(data.values(f.run,key).size()==1&&saved(merc).getIntOr("mercenary_wolf_ticks",-1)==before,
                    "Repeated callback cannot advance time or burst-spawn");
            merc.tickCount+=10;MercenaryWolves.tick(merc,f.level,f.p(),Integer.MAX_VALUE);
            f.check(saved(merc).getIntOr("mercenary_wolf_ticks",-1)==before-10,"Long elapsed input never catches up offline time");
            var playerWolf=f.wolf(0,0,3,3);
            for(int i=1;i<20;i++){
                clock(merc,0);merc.tickCount+=10;MercenaryWolves.tick(merc,f.level,f.p(),10);
                f.check(data.values(f.run,key).size()==i+1,"Each successful insertion admits one mercenary dog");
            }
            clock(merc,0);merc.tickCount+=10;MercenaryWolves.tick(merc,f.level,f.p(),10);
            f.check(data.values(f.run,key).size()==20&&f.pack().equals(List.of(playerWolf)),
                    "Mercenary cap includes all saved identities and never consumes player pack slots");
            // Dismissal marks all first; bounded native validation retires the remainder.
            var dogs=data.values(f.run,key).stream().map(UUID::fromString).map(f.level::getEntity)
                    .map(e->(Wolf)e).toList();f.extras.addAll(dogs);
            MercenaryWolves.dismiss(f.level.getServer(),f.runs.get(f.run),contract);
            for(var dog:dogs){
                dog.tickCount=40;
                MercenaryWolves.tickWolf(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(dog));
            }
            f.check(dogs.stream().allMatch(Entity::isRemoved)&&!playerWolf.isRemoved(),"Whole mercenary pack retires independently");
            }finally{
                for(var id:data.values(f.run,key)){
                    var entity=f.level.getEntity(UUID.fromString(id));
                    if(entity!=null)f.extras.add(entity);
                }
            }
        }
        helper.succeed();
    }
}
