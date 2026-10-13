package net.goui.cosmicdungeon.playerclass.d1;

import net.goui.cosmicdungeon.block.entity.*;
import net.goui.cosmicdungeon.dungeon.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

/** Actual loaded block entities with saved legacy payloads; no forced/unloaded chunk access. */
public final class SpawnerMigrationGameTests {
    private SpawnerMigrationGameTests(){}
    public static void contexts(GameTestHelper helper){
        try(var f=new CosmicSpawnerGameTests.Fixture(helper,Long.MAX_VALUE-3201)){
            var pos=f.spawner(0,1,2);
            var be=(CosmicSpawnerBlockEntity)f.level.getBlockEntity(pos);
            var image=be.saveWithoutMetadata(f.level.registryAccess());
            image.putInt(CosmicSpawnerBlockEntity.DATA_VERSION_KEY,150);image.putString("SpawnerEntityId","minecraft:zombie");
            image.putString("future_block_field","unchanged");
            var preset=new CompoundTag();preset.putInt("presetVersion",2);preset.putString("entityType","minecraft:zombie");
            var opaque=new CompoundTag();opaque.putString("id","futuremod:unavailable");opaque.putInt("count",1);
            preset.put("eq_mainhand",opaque);preset.putLong("future_preset_field",7123);image.put("SpawnerPreset",preset);
            var entity=new CompoundTag();entity.putString("id","minecraft:zombie");entity.putString("AuthoredMarker","keep");
            var spawn=new CompoundTag();spawn.put("entity",entity);image.put("SpawnData",spawn);
            var weighted=new CompoundTag();weighted.putInt("weight",7);weighted.put("data",spawn.copy());
            var choices=new ListTag();choices.add(weighted);image.put("SpawnPotentials",choices);
            be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),image));
            var baseline=be.saveWithoutMetadata(f.level.registryAccess());
            for(var state:java.util.List.of(DungeonRunState.ACTIVE,DungeonRunState.PAUSED,DungeonRunState.PREPARING)){
                f.runs.put(f.id,f.runs.get(f.id).withState(state,DungeonResetReason.MANUAL));
                be.onLoad();
                f.check(baseline.equals(be.saveWithoutMetadata(f.level.registryAccess())),"Instance "+state+" does not upgrade old saved data");
            }
            var run=f.runs.remove(f.id);
            try{
                f.check(SpawnerDevelopmentMigration.developmentWorld(f.level),"Fixture now represents the development main world");
                for(int pass=0;pass<2;pass++)try(var timing=new DungeonStartupTiming("native-spawner-reload-pass-"+pass)){
                    be.onLoad();var saved=be.saveWithoutMetadata(f.level.registryAccess());
                    f.check(saved.getIntOr(CosmicSpawnerBlockEntity.DATA_VERSION_KEY,0)==154,"Development version advances");
                    var expected=baseline.copy();expected.putInt(CosmicSpawnerBlockEntity.DATA_VERSION_KEY,154);
                    expected.getCompoundOrEmpty("SpawnerPreset").putInt("presetVersion",5);
                    f.check(expected.equals(saved),"Upgrade changes only version markers; authored settings, weighted data and unknown components survive");
                    // A detached block entity models serialized data from a previously unloaded chunk.
                    var detached=new CosmicSpawnerBlockEntity(pos,be.getBlockState());
                    detached.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),saved));
                    f.check(saved.equals(detached.saveWithoutMetadata(f.level.registryAccess())),"Detached old/unloaded data remains lossless before world context");
                    detached.setLevel(f.level);detached.onLoad();
                    f.check(saved.equals(detached.saveWithoutMetadata(f.level.registryAccess())),"Current preset retains opaque components through another native reload");
                    be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,f.level.registryAccess(),saved));
                    timing.success();
                }
            }finally{f.runs.put(f.id,run);}
        }
        helper.succeed();
    }
}
