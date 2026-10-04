package net.goui.cosmicdungeon.mercenary;

import com.mojang.serialization.Codec;
import java.util.*;
import net.goui.cosmicdungeon.dungeon.DungeonRunRegistryData;
import net.goui.cosmicdungeon.dungeon.d1.D1RunData;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.storage.*;
import net.neoforged.neoforge.event.entity.living.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MercenaryWolvesTest {
    private static final RegistryAccess LOOKUP=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private final UUID merc=UUID.randomUUID(),hirer=UUID.randomUUID();
    private MercenaryWolves.Bond bond(){return new MercenaryWolves.Bond(42,merc,hirer);}
    @SuppressWarnings("unchecked") private static Codec<D1RunData> codec()throws Exception{
        var field=D1RunData.class.getDeclaredField("CODEC");field.setAccessible(true);return (Codec<D1RunData>)field.get(null);
    }
    private static D1RunData empty()throws Exception{return codec().parse(NbtOps.INSTANCE,new CompoundTag()).getOrThrow();}
    private static D1RunData reload(D1RunData data)throws Exception{return codec().parse(NbtOps.INSTANCE,codec().encodeStart(NbtOps.INSTANCE,data).getOrThrow()).getOrThrow();}
    private Wolf wolf(){var wolf=new MercenaryTestWolf();MercenaryWolves.mark(wolf,bond());return wolf;}
    private DungeonRunRegistryData.RunRecord run(String role){
        return new DungeonRunRegistryData.RunRecord(42,"dungeon_1","minecraft:overworld",0,
                List.of("cosmicdungeon:d1_instance_42"),1,"ACTIVE","",0,List.of(hirer),List.of(),List.of())
                .withMercenaries(List.of(new MercenaryContract(merc,hirer,role,2,500)));
    }
    @Test void twoMinuteClockDoesNotSummonEarlyAndOldSavesStartAtFullInterval(){
        int ticks=MercenaryWolves.INTERVAL;assertEquals(2400,ticks);
        for(int i=0;i<239;i++)ticks=MercenaryWolves.advance(ticks,10);
        assertEquals(10,ticks);assertEquals(0,MercenaryWolves.advance(ticks,10));
        assertEquals(0,MercenaryWolves.advance(0,10));
        var old=TagValueInput.create(ProblemReporter.DISCARDING,LOOKUP,new CompoundTag());
        assertEquals(2400,MercenaryWolves.loadCooldown(old));
    }
    @Test void remainingSummonClockSurvivesNativeNbtWithoutOfflineCatchup(){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,LOOKUP);
        out.putInt("mercenary_wolf_ticks",1370);
        assertEquals(1370,MercenaryWolves.loadCooldown(TagValueInput.create(ProblemReporter.DISCARDING,LOOKUP,out.buildResult())));
        out.putInt("mercenary_wolf_ticks",Integer.MAX_VALUE);
        assertEquals(2400,MercenaryWolves.loadCooldown(TagValueInput.create(ProblemReporter.DISCARDING,LOOKUP,out.buildResult())));
    }
    @Test void unloadedPackIsCappedAcrossSaveReloadAndDuplicateAdmission(){
        var ids=new ArrayList<UUID>();
        assertDoesNotThrow(()->{
            var data=empty();
            for(int i=0;i<5;i++){var id=UUID.randomUUID();ids.add(id);assertTrue(MercenaryWolves.remember(data,bond(),id));}
            data=reload(data);
            assertEquals(5,data.values(42,MercenaryWolves.key(merc)).size());
            assertFalse(MercenaryWolves.remember(data,bond(),UUID.randomUUID()));
            assertTrue(MercenaryWolves.remember(data,bond(),ids.getFirst()));
            assertEquals(5,data.values(42,MercenaryWolves.key(merc)).size());
        });
    }
    @Test void OnlyObservedDeathReleasesASlotNotUnloadOrDimensionChange()throws Exception{
        var data=empty();var id=UUID.randomUUID();assertTrue(MercenaryWolves.remember(data,bond(),id));
        for(var reason:List.of(Entity.RemovalReason.UNLOADED_TO_CHUNK,Entity.RemovalReason.CHANGED_DIMENSION,Entity.RemovalReason.DISCARDED)){
            MercenaryWolves.died(data,bond(),id,reason);assertEquals(1,data.values(42,MercenaryWolves.key(merc)).size());
        }
        MercenaryWolves.died(data,bond(),id,Entity.RemovalReason.KILLED);
        assertTrue(data.values(42,MercenaryWolves.key(merc)).isEmpty());
        assertTrue(MercenaryWolves.remember(reload(data),bond(),UUID.randomUUID()));
    }
    @Test void packsAreIndependentAndDismissalSurvivesReload()throws Exception{
        var data=empty();var other=new MercenaryWolves.Bond(42,UUID.randomUUID(),hirer);
        for(int i=0;i<5;i++)assertTrue(MercenaryWolves.remember(data,bond(),UUID.randomUUID()));
        assertTrue(MercenaryWolves.remember(data,other,UUID.randomUUID()));
        data.setCount(42,MercenaryWolves.dismissed(merc),1);data=reload(data);
        assertFalse(MercenaryWolves.remember(data,bond(),UUID.randomUUID()));
        assertTrue(MercenaryWolves.remember(data,other,UUID.randomUUID()));
    }
    @Test void onlyAnActiveBogatyrContractInItsOwnRunAndDimensionAdmitsDogs(){
        var run=run("bogatyr");
        assertTrue(MercenaryWolves.admitted(run,bond(),hirer,"cosmicdungeon:d1_instance_42"));
        assertFalse(MercenaryWolves.admitted(run("theurgist"),bond(),hirer,"cosmicdungeon:d1_instance_42"));
        assertFalse(MercenaryWolves.admitted(run,bond(),UUID.randomUUID(),"cosmicdungeon:d1_instance_42"));
        assertFalse(MercenaryWolves.admitted(run,bond(),hirer,"minecraft:overworld"));
        assertFalse(MercenaryWolves.admitted(run.withCompletionExited(hirer),bond(),hirer,"cosmicdungeon:d1_instance_42"));
        assertFalse(MercenaryWolves.admitted(run.withoutPlayer(hirer),bond(),hirer,"cosmicdungeon:d1_instance_42"));
        assertFalse(MercenaryWolves.admitted(null,bond(),hirer,"cosmicdungeon:d1_instance_42"));
    }
    @Test void markerRoundTripsAndMalformedMarkerCannotBecomeAnOrdinaryUnprotectedWolf(){
        var wolf=wolf();assertEquals(bond(),MercenaryWolves.bond(wolf));assertTrue(MercenaryWolves.managed(wolf));
        var saved=wolf.getPersistentData().copy();var loaded=new MercenaryTestWolf();loaded.getPersistentData().merge(saved);
        assertEquals(bond(),MercenaryWolves.bond(loaded));
        loaded.getPersistentData().putString(MercenaryWolves.MARKER,"bad");
        assertTrue(MercenaryWolves.managed(loaded));assertNull(MercenaryWolves.bond(loaded));
        assertTrue(MercenaryWolves.protectedCompanion(loaded));
    }
    @Test void aRosterEntryCannotCommandOrDismissAnotherMercenarysDog(){
        var wolf=wolf();var contract=new MercenaryContract(merc,hirer,"bogatyr",2,500);
        assertTrue(MercenaryWolves.belongs(wolf,42,contract));
        assertFalse(MercenaryWolves.belongs(wolf,43,contract));
        assertFalse(MercenaryWolves.belongs(wolf,42,new MercenaryContract(UUID.randomUUID(),hirer,"bogatyr",2,500)));
        assertFalse(MercenaryWolves.belongs(wolf,42,new MercenaryContract(merc,UUID.randomUUID(),"bogatyr",2,500)));
    }
    @Test void summonedWolvesNeverTargetFriendlyEntitiesButCanTargetHostiles(){
        var wolf=wolf();var ally=new IronGolem(EntityType.IRON_GOLEM,null);var enemy=new Zombie(EntityType.ZOMBIE,null);
        var friendly=new LivingChangeTargetEvent(wolf,ally,LivingChangeTargetEvent.LivingTargetType.MOB_TARGET);
        MercenaryWolves.targeting(friendly);assertNull(friendly.getNewAboutToBeSetTarget());
        var hostile=new LivingChangeTargetEvent(wolf,enemy,LivingChangeTargetEvent.LivingTargetType.MOB_TARGET);
        MercenaryWolves.targeting(hostile);assertSame(enemy,hostile.getNewAboutToBeSetTarget());
    }
    @Test void breedingCannotCreateExtraMercenaryDogs(){
        var parent=wolf();var other=new MercenaryTestWolf();var child=new MercenaryTestWolf();
        var event=new BabyEntitySpawnEvent(parent,other,child);
        MercenaryWolves.breeding(event);assertTrue(event.isCanceled());
        var ordinary=new BabyEntitySpawnEvent(other,new MercenaryTestWolf(),child);
        MercenaryWolves.breeding(ordinary);assertFalse(ordinary.isCanceled());
    }
}
