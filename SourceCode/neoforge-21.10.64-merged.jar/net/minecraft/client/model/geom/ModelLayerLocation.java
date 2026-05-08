package net.minecraft.client.model.geom;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ModelLayerLocation(ResourceLocation model, String layer) {
    @Override
    public String toString() {
        return this.model + "#" + this.layer;
    }
}
