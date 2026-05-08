package net.minecraft.client.resources;

import java.io.IOException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.DryFoliageColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DryFoliageColorReloadListener extends SimplePreparableReloadListener<int[]> {
    private static final ResourceLocation LOCATION = ResourceLocation.withDefaultNamespace("textures/colormap/dry_foliage.png");

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    protected int[] prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        try {
            return LegacyStuffWrapper.getPixels(resourceManager, LOCATION);
        } catch (IOException ioexception) {
            throw new IllegalStateException("Failed to load dry foliage color texture", ioexception);
        }
    }

    protected void apply(int[] object, ResourceManager resourceManager, ProfilerFiller profiler) {
        DryFoliageColor.init(object);
    }
}
