package net.goui.cosmicdungeon.mercenary;

import java.util.*;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.*;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryQueryTest {
    private static final AABB BOX=new AABB(0,0,0,4,4,4);
    private record Candidate(int id) implements EntityAccess {
        public int getId(){return id;}
        public UUID getUUID(){return new UUID(0,id);}
        public BlockPos blockPosition(){return BlockPos.ZERO;}
        public AABB getBoundingBox(){return new AABB(0,0,0,1,1,1);}
        public void setLevelCallback(EntityInLevelCallback callback){}
        public Stream<? extends EntityAccess> getSelfAndPassengers(){return Stream.of(this);}
        public Stream<? extends EntityAccess> getPassengersAndSelf(){return Stream.of(this);}
        public void setRemoved(Entity.RemovalReason reason){}
        public boolean shouldBeSaved(){return false;}
        public boolean isRemoved(){return false;}
        public boolean isAlwaysTicking(){return false;}
    }
    private static EntitySection<EntityAccess> section(int count){
        var section=new EntitySection<>(EntityAccess.class,Visibility.TICKING);
        for(int i=1;i<=count;i++)section.add(new Candidate(i));
        return section;
    }
    @Test void nativeRemovalDuringTraversalReproducesReportedCrash(){
        var section=section(3);
        assertThrows(ConcurrentModificationException.class,()->section.getEntities(
            EntityTypeTest.forClass(Candidate.class),BOX,item->{
                section.remove(item);
                return AbortableIterationConsumer.Continuation.CONTINUE;
            }));
    }
    @Test void removalAfterSnapshotProcessesEveryCandidateWithoutNativeIteratorMutation(){
        var section=section(3);var processed=new ArrayList<Integer>();
        assertDoesNotThrow(()->MercenaryBrain.<Candidate>afterSnapshot(24,
            visitor->section.getEntities(EntityTypeTest.forClass(Candidate.class),BOX,visitor),
            item->{processed.add(item.id());assertTrue(section.remove(item));}));
        assertEquals(List.of(1,2,3),processed);assertEquals(0,section.size());
    }
    @Test void mutationsPreserveTheExistingCandidateLimitAndDeferNewEntities(){
        var section=section(30);var processed=new ArrayList<Integer>();
        MercenaryBrain.<Candidate>afterSnapshot(24,
            visitor->section.getEntities(EntityTypeTest.forClass(Candidate.class),BOX,visitor),
            item->{processed.add(item.id());section.remove(item);section.add(new Candidate(100+item.id()));});
        assertEquals(24,processed.size());assertEquals(24,processed.stream().distinct().count());
        assertTrue(processed.stream().allMatch(id->id<=24));assertEquals(30,section.size());
    }
    @Test void nonpositiveLimitsDoNotTraverseOrMutate(){
        for(int limit:List.of(0,-1))MercenaryBrain.afterSnapshot(limit,
            visitor->fail("No query with a nonpositive bound"),item->fail("No action with a nonpositive bound"));
    }
}
