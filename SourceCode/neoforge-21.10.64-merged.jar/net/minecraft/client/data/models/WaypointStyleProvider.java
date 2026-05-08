package net.minecraft.client.data.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaypointStyleProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;

    public WaypointStyleProvider(PackOutput packOutput) {
        this.pathProvider = packOutput.createPathProvider(PackOutput.Target.RESOURCE_PACK, "waypoint_style");
    }

    private static void bootstrap(BiConsumer<ResourceKey<WaypointStyleAsset>, WaypointStyle> consumer) {
        consumer.accept(
            WaypointStyleAssets.DEFAULT,
            new WaypointStyle(
                128,
                332,
                List.of(
                    ResourceLocation.withDefaultNamespace("default_0"),
                    ResourceLocation.withDefaultNamespace("default_1"),
                    ResourceLocation.withDefaultNamespace("default_2"),
                    ResourceLocation.withDefaultNamespace("default_3")
                )
            )
        );
        consumer.accept(
            WaypointStyleAssets.BOWTIE,
            new WaypointStyle(
                64,
                332,
                List.of(
                    ResourceLocation.withDefaultNamespace("bowtie"),
                    ResourceLocation.withDefaultNamespace("default_0"),
                    ResourceLocation.withDefaultNamespace("default_1"),
                    ResourceLocation.withDefaultNamespace("default_2"),
                    ResourceLocation.withDefaultNamespace("default_3")
                )
            )
        );
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        Map<ResourceKey<WaypointStyleAsset>, WaypointStyle> map = new HashMap<>();
        bootstrap((p_419715_, p_419684_) -> {
            if (map.putIfAbsent(p_419715_, p_419684_) != null) {
                throw new IllegalStateException("Tried to register waypoint style twice for id: " + p_419715_);
            }
        });
        return DataProvider.saveAll(output, WaypointStyle.CODEC, this.pathProvider::json, map);
    }

    @Override
    public String getName() {
        return "Waypoint Style Definitions";
    }
}
