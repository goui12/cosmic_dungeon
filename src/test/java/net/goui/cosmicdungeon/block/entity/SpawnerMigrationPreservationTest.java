package net.goui.cosmicdungeon.block.entity;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class SpawnerMigrationPreservationTest {
    private static RegistryAccess lookup(){return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);}
    private static CosmicSpawnerPreset load(CompoundTag tag){
        return CosmicSpawnerPreset.load(TagValueInput.create(ProblemReporter.DISCARDING,lookup(),tag));
    }
    private static CompoundTag save(CosmicSpawnerPreset preset){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,lookup());preset.save(out);return out.buildResult();
    }
    @Test void oldPresetStaysExactUntilDevelopmentUpgradeAndRemainsLosslessAcrossReloads(){
        var old=new CompoundTag();old.putInt("presetVersion",2);old.putString("entityType","minecraft:zombie");
        var opaque=new CompoundTag();opaque.putString("id","futuremod:authored_weapon");opaque.putInt("count",1);
        var components=new CompoundTag();components.putString("futuremod:component","untouched");opaque.put("components",components);
        old.put("eq_mainhand",opaque);old.putLong("future_preset_field",8123);
        var preset=load(old);assertEquals(old,save(preset));
        preset.upgradeDevelopmentData();var expected=old.copy();expected.putInt("presetVersion",5);
        assertEquals(expected,save(preset));assertEquals(expected,save(load(save(preset))));
        var reloaded=load(save(preset));reloaded.setCustomName(net.minecraft.network.chat.Component.literal("Changed"));
        var edited=save(reloaded);
        assertEquals(opaque,edited.get("eq_mainhand"));assertEquals(8123,edited.getLongOr("future_preset_field",0));
        assertEquals(edited,save(load(edited)));
    }
}
