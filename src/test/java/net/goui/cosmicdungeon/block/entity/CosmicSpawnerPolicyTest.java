package net.goui.cosmicdungeon.block.entity;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.goui.cosmicdungeon.block.ModBlocks;
import net.goui.cosmicdungeon.mixin.BaseSpawnerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.TagValueInput;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CosmicSpawnerPolicyTest {
    private static RegistryAccess lookup() { return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY); }
    private static CosmicSpawnerBlockEntity spawner() {
        return new CosmicSpawnerBlockEntity(new BlockPos(12, 60, -9),
                ModBlocks.COSMIC_MOB_SPAWNER.get().defaultBlockState());
    }
    private static void load(CosmicSpawnerBlockEntity be, CompoundTag tag) {
        be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, lookup(), tag));
    }
    @Test void fixedOrderIsTopNorthWestSouthEastThenDiagonals() {
        var p = new BlockPos(-17, 70, 33);
        assertEquals(List.of(p.above(),p.north(),p.west(),p.south(),p.east(),
                p.north().east(),p.north().west(),p.south().west(),p.south().east()),
                CosmicSpawnerPlacement.candidates(p));
    }
    @Test void obstructionFallsThroughEveryPositionWithoutExpandingTheSearch() {
        var p = BlockPos.ZERO;
        var choices = CosmicSpawnerPlacement.candidates(p);
        for (int i=0;i<choices.size();i++) {
            int first=i; var calls=new AtomicInteger();
            assertEquals(choices.get(i), CosmicSpawnerPlacement.firstClear(p, candidate -> {
                calls.incrementAndGet(); return choices.indexOf(candidate)>=first;
            }));
            assertEquals(i+1,calls.get());
        }
        var calls=new AtomicInteger();
        assertNull(CosmicSpawnerPlacement.firstClear(p,candidate->{calls.incrementAndGet();return false;}));
        assertEquals(9,calls.get());
        assertEquals(p.above(),CosmicSpawnerPlacement.firstClear(p,candidate->true));
    }
    @Test void fullMobSizeMattersForLowCeilings() {
        var p=BlockPos.ZERO;
        var result=CosmicSpawnerPlacement.firstClear(p,candidate->{
            var box=new net.minecraft.world.phys.AABB(candidate.getX()+0.1,candidate.getY(),
                    candidate.getZ()+0.1,candidate.getX()+0.9,candidate.getY()+2.9,candidate.getZ()+0.9);
            var ceiling=new net.minecraft.world.phys.AABB(0,3,0,1,4,1);
            return !box.intersects(ceiling);
        });
        assertEquals(p.north(),result);
    }
    @Test void nativeDelayInvokerAndOwnerAreAvailableWithoutAWorld() {
        var be=spawner();
        assertInstanceOf(BaseSpawnerAccess.class,be.getSpawner());
        assertSame(be,be.getSpawner().getOwner().left().orElseThrow());
    }
    @Test void blockedStateSynchronizesButIsNotDurableAndRetriesAreBounded() {
        var be=spawner();be.setSpawnBlocked(true);be.deferPlacement(100);
        assertFalse(be.mayRetryPlacement(109));assertTrue(be.mayRetryPlacement(110));
        assertFalse(be.saveWithoutMetadata(lookup()).contains("CosmicSpawnBlocked"));
        var client=spawner();load(client,be.getUpdateTag(lookup()));assertTrue(client.isSpawnBlocked());
        be.setSpawnBlocked(false);load(client,be.getUpdateTag(lookup()));assertFalse(client.isSpawnBlocked());
    }
    @Test void legacyPlacedSpawnerRetainsConfigurationAndAuthoredGear() {
        var original=spawner();var preset=new CosmicSpawnerPreset();
        preset.setEntityTypeId(ResourceLocation.withDefaultNamespace("skeleton"));
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(11);
        sword.set(DataComponents.CUSTOM_NAME,Component.literal("Authored weapon"));
        preset.setEquipment(CosmicSpawnerPreset.Slot.MAINHAND,sword);
        preset.setDropChance(CosmicSpawnerPreset.Slot.MAINHAND,0.35F);
        original.setSpawnerPreset(preset);original.setSpawnerSpawnRange(19);
        original.setSpawnerSpawnCount(7);original.setSpawnerDelayRange(81,119);
        original.setSpawnerDelayTicks(55);original.setSpawnerMobCap(23);original.setBossOneShot(true);
        var saved=original.saveWithoutMetadata(lookup());saved.remove(CosmicSpawnerBlockEntity.DATA_VERSION_KEY);
        var restored=spawner();load(restored,saved);
        assertEquals(19,restored.getSpawnerSpawnRange());assertEquals(7,restored.getSpawnerSpawnCount());
        assertEquals(81,restored.getSpawnerMinSpawnDelay());assertEquals(119,restored.getSpawnerMaxSpawnDelay());
        assertEquals(55,restored.getSpawnerDelayTicks());assertEquals(23,restored.getSpawnerMobCap());
        assertTrue(restored.isBossOneShot());assertFalse(restored.hasBossSpawned());
        assertTrue(ItemStack.matches(sword,restored.getSpawnerPreset().getEquipment(CosmicSpawnerPreset.Slot.MAINHAND)));
        assertEquals(0.35F,restored.getSpawnerPreset().getDropChance(CosmicSpawnerPreset.Slot.MAINHAND));
        var again=spawner();load(again,restored.saveWithoutMetadata(lookup()));
        assertEquals(restored.saveWithoutMetadata(lookup()),again.saveWithoutMetadata(lookup()));
    }
    @Test void absoluteSpawnNbtAndWeightedPotentialsSurviveRoundTrip() {
        var saved=spawner().saveWithoutMetadata(lookup());
        var entity=new CompoundTag();entity.putString("id","minecraft:zombie");
        entity.putString("CustomName","authored");
        var pos=new ListTag();pos.add(DoubleTag.valueOf(987));pos.add(DoubleTag.valueOf(77));pos.add(DoubleTag.valueOf(654));
        entity.put("Pos",pos);
        var spawn=new CompoundTag();spawn.put("entity",entity);
        var weighted=new CompoundTag();weighted.putInt("weight",3);weighted.put("data",spawn.copy());
        var potentials=new ListTag();potentials.add(weighted);
        saved.put("SpawnData",spawn);saved.put("SpawnPotentials",potentials);
        saved.putString("SpawnerEntityId","minecraft:zombie");
        var be=spawner();load(be,saved);var round=be.saveWithoutMetadata(lookup());
        assertEquals(pos,round.getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity").get("Pos"));
        assertEquals(potentials,round.get("SpawnPotentials"));
    }
    @Test void detectionRequiresAnEligibleVisibleNearbyPlayer() {
        assertTrue(CosmicSpawnerAwareness.detects(64,true,true));
        assertFalse(CosmicSpawnerAwareness.detects(64.01,true,true));
        assertFalse(CosmicSpawnerAwareness.detects(4,false,true));
        assertFalse(CosmicSpawnerAwareness.detects(4,true,false));
    }
    @Test void passengerGroupsCannotOverflowTheLivingCap() {
        assertTrue(CosmicSpawnerRuntime.fitsCap(5,3,2));
        assertFalse(CosmicSpawnerRuntime.fitsCap(5,4,2));
        assertFalse(CosmicSpawnerRuntime.fitsCap(5,5,1));
        assertTrue(CosmicSpawnerRuntime.fitsCap(0,500,4));
        assertFalse(CosmicSpawnerRuntime.fitsCap(Integer.MAX_VALUE,Integer.MAX_VALUE,1));
    }
    @Test void freshAwarenessIsIndependentAndSurvivesNativeNbtCopy() {
        var older=new CompoundTag();older.putBoolean(CosmicSpawnerAwareness.AWAKE,true);
        var fresh=new CompoundTag();fresh.putBoolean(CosmicSpawnerAwareness.AWAKE,false);
        assertTrue(CosmicSpawnerAwareness.awake(older,false));
        assertFalse(CosmicSpawnerAwareness.awake(fresh,true)); // distant alert cannot change explicit dormancy
        assertFalse(CosmicSpawnerAwareness.awake(fresh.copy(),true));
        fresh.putBoolean(CosmicSpawnerAwareness.AWAKE,true); // direct damage or local detection
        assertTrue(CosmicSpawnerAwareness.awake(fresh.copy(),false));
        assertTrue(CosmicSpawnerAwareness.awake(new CompoundTag(),true));
        assertFalse(CosmicSpawnerAwareness.awake(new CompoundTag(),false));
    }
    @Test void fightingAndIdleMembersBothCountAgainstTheSameCap() {
        var index=new SpawnerMembership<Object>();var first=new Object();var second=new Object();
        var firstId=UUID.randomUUID();var secondId=UUID.randomUUID();var tag="cosmic_spawner_1_2_3";
        index.put(firstId,first,Set.of(tag),true);index.put(secondId,second,Set.of(tag),true);
        assertEquals(2,index.alive(tag));
        index.alive(firstId,first,false);assertEquals(1,index.alive(tag));
        index.alive(firstId,first,true);assertEquals(2,index.alive(tag));
    }
}
