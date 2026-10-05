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
    @Test void baselineAndEverySecondLevelUpKeepTheRequestedPackGrowth(){
        assertEquals(5,MercenaryWolfBalance.capacity(1));assertEquals(5,MercenaryWolfBalance.capacity(2));
        assertEquals(6,MercenaryWolfBalance.capacity(3));assertEquals(6,MercenaryWolfBalance.capacity(4));
        assertEquals(7,MercenaryWolfBalance.capacity(5));assertEquals(9,MercenaryWolfBalance.capacity(10));
        assertEquals(17,MercenaryWolfBalance.capacity(25));assertEquals(29,MercenaryWolfBalance.capacity(50));
        assertEquals(5,MercenaryWolfBalance.capacity(Integer.MIN_VALUE));
        assertEquals(32772,MercenaryWolfBalance.capacity(Integer.MAX_VALUE));
    }
    @Test void cadenceImprovesAndCannotOverflowOrCreateABurst(){
        assertEquals(2400,MercenaryWolfBalance.interval(1));
        assertTrue(MercenaryWolfBalance.interval(2)<2400);
        int previous=2400;
        for(int level=1;level<=65536;level++){
            int interval=MercenaryWolfBalance.interval(level);
            assertTrue(interval>=400&&interval<=previous);previous=interval;
        }
        assertEquals(400,MercenaryWolfBalance.interval(Integer.MAX_VALUE));
        assertEquals(2400,MercenaryWolfBalance.interval(Integer.MIN_VALUE));
        assertEquals(0,MercenaryWolfBalance.remaining(10,10,25));
        assertEquals(10,MercenaryWolfBalance.remaining(20,10,25));
        assertEquals(MercenaryWolfBalance.interval(25)-10,MercenaryWolfBalance.remaining(2400,10,25));
    }
    @Test void scaledCapacityCountsUnloadedWolvesAcrossReloadAndNewRunsReset()throws Exception{
        var data=empty();var bond=bond(merc());progress(data,bond,10);
        var ids=new ArrayList<UUID>();
        for(int i=0;i<9;i++){var id=UUID.randomUUID();ids.add(id);assertTrue(MercenaryWolves.remember(data,bond,id));}
        data=reload(data);
        assertEquals(9,MercenaryWolves.capacity(data,bond));
        assertFalse(MercenaryWolves.remember(data,bond,UUID.randomUUID()));
        assertTrue(MercenaryWolves.remember(data,bond,ids.getLast()));
        MercenaryWolves.died(data,bond,ids.getLast(),Entity.RemovalReason.UNLOADED_TO_CHUNK);
        assertFalse(MercenaryWolves.remember(data,bond,UUID.randomUUID()));
        MercenaryWolves.died(data,bond,ids.getLast(),Entity.RemovalReason.KILLED);
        assertTrue(MercenaryWolves.remember(data,bond,UUID.randomUUID()));
        assertEquals(5,MercenaryWolves.capacity(data,new MercenaryWolves.Bond(43,bond.mercenary(),bond.hirer())));
    }
    @Test void existingWolvesAreNotDeletedByADowngradedCounterAndOtherPacksAreIndependent()throws Exception{
        var data=empty();var bond=bond(merc());progress(data,bond,5);var last=UUID.randomUUID();
        for(int i=0;i<6;i++)assertTrue(MercenaryWolves.remember(data,bond,UUID.randomUUID()));
        assertTrue(MercenaryWolves.remember(data,bond,last));progress(data,bond,1);
        assertTrue(MercenaryWolves.remember(data,bond,last));
        assertFalse(MercenaryWolves.remember(data,bond,UUID.randomUUID()));
        assertEquals(5,MercenaryWolves.capacity(data,new MercenaryWolves.Bond(42,UUID.randomUUID(),bond.hirer())));
        data.setCount(42,MercenaryWolves.dismissed(bond.mercenary()),1);
        assertFalse(MercenaryWolves.remember(reload(data),bond,last));
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
