package net.goui.cosmicdungeon.region;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RegionLookServerTest {
    private static ResourceKey<Level> key(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("cosmicdungeon", name));
    }

    @Test void templateOutlineIsSentForTheViewersPhysicalInstance() {
        var template = key("dungeon_1");
        var physical = key("dungeon_instance_01_run_42");
        assertEquals(physical, RegionLookServer.displayDimension(template, physical, template));
    }

    @Test void otherTemplatesAndOtherInstancesDoNotAppearInThisInstance() {
        var template = key("dungeon_1");
        var physical = key("dungeon_instance_01_run_42");
        var otherTemplate = key("dungeon_2");
        var otherInstance = key("dungeon_instance_02_run_43");
        var oldGeneration = key("dungeon_instance_01_run_41");
        for (var region : java.util.List.of(otherTemplate, otherInstance, oldGeneration, Level.OVERWORLD)) {
            assertEquals(region, RegionLookServer.displayDimension(region, physical, template));
            assertNotEquals(physical, RegionLookServer.displayDimension(region, physical, template));
        }
    }

    @Test void nativeAndExplicitPhysicalOutlinesKeepTheirOriginalIdentity() {
        var template = key("dungeon_1");
        var physical = key("dungeon_instance_01_run_42");
        assertEquals(physical, RegionLookServer.displayDimension(physical, physical, template));
        assertEquals(template, RegionLookServer.displayDimension(template, template, template));
        assertEquals(Level.OVERWORLD, RegionLookServer.displayDimension(Level.OVERWORLD, Level.OVERWORLD, Level.OVERWORLD));
    }
}
