package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.goui.cosmicdungeon.entity.ModEntities;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryWolfScalingTest {
    @org.junit.jupiter.api.BeforeAll static void config()throws Exception{MercenaryEntryTest.config();}
    @org.junit.jupiter.api.AfterAll static void unload(){MercenaryEntryTest.unloadConfig();}
    @SuppressWarnings("unchecked") private static Codec<D1RunData> codec()throws Exception{
        var field=D1RunData.class.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<D1RunData>)field.get(null);
    }
    private static D1RunData empty()throws Exception{return codec().parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();}
    private static D1RunData reload(D1RunData data)throws Exception{
        return codec().parse(NbtOps.INSTANCE,codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();
    }
    private static MercenaryEntity merc(){
        var merc=new MercenaryEntity(ModEntities.MERCENARY.get(),null);
        merc.initialize(42,new MercenaryContract(UUID.randomUUID(),UUID.randomUUID(),"bogatyr",2,50));return merc;
    }
    private static MercenaryWolves.Bond bond(MercenaryEntity merc){
        return new MercenaryWolves.Bond(merc.runId(),merc.contract().id(),merc.contract().hirer());
    }
    private static void progress(D1RunData data,MercenaryWolves.Bond bond,int level){
        data.setCount(bond.run(),MercenarySkills.key(bond.mercenary(),MercenarySkill.WOLVES),(int)MercenarySkill.threshold(level));
    }
    @Test void capacityIsTwentyAtEverySkillLevel(){
        for(int level:List.of(Integer.MIN_VALUE,0,1,2,3,5,10,25,50,Integer.MAX_VALUE))
            assertEquals(20,MercenaryWolfBalance.capacity(level));
    }
    @Test void approvedCadenceImprovesFromThirtySecondsWithTwentySecondFloor(){
        assertEquals(600,MercenaryWolfBalance.interval(1));
        assertTrue(MercenaryWolfBalance.interval(2)<600);
        int previous=600;
        for(int level=1;level<=65536;level++){
            int interval=MercenaryWolfBalance.interval(level);
            assertTrue(interval>=400&&interval<=previous);previous=interval;
        }
        assertEquals(400,MercenaryWolfBalance.interval(Integer.MAX_VALUE));
        assertEquals(600,MercenaryWolfBalance.interval(Integer.MIN_VALUE));
        assertEquals(0,MercenaryWolfBalance.remaining(10,10,25));
        assertEquals(10,MercenaryWolfBalance.remaining(20,10,25));
        assertEquals(MercenaryWolfBalance.interval(25)-10,MercenaryWolfBalance.remaining(2400,10,25));
    }
    @Test void capacityCountsUnloadedWolvesAcrossReloadAndNewRunsReset()throws Exception{
        var data=empty();var bond=bond(merc());progress(data,bond,10);
        var ids=new ArrayList<UUID>();
        for(int i=0;i<20;i++){var id=UUID.randomUUID();ids.add(id);assertTrue(MercenaryWolves.remember(data,bond,id));}
        data=reload(data);
        assertEquals(20,MercenaryWolves.capacity(data,bond));
        assertFalse(MercenaryWolves.remember(data,bond,UUID.randomUUID()));
        assertTrue(MercenaryWolves.remember(data,bond,ids.getLast()));
        MercenaryWolves.died(data,bond,ids.getLast(),Entity.RemovalReason.UNLOADED_TO_CHUNK);
        assertFalse(MercenaryWolves.remember(data,bond,UUID.randomUUID()));
        MercenaryWolves.died(data,bond,ids.getLast(),Entity.RemovalReason.KILLED);
        assertTrue(MercenaryWolves.remember(data,bond,UUID.randomUUID()));
        assertEquals(20,MercenaryWolves.capacity(data,new MercenaryWolves.Bond(43,bond.mercenary(),bond.hirer())));
    }
    @Test void legacyExcessIsPreservedAndOtherPacksAreIndependent()throws Exception{
        var data=empty();var bond=bond(merc());progress(data,bond,50);var ids=new ArrayList<UUID>();
        // Actual old-save shape: the previously approved level50 cap admitted29 identities.
        for(int i=0;i<29;i++){var id=UUID.randomUUID();ids.add(id);data.recordUnique(42,MercenaryWolves.key(bond.mercenary()),id.toString());}
        data=reload(data);progress(data,bond,1);
        for(var id:ids)assertTrue(MercenaryWolves.remember(data,bond,id));
        assertEquals(29,data.values(42,MercenaryWolves.key(bond.mercenary())).size());
        assertFalse(MercenaryWolves.remember(data,bond,UUID.randomUUID()));
        var other=new MercenaryWolves.Bond(42,UUID.randomUUID(),bond.hirer());
        assertTrue(MercenaryWolves.remember(data,other,UUID.randomUUID()));
        assertEquals(20,MercenaryWolves.capacity(data,other));
        data.setCount(42,MercenaryWolves.dismissed(bond.mercenary()),1);
        assertFalse(MercenaryWolves.remember(reload(data),bond,ids.getFirst()));
    }
    @Test void repeatedNativeTickIsIdempotentAndLongElapsedDoesNotCatchUp(){
        var merc=merc();
        assertTrue(merc.beginWolfTick());assertFalse(merc.beginWolfTick());
        merc.tickCount+=10;assertTrue(merc.beginWolfTick());assertFalse(merc.beginWolfTick());
        assertEquals(590,MercenaryWolfBalance.remaining(600,Integer.MAX_VALUE,1));
    }
    @Test void commandsReachWolvesBeyondFiveWithoutIncreasingPerDecisionWork(){
        var merc=merc();var roster=new ArrayList<String>();var wolves=new HashMap<UUID,Wolf>();
        for(int i=0;i<9;i++){
            var wolf=new MercenaryTestWolf();MercenaryWolves.mark(wolf,bond(merc));
            roster.add(wolf.getUUID().toString());wolves.put(wolf.getUUID(),wolf);
        }
        var enemy=new Zombie(EntityType.ZOMBIE,null);var lookups=new AtomicInteger();
        java.util.function.Function<UUID,Entity> find=id->{lookups.incrementAndGet();return wolves.get(id);};
        MercenaryWolves.command(merc,roster,find,enemy);
        assertEquals(5,lookups.get());assertEquals(5,wolves.values().stream().filter(w->w.getTarget()==enemy).count());
        lookups.set(0);MercenaryWolves.command(merc,roster,find,enemy);
        assertEquals(5,lookups.get());assertTrue(wolves.values().stream().allMatch(w->w.getTarget()==enemy));
        assertDoesNotThrow(()->MercenaryWolves.command(merc,List.of(),find,enemy));
        assertDoesNotThrow(()->MercenaryWolves.command(merc,roster.subList(0,1),find,enemy));
    }
    @Test void unloadedInvalidSittingAndForeignEntriesDoNotStealCommandsOrStallRotation(){
        var merc=merc();var own=new MercenaryTestWolf();MercenaryWolves.mark(own,bond(merc));
        var sitting=new MercenaryTestWolf();MercenaryWolves.mark(sitting,bond(merc));sitting.setOrderedToSit(true);
        var foreign=new MercenaryTestWolf();MercenaryWolves.mark(foreign,bond(merc()));
        var roster=List.of("invalid",UUID.randomUUID().toString(),sitting.getUUID().toString(),
                foreign.getUUID().toString(),UUID.randomUUID().toString(),own.getUUID().toString());
        var wolves=Map.of(own.getUUID(),own,sitting.getUUID(),sitting,foreign.getUUID(),foreign);
        var enemy=new Zombie(EntityType.ZOMBIE,null);
        MercenaryWolves.command(merc,roster,wolves::get,enemy);
        assertNull(own.getTarget());MercenaryWolves.command(merc,roster,wolves::get,enemy);
        assertSame(enemy,own.getTarget());assertNull(sitting.getTarget());assertNull(foreign.getTarget());
    }
}
