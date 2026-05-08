package net.minecraft.client.resources;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaypointStyleManager extends SimpleJsonResourceReloadListener<WaypointStyle> {
    private static final FileToIdConverter ASSET_LISTER = FileToIdConverter.json("waypoint_style");
    private static final WaypointStyle MISSING = new WaypointStyle(0, 1, List.of(MissingTextureAtlasSprite.getLocation()));
    private Map<ResourceKey<WaypointStyleAsset>, WaypointStyle> waypointStyles = Map.of();

    public WaypointStyleManager() {
        super(WaypointStyle.CODEC, ASSET_LISTER);
    }

    protected void apply(Map<ResourceLocation, WaypointStyle> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.waypointStyles = object.entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(p_419819_ -> ResourceKey.create(WaypointStyleAssets.ROOT_ID, p_419819_.getKey()), Entry::getValue));
    }

    public WaypointStyle get(ResourceKey<WaypointStyleAsset> key) {
        return this.waypointStyles.getOrDefault(key, MISSING);
    }
}
